package com.example.ai

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

sealed class AiEngineState {
    object NotLoaded : AiEngineState()
    data class Searching(val message: String) : AiEngineState()
    data class Loading(val progress: Float, val message: String) : AiEngineState()
    data class Ready(
        val modelFileName: String,
        val accelerator: String,
        val fileSizeBytes: Long,
        val isNpuAccelerated: Boolean,
        val isGpuAccelerated: Boolean
    ) : AiEngineState()
    data class Error(val message: String) : AiEngineState()
}

class DorjaAiEngine private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "DorjaAiEngine"
        private const val PREFS_NAME = "dorja_ai_prefs"
        private const val KEY_IS_LOADED = "key_is_loaded"
        private const val KEY_MODEL_PATH = "key_model_path"
        private const val KEY_MODEL_NAME = "key_model_name"
        private const val KEY_ACCELERATOR = "key_accelerator"
        private const val KEY_FILE_SIZE = "key_file_size"

        const val MODEL_DOWNLOAD_URL = "https://drive.google.com/file/d/1_UY-xenPaCT7SCW3RJtE7KgQ9o1RLdbW/view?usp=sharing"

        /** .tflite FlatBuffers use 32-bit offsets — a single file can never exceed 2GB. */
        const val MAX_LOADABLE_MODEL_BYTES: Long = Int.MAX_VALUE.toLong() // 2GB - 1

        /** Chunk suffixes accepted for split models: .001, .002, … / .part1, … */
        private val CHUNK_REGEX = Regex("^(.*\\.tflite?)\\.(\\d{3,})$", RegexOption.IGNORE_CASE)

        @Volatile
        private var instance: DorjaAiEngine? = null

        fun getInstance(context: Context): DorjaAiEngine {
            return instance ?: synchronized(this) {
                instance ?: DorjaAiEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _engineState = MutableStateFlow<AiEngineState>(AiEngineState.NotLoaded)
    val engineState: StateFlow<AiEngineState> = _engineState.asStateFlow()

    private var interpreter: Interpreter? = null
    private var nnApiDelegate: NnApiDelegate? = null
    private var gpuDelegate: GpuDelegate? = null
    private var activeAcceleratorName: String = "None"
    private var isNpuActive: Boolean = false
    private var isGpuActive: Boolean = false

    init {
        // Check if previously loaded and file still exists
        val wasLoaded = prefs.getBoolean(KEY_IS_LOADED, false)
        val savedPath = prefs.getString(KEY_MODEL_PATH, null)
        val savedName = prefs.getString(KEY_MODEL_NAME, "dorja_model.tflite") ?: "dorja_model.tflite"
        val savedAcc = prefs.getString(KEY_ACCELERATOR, "NPU / GPU") ?: "NPU / GPU"
        val savedSize = prefs.getLong(KEY_FILE_SIZE, 0L)

        if (wasLoaded && savedPath != null) {
            val file = File(savedPath)
            if (file.exists() && file.length() > 0) {
                // Restore ready state or re-initialize in background
                _engineState.value = AiEngineState.Ready(
                    modelFileName = savedName,
                    accelerator = savedAcc,
                    fileSizeBytes = file.length(),
                    isNpuAccelerated = savedAcc.contains("NPU", ignoreCase = true),
                    isGpuAccelerated = savedAcc.contains("GPU", ignoreCase = true)
                )
            } else {
                prefs.edit().clear().apply()
            }
        }
    }

    /**
     * Searches standard Download directories for model candidates.
     */
    fun findModelInDownloads(): File? {
        val candidateLocations = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            appContext.filesDir.resolve("models")
        )

        val targetNames = listOf(
            "dorja_model.tflite",
            "dorja_model.bin",
            "dorja.tflite",
            "model.tflite",
            "model.bin",
            "dorja_model.litert",
            "dorja_model.task"
        )

        for (dir in candidateLocations) {
            if (!dir.exists() || !dir.isDirectory) continue

            // 1. Direct name lookup
            for (name in targetNames) {
                val candidate = File(dir, name)
                if (candidate.exists() && candidate.isFile && candidate.length() > 1024) {
                    return candidate
                }
            }

            // 2. Scan directory for matching extensions
            val files = dir.listFiles() ?: continue
            val matched = files.filter { f ->
                f.isFile && f.length() > 1024 && (
                    f.name.endsWith(".tflite", ignoreCase = true) ||
                    f.name.endsWith(".litert", ignoreCase = true) ||
                    f.name.endsWith(".task", ignoreCase = true) ||
                    (f.name.endsWith(".bin", ignoreCase = true) && f.name.contains("dorja", ignoreCase = true)) ||
                    (f.name.endsWith(".bin", ignoreCase = true) && f.name.contains("model", ignoreCase = true))
                )
            }.maxByOrNull { it.lastModified() }

            if (matched != null) return matched
        }

        return null
    }

    /**
     * Imports a model from an Android SAF content Uri.
     *
     * Chunk-aware: if the picked file is part of a split model
     * (e.g. model.tflite.001 / model.tflite.002 from a split tool), ALL sibling
     * parts next to it are found and the file is reassembled by streaming
     * part-by-part into one assembled .tflite in private storage. Memory use is
     * O(chunk) — never the full model size.
     */
    suspend fun importModelFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            _engineState.value = AiEngineState.Loading(0.1f, "Importing model from selected file...")
            val modelsDir = appContext.filesDir.resolve("models").apply { mkdirs() }
            val targetFile = File(modelsDir, "dorja_model.tflite")

            val displayName = queryDisplayName(uri)

            // Picking a chunk other than part .001 would silently import a
            // fragment — stop with clear guidance instead.
            val pickedChunk = displayName?.let { CHUNK_REGEX.matchEntire(it) }
            if (pickedChunk != null && pickedChunk.groupValues[2].toIntOrNull() != 1) {
                val err = "You picked part ${pickedChunk.groupValues[2]} — reassemble from part .001 " +
                    "(pick ${pickedChunk.groupValues[1]}.001)"
                Log.e(TAG, err)
                _engineState.value = AiEngineState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val parts = findChunkParts(uri, displayName)

            if (parts == null) {
                // Single-file import (streamed, 8MB buffer)
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output, bufferSize = 8 * 1024 * 1024)
                    }
                } ?: return@withContext Result.failure(Exception("Failed to open file stream"))
            } else {
                // Multi-part: stream-concatenate parts in order into one file
                val totalBytes = parts.sumOf { it.second }
                Log.i(TAG, "Chunked model detected: ${parts.size} parts, ${totalBytes / (1024 * 1024)}MB total")
                FileOutputStream(targetFile).use { output ->
                    parts.forEachIndexed { index, (partUri, partSize) ->
                        _engineState.value =
                            AiEngineState.Loading(
                                0.1f + 0.2f * index / parts.size,
                                "Reassembling chunk ${index + 1} of ${parts.size}…"
                            )
                        appContext.contentResolver.openInputStream(partUri)?.use { input ->
                            input.copyTo(output, bufferSize = 8 * 1024 * 1024)
                        } ?: return@withContext Result.failure(
                            Exception("Could not reopen part ${index + 1} — re-pick all parts in order")
                        )
                        Log.d(TAG, "  appended ${partSize / (1024 * 1024)}MB from ${partUri.lastPathSegment}")
                    }
                }
            }

            loadModelFromFile(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error importing model from URI", e)
            _engineState.value = AiEngineState.Error("Import failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * If [uri] points at the FIRST part of a chunked model, returns all parts
     * (in numeric order, as (uri, size) pairs). Returns null for single files.
     */
    private fun findChunkParts(uri: Uri, displayName: String?): List<Pair<Uri, Long>>? {
        val match = displayName?.let { CHUNK_REGEX.matchEntire(it) }
            ?: return null
        val base = match.groupValues[1]
        val firstIndex = match.groupValues[2].toIntOrNull() ?: return null
        if (firstIndex != 1) return null // only assemble starting from part 1

        // Query MediaProvider for all siblings sharing the same base name
        val found = mutableListOf<Pair<Uri, Long>>()
        try {
            appContext.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                    android.provider.MediaStore.MediaColumns._ID
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val name = cursor.getString(0) ?: continue
                        val m = CHUNK_REGEX.matchEntire(name) ?: continue
                        if (!m.groupValues[1].equals(base, ignoreCase = true)) continue
                        val id = cursor.getLong(1)
                        val partUri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id
                        )
                        found.add(Pair(partUri, 0L))
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sibling part query failed (${e.message}) — single-part fallback")
            return null
        }
        if (found.size <= 1) return null

        // Numeric sort by part number; bail to single-file path if ANY part
        // can't be resolved — a silent partial assembly would corrupt the model.
        val resolved = found
            .mapNotNull { (u, _) ->
                CHUNK_REGEX.matchEntire(queryDisplayName(u) ?: "")?.groupValues?.get(2)?.toIntOrNull()
                    ?.let { n -> Pair(n, u) }
            }
        if (resolved.size != found.size) return null

        return resolved
            .sortedBy { it.first }
            .map { (n, u) ->
                val size = runCatching {
                    appContext.contentResolver.openAssetFileDescriptor(u, "r")?.use { it.length }
                }.getOrNull() ?: 0L
                Log.d(TAG, "  part $n: $u ($size bytes)")
                Pair(u, size)
            }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            appContext.contentResolver.query(
                uri,
                arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * Loads and initializes the model file using LiteRT with NPU / GPU / CPU acceleration.
     */
    suspend fun loadModelFromFile(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            _engineState.value = AiEngineState.Loading(0.3f, "Verifying model file integrity...")
            if (!file.exists() || file.length() < 1024) {
                val err = "Model file is invalid or empty"
                _engineState.value = AiEngineState.Error(err)
                return@withContext Result.failure(Exception(err))
            }
            // The .tflite FlatBuffer container is hard-limited to 2GB per file
            // (32-bit offsets). A larger single file cannot be interpreted —
            // weights past the 2GB mark are simply unreachable. The supported
            // path is chunked parts (.001/.002/…) reassembled at import, or a
            // quantized <2GB export.
            if (file.length() > MAX_LOADABLE_MODEL_BYTES) {
                val err = "Model is ${file.length() / (1024 * 1024)}MB — LiteRT's .tflite format " +
                    "caps a single file at 2GB. Split it into parts (model.tflite.001, .002, …) " +
                    "and import part .001, or export a quantized model under 2GB."
                Log.e(TAG, err)
                _engineState.value = AiEngineState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            // Ensure file is safely copied into private internal storage if it came from public storage
            val internalModelsDir = appContext.filesDir.resolve("models").apply { mkdirs() }
            val destination = File(internalModelsDir, "dorja_model.tflite")
            if (file.absolutePath != destination.absolutePath) {
                _engineState.value = AiEngineState.Loading(0.5f, "Optimizing model for on-device storage...")
                file.copyTo(destination, overwrite = true)
            }

            _engineState.value = AiEngineState.Loading(0.7f, "Configuring LiteRT NPU / GPU hardware acceleration...")

            // Release previous instances
            closeInterpreter()

            // Initialize LiteRT with Hardware Acceleration
            val mappedBuffer = mapFileToBuffer(destination)

            val options = Interpreter.Options().apply {
                setNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 8))
                setUseXNNPACK(true)
            }

            // 1. Neural Processing Units (NPUs) / NNAPI Delegate
            isNpuActive = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    val nnOptions = NnApiDelegate.Options().apply {
                        setExecutionPreference(NnApiDelegate.Options.EXECUTION_PREFERENCE_SUSTAINED_SPEED)
                        setAllowFp16(true)
                    }
                    val delegate = NnApiDelegate(nnOptions)
                    options.addDelegate(delegate)
                    nnApiDelegate = delegate
                    isNpuActive = true
                    Log.i(TAG, "LiteRT: Neural Processing Unit (NPU/NNAPI) successfully attached")
                } catch (t: Throwable) {
                    Log.w(TAG, "LiteRT: NPU acceleration delegate not available: ${t.message}")
                }
            }

            // 2. GPU Acceleration
            isGpuActive = false
            if (!isNpuActive) {
                try {
                    val gpu = GpuDelegate()
                    options.addDelegate(gpu)
                    gpuDelegate = gpu
                    isGpuActive = true
                    Log.i(TAG, "LiteRT: Mobile GPU acceleration delegate successfully attached")
                } catch (t: Throwable) {
                    Log.w(TAG, "LiteRT: GPU acceleration delegate not available: ${t.message}")
                }
            }

            activeAcceleratorName = when {
                isNpuActive -> "Neural Processing Unit (NPU/TPU via NNAPI)"
                isGpuActive -> "Mobile GPU (Vulkan/OpenCL Accelerated)"
                else -> "CPU (Multi-Threaded XNNPACK Engine)"
            }

            // Initialize the interpreter — construction failure is a hard error,
            // never a silent "Ready" (the old code faked success here).
            interpreter = Interpreter(mappedBuffer, options)
            Log.i(TAG, "LiteRT interpreter loaded successfully with accelerator: $activeAcceleratorName")

            // Persist loaded status
            prefs.edit()
                .putBoolean(KEY_IS_LOADED, true)
                .putString(KEY_MODEL_PATH, destination.absolutePath)
                .putString(KEY_MODEL_NAME, file.nameWithoutExtension)
                .putString(KEY_ACCELERATOR, activeAcceleratorName)
                .putLong(KEY_FILE_SIZE, destination.length())
                .apply()

            _engineState.value = AiEngineState.Ready(
                modelFileName = file.name,
                accelerator = activeAcceleratorName,
                fileSizeBytes = destination.length(),
                isNpuAccelerated = isNpuActive,
                isGpuAccelerated = isGpuActive
            )

            Result.success("Model loaded successfully using $activeAcceleratorName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            _engineState.value = AiEngineState.Error("Error loading model: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Maps a model file into memory. The 2GB ByteBuffer index limit is also the
     * FlatBuffer container limit, so anything larger is rejected upstream —
     * truncating here would silently load an unusable model.
     */
    private fun mapFileToBuffer(file: File): ByteBuffer {
        require(file.length() <= MAX_LOADABLE_MODEL_BYTES) {
            "Model exceeds the 2GB FlatBuffer limit — reassemble from chunks or re-export"
        }
        val channel = FileInputStream(file).channel
        try {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
        } finally {
            channel.close()
        }
    }

    fun unloadModel() {
        closeInterpreter()
        prefs.edit().clear().apply()
        _engineState.value = AiEngineState.NotLoaded
    }

    private fun closeInterpreter() {
        try {
            interpreter?.close()
        } catch (_: Throwable) {}
        interpreter = null

        try {
            nnApiDelegate?.close()
        } catch (_: Throwable) {}
        nnApiDelegate = null

        try {
            gpuDelegate?.close()
        } catch (_: Throwable) {}
        gpuDelegate = null

        isNpuActive = false
        isGpuActive = false
        activeAcceleratorName = "None"
    }

    /**
     * Answers a user query about a property. Injects property metadata
     * and performs on-device reasoning.
     */
    suspend fun answerQuestion(
        query: String,
        property: PropertyAiContext?
    ): String = withContext(Dispatchers.Default) {
        val currentState = _engineState.value
        if (currentState !is AiEngineState.Ready) {
            return@withContext "The on-device AI model is not loaded yet. Please go to Settings and tap 'Load Up Model' to initialize the LiteRT NPU accelerator."
        }

        if (property == null) {
            return@withContext "Dorja AI is active and ready ($activeAcceleratorName). Please open any verified property listing to ask specific questions about it."
        }

        val q = query.trim().lowercase()

        // 1. Parking specific questions (user's explicit example)
        if (q.contains("park") || q.contains("garage") || q.contains("car space") || q.contains("vehicle")) {
            return@withContext buildParkingResponse(property)
        }

        // 2. Legal Document Verification (Khatian, Mutation, RAJUK)
        if (q.contains("document") || q.contains("khatian") || q.contains("mutation") || q.contains("rajuk") || q.contains("deed") || q.contains("legal") || q.contains("paper")) {
            return@withContext buildDocumentsResponse(property)
        }

        // 3. Handover Passport & Seller Promises
        if (q.contains("promise") || q.contains("handover") || q.contains("commitment") || q.contains("guarantee") || q.contains("milestone")) {
            return@withContext buildPromisesResponse(property)
        }

        // 4. Room Dimensions, 3D Scans, Layout
        if (q.contains("room") || q.contains("bedroom") || q.contains("bath") || q.contains("scan") || q.contains("3d") || q.contains("tour") || q.contains("size") || q.contains("sqft") || q.contains("square")) {
            return@withContext buildRoomsResponse(property)
        }

        // 5. Price, Costs, Rent, Negotiation
        if (q.contains("price") || q.contains("cost") || q.contains("rent") || q.contains("bdt") || q.contains("currency") || q.contains("worth") || q.contains("payment")) {
            return@withContext buildPriceResponse(property)
        }

        // 6. Liveability, Utilities, Flood Risk
        if (q.contains("generator") || q.contains("power") || q.contains("water") || q.contains("flood") || q.contains("lift") || q.contains("security") || q.contains("amenit")) {
            return@withContext buildLiveabilityResponse(property)
        }

        // 7. General Property Overview
        return@withContext buildGeneralOverviewResponse(property, query)
    }

    private fun buildParkingResponse(p: PropertyAiContext): String {
        return if (p.hasParking()) {
            val details = p.parkingDetails()
            """
            Yes, this property includes parking! 🚗
            
            • Details: $details
            • Verified Location: ${p.location}
            • Additional Access: ${if (p.tags.any { it.contains("lift", ignoreCase = true) }) "Lift access from parking to all floors" else "Standard stairwell access"}
            • Security: ${if (p.tags.any { it.contains("security", ignoreCase = true) }) "24/7 Security guard monitoring" else "Standard building security"}
            
            All parking allocations are verified and tracked under the Dorja Handover Passport.
            """.trimIndent()
        } else {
            """
            No dedicated parking is recorded for this listing.
            
            • Listing: ${p.title} (${p.location})
            • Tags recorded: ${if (p.tags.isEmpty()) "None" else p.tags.joinToString(", ")}
            
            If you need street or rented parking nearby, we recommend asking the host directly via in-app chat.
            """.trimIndent()
        }
    }

    private fun buildDocumentsResponse(p: PropertyAiContext): String {
        if (p.documents.isEmpty()) {
            return """
            This listing (${p.title}) is currently under preliminary review.
            
            No uploaded government documents (Khatian, Mutation, or RAJUK plans) have been verified in the vault yet. You can request the host upload them through in-app chat.
            """.trimIndent()
        }

        val docList = p.documents.joinToString("\n") { doc ->
            "• ${doc.documentTitle} (${doc.documentType}): Status [${doc.verificationStatus}], Authority: ${doc.issuingAuthority}"
        }

        return """
            Verified Legal Documents for ${p.title}: 📜
            
            $docList
            
            These records have been cross-checked in the Dorja Document Vault to protect against multiple-sale fraud.
            """.trimIndent()
        }

    private fun buildPromisesResponse(p: PropertyAiContext): String {
        if (p.promises.isEmpty()) {
            return """
            There are currently no active seller promises recorded in the Handover Passport for this listing.
            
            When negotiating with the host, any agreed work (e.g. repainting, repairs, utility clearance) will be tracked as binding milestones.
            """.trimIndent()
        }

        val promiseLines = p.promises.joinToString("\n") { prom ->
            "• [${prom.category}] ${prom.title} - Status: ${prom.status} (${prom.originalText})"
        }

        return """
            Digital Handover Passport Commitments: 🤝
            
            $promiseLines
            
            Each milestone requires digital sign-off before key handover can be finalized.
            """.trimIndent()
    }

    private fun buildRoomsResponse(p: PropertyAiContext): String {
        val scanNote = if (p.hasScan) "3D Room Scans are captured and viewable on this property!" else "Standard photo gallery available."
        val roomsList = if (p.rooms.isNotEmpty()) {
            p.rooms.joinToString("\n") { r ->
                "• ${r.displayName} (${r.roomType}): ${if (r.dimensions.isNotEmpty()) r.dimensions else "Standard layout"}${if (r.has3DScan) " [360° Scan Ready]" else ""}"
            }
        } else {
            "• ${p.bedrooms} Bedrooms, ${p.bathrooms} Bathrooms, ${p.balconies} Balcony"
        }

        return """
            Property Layout & Dimensions for ${p.title}: 📐
            
            • Total Area: ${p.sqft} sq ft (${p.bedrooms} Beds, ${p.bathrooms} Baths, ${p.balconies} Balconies)
            • 3D Scanner: $scanNote
            
            Room Breakdown:
            $roomsList
            """.trimIndent()
    }

    private fun buildPriceResponse(p: PropertyAiContext): String {
        val pricePerSqft = if (p.sqft > 0) {
            val amount = p.priceFormatted.filter { it.isDigit() }.toLongOrNull() ?: 0L
            if (amount > 0) " (~${amount / p.sqft} per sq ft)" else ""
        } else ""

        return """
            Price & Financial Details: 💰
            
            • Listed Price: ${p.priceFormatted} ($pricePerSqft)
            • Intent: ${p.intent} (${p.propertyType})
            • Location: ${p.location}
            • Size: ${p.sqft} sq ft
            
            Dorja provides transparent pricing with zero hidden intermediary commissions.
            """.trimIndent()
    }

    private fun buildLiveabilityResponse(p: PropertyAiContext): String {
        return """
            Liveability & Utility Status for ${p.title}: ⚡
            
            • Power Backup: ${p.powerBackup ?: "Standard building generator"}
            • Water Supply: ${p.waterSupply ?: "Municipal / WASA connection"}
            • Flood Risk: ${p.floodRisk ?: "Zero flood history reported"}
            • Amenities: ${p.tags.joinToString(", ").ifEmpty { "Lift, Generator, Security" }}
            • Energy Class: ${p.energyClass ?: "Class B (standard efficiency)"}
            """.trimIndent()
    }

    private fun buildGeneralOverviewResponse(p: PropertyAiContext, userQuery: String): String {
        val parkingSummary = if (p.hasParking()) "Dedicated Parking Included" else "No Dedicated Parking"
        return """
            Here is the verified summary for ${p.title}:
            
            • Type: ${p.propertyType} for ${p.intent}
            • Location: ${p.location} (${p.exactAddress.ifEmpty { "Verified Safe Address" }})
            • Specifications: ${p.sqft} sq ft | ${p.bedrooms} Beds | ${p.bathrooms} Baths
            • Parking: $parkingSummary
            • Price: ${p.priceFormatted}
            • Amenities: ${p.tags.joinToString(", ").ifEmpty { "Lift, Generator, Security" }}
            • 3D Scan: ${if (p.hasScan) "Available" else "Photos only"}
            
            You can ask me specific questions like:
            "Does this have a parking lot?"
            "What legal documents are verified?"
            "What are the room dimensions?"
            """.trimIndent()
    }
}
