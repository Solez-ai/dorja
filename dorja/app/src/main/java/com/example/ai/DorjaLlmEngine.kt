package com.example.ai

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Real on-device LLM engine for the Hey Dorja assistant, built on LiteRT-LM
 * (Qwen3-1.7B, official .litertlm from litert-community).
 *
 * Responsibilities:
 *  - Download the 977MB model in resumable 256MB chunks (notification of
 *    progress via [downloadState]); survives app kills via .part state files
 *  - Initialize the LiteRT-LM engine on GPU (fallback CPU)
 *  - Stream answers to property questions via [sendMessage]
 *
 * The rule-based [DorjaAiEngine] remains the instant fallback when the model
 * is not downloaded/loaded — the sheet decides which one to use.
 */
object DorjaLlmEngine {

    private const val TAG = "DorjaLlmEngine"

    /** Official Qwen3-1.7B (dynamic 4-bit weights, 977MB) in .litertlm format. */
    const val MODEL_URL: String =
        "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm"

    const val MODEL_FILE_NAME: String = "Qwen3-1.7B.litertlm"

    /** 256MB chunks — resumable; each part flushed to disk before continuing. */
    private const val CHUNK_SIZE_BYTES: Long = 256L * 1024 * 1024

    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 60_000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Download state ───────────────────────────────────────────────────
    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(
            val chunkIndex: Int,
            val totalChunks: Int,
            val downloadedBytes: Long,
            val totalBytes: Long
        ) : DownloadState() {
            val fraction: Float get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
        }
        data object Finalizing : DownloadState()
        data class Failed(val message: String) : DownloadState()
        data object Done : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // ── Engine readiness state ───────────────────────────────────────────
    sealed class LlmState {
        data object NoModel : LlmState()
        data class Initializing(val message: String) : LlmState()
        data class Ready(val backend: String) : LlmState()
        data class Error(val message: String) : LlmState()
    }

    private val _llmState = MutableStateFlow<LlmState>(LlmState.NoModel)
    val llmState: StateFlow<LlmState> = _llmState.asStateFlow()

    private var engine: Engine? = null
    private var initJob: Job? = null

    // ── Answer cache ─────────────────────────────────────────────────────
    // Prompt-keyed LRU: an identical question about identical property data
    // replays instantly instead of re-running inference. Keying on the full
    // prompt means any change to the listing data naturally invalidates the
    // cached entry (the prompt changes with it).
    private const val ANSWER_CACHE_MAX = 32
    private val answerCache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>): Boolean =
            size > ANSWER_CACHE_MAX
    }

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        refreshStateFromDisk()
    }

    /** Absolute path of the assembled model, or null if not (fully) present. */
    fun modelFile(): File? {
        if (!::appContext.isInitialized) return null
        val f = File(appContext.filesDir, "llm/$MODEL_FILE_NAME")
        return if (f.exists() && f.length() > 0) f else null
    }

    fun isModelDownloaded(): Boolean = modelFile() != null

    private fun refreshStateFromDisk() {
        if (isModelDownloaded()) {
            // Set synchronously so the sheet never flashes the download CTA
            _llmState.value = LlmState.Initializing("Loading Qwen3 onto accelerator…")
            initializeInBackground()
        } else {
            _llmState.value = LlmState.NoModel
        }
    }

    // ── Download: chunked, resumable, streaming to disk ─────────────────

    fun startDownload() {
        if (_downloadState.value is DownloadState.Downloading) return
        scope.launch { downloadModel() }
    }

    fun cancelDownload() {
        _downloadState.value = DownloadState.Idle
    }

    private suspend fun downloadModel() {
        try {
            val dir = File(appContext.filesDir, "llm").apply { mkdirs() }
            val outFile = File(dir, MODEL_FILE_NAME)
            val stateFile = File(dir, "$MODEL_FILE_NAME.part")

            _downloadState.value = DownloadState.Downloading(0, 0, 0, 0)

            // 1. Total size via HEAD
            val (totalSize, acceptsRanges) = probeRemote(MODEL_URL)
            if (totalSize <= 0) {
                _downloadState.value = DownloadState.Failed("Server did not report file size")
                return
            }
            val totalChunks = ((totalSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).toInt()

            // 2. Resume offset = existing .part size (server must support ranges)
            var offset = if (acceptsRanges && stateFile.exists()) stateFile.length() else 0L
            if (!acceptsRanges) stateFile.delete()

            Log.i(TAG, "Downloading $MODEL_FILE_NAME: $totalSize bytes in $totalChunks chunks (resuming at $offset)")

            val buffer = ByteArray(1 shl 20) // 1MB network buffer
            var chunkIndex = (offset / CHUNK_SIZE_BYTES).toInt()

            while (offset < totalSize) {
                if (_downloadState.value == DownloadState.Idle) {
                    Log.i(TAG, "Download cancelled — .part preserved for resume")
                    return
                }
                val end = minOf(offset + CHUNK_SIZE_BYTES, totalSize) - 1
                val conn = URL(MODEL_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.instanceFollowRedirects = true
                if (offset > 0) conn.setRequestProperty("Range", "bytes=$offset-$end")

                try {
                    conn.connect()
                    val code = conn.responseCode
                    if (code != 200 && code != 206) {
                        _downloadState.value = DownloadState.Failed("HTTP $code while downloading chunk ${chunkIndex + 1}")
                        return
                    }
                    if (offset > 0 && code == 200) {
                        // Server ignored the Range header — the .part would corrupt on append.
                        Log.w(TAG, "Server ignored Range header — restarting from scratch")
                        stateFile.delete()
                        offset = 0
                        chunkIndex = 0
                    }

                    conn.inputStream.use { input ->
                        FileOutputStream(stateFile, true).use { output ->
                            while (true) {
                                if (_downloadState.value == DownloadState.Idle) return
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                offset += read
                                _downloadState.value = DownloadState.Downloading(
                                    chunkIndex = chunkIndex + 1,
                                    totalChunks = totalChunks,
                                    downloadedBytes = offset,
                                    totalBytes = totalSize
                                )
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
                chunkIndex++
            }

            // 3. Assemble: rename .part → final
            _downloadState.value = DownloadState.Finalizing
            if (outFile.exists()) outFile.delete()
            if (!stateFile.renameTo(outFile)) {
                _downloadState.value = DownloadState.Failed("Could not finalize model file")
                return
            }

            _downloadState.value = DownloadState.Done
            Log.i(TAG, "Model download complete: ${outFile.absolutePath} (${outFile.length()} bytes)")
            initializeInBackground()
        } catch (e: Exception) {
            Log.e(TAG, "Model download failed", e)
            _downloadState.value = DownloadState.Failed(e.message ?: "Download failed")
        }
    }

    private fun probeRemote(url: String): Pair<Long, Boolean> {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.requestMethod = "HEAD"
        conn.instanceFollowRedirects = true
        try {
            conn.connect()
            val size = conn.contentLengthLong
            val ranges = conn.getHeaderField("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
            return Pair(size, ranges)
        } finally {
            conn.disconnect()
        }
    }

    /** Deletes the downloaded model + partial download. */
    fun deleteModel() {
        cancelDownload()
        closeEngine()
        synchronized(answerCache) { answerCache.clear() }
        val dir = File(appContext.filesDir, "llm")
        dir.listFiles()?.forEach { it.delete() }
        refreshStateFromDisk()
    }

    // ── Engine lifecycle ─────────────────────────────────────────────────

    fun initializeInBackground() {
        if (engine != null || initJob?.isActive == true) return
        val file = modelFile() ?: return
        initJob = scope.launch { initialize(file) }
    }

    private suspend fun initialize(modelFile: File) {
        _llmState.value = LlmState.Initializing("Loading Qwen3 onto accelerator…")
        try {
            withContext(Dispatchers.IO) {
                closeEngineQuiet()

                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.GPU(),
                    cacheDir = appContext.cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                try {
                    newEngine.initialize()
                    engine = newEngine
                    _llmState.value = LlmState.Ready("GPU")
                    Log.i(TAG, "LiteRT-LM engine ready (GPU backend)")
                } catch (gpuError: Throwable) {
                    Log.w(TAG, "GPU backend failed (${gpuError.message}) — retrying CPU")
                    try { newEngine.close() } catch (_: Throwable) {}
                    closeEngineQuiet()
                    val cpuConfig = EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = Backend.CPU(),
                        cacheDir = appContext.cacheDir.absolutePath
                    )
                    val cpuEngine = Engine(cpuConfig)
                    cpuEngine.initialize()
                    engine = cpuEngine
                    _llmState.value = LlmState.Ready("CPU")
                    Log.i(TAG, "LiteRT-LM engine ready (CPU backend)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "LiteRT-LM init failed", e)
            closeEngineQuiet()
            _llmState.value = LlmState.Error(e.message ?: "Failed to initialize model")
        }
    }

    /**
     * Sends a question with property context, streaming tokens.
     * The sheet collects this flow to render text as it arrives.
     */
    fun sendMessage(question: String, property: PropertyAiContext?): Flow<String> = callbackFlow {
        val prompt = buildPrompt(question, property)

        // Cache hit → replay the stored answer, skip inference entirely
        val cached = synchronized(answerCache) { answerCache[prompt] }
        if (cached != null) {
            Log.i(TAG, "Answer cache hit — skipping inference")
            trySend(cached)
            close()
            return@callbackFlow
        }

        val eng = engine
        if (eng == null) {
            trySend("Qwen3 is still loading — quick answers are active meanwhile.")
            close()
            return@callbackFlow
        }
        val job = scope.launch {
            var conv: Conversation? = null
            try {
                // Stateless Q&A: fresh conversation per question so context from a
                // previously opened property can't leak into the next answer.
                conv = eng.createConversation()
                val full = StringBuilder()
                conv.sendMessageAsync(prompt).collect { message ->
                    val text = message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    if (text.isNotEmpty()) {
                        full.append(text)
                        trySend(text)
                    }
                }
                // Stream completed successfully → remember the full answer
                val answer = full.toString().trim()
                if (answer.isNotEmpty()) {
                    synchronized(answerCache) { answerCache[prompt] = answer }
                }
                close()
            } catch (t: Throwable) {
                Log.e(TAG, "Inference failed", t)
                close(t)
            } finally {
                try { conv?.close() } catch (_: Throwable) {}
            }
        }
        awaitClose { job.cancel() }
    }

    @VisibleForTesting
    internal fun buildPrompt(question: String, property: PropertyAiContext?): String {
        val ctx = if (property != null) {
            """
            You are Dorja, a verified real-estate assistant. Answer ONLY from the
            property data below; say "not recorded" when something is missing.

            Property: ${property.title} (${property.propertyType}, for ${property.intent})
            Location: ${property.location}. Size: ${property.sqft} sqft, ${property.bedrooms} bed / ${property.bathrooms} bath / ${property.balconies} balcony.
            Price: ${property.priceFormatted}. Parking: ${if (property.hasParking()) "yes" else "not recorded"}.
            Amenities: ${property.tags.ifEmpty { "none recorded" }}.
            Power: ${property.powerBackup ?: "not recorded"}. Water: ${property.waterSupply ?: "not recorded"}. Flood risk: ${property.floodRisk ?: "not recorded"}.
            Documents verified: ${property.documents.size}. Seller promises: ${property.promises.size}.
            """.trimIndent()
        } else {
            "You are Dorja, a verified real-estate assistant. No property is open right now."
        }
        return "$ctx\n\nUser question: $question"
    }

    fun closeEngine() {
        closeEngineQuiet()
        _llmState.value = if (isModelDownloaded()) {
            LlmState.Error("Model downloaded — tap to initialize")
        } else {
            LlmState.NoModel
        }
    }

    private fun closeEngineQuiet() {
        try { engine?.close() } catch (_: Throwable) {}
        engine = null
    }
}
