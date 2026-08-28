package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

// ── Phases ──────────────────────────────────────────────────
private enum class Phase { SELECT, PREVIEW, CAPTURING, DONE }

// ── How many shots around the room ──────────────────────────
private const val TOTAL_SHOTS = 12

// ── Colors ──────────────────────────────────────────────────
private val Accent = Color(0xFF00BCD4)
private val Green = Color(0xFF4CAF50)

// ═════════════════════════════════════════════════════════════
//  MAIN SCREEN
// ═════════════════════════════════════════════════════════════
@Composable
fun RoomScannerScreen(
    listingId: String,
    onBack: () -> Unit,
    onScanComplete: (roomId: String, panoramaJson: String) -> Unit
) {
    val repo = DorjaApp.instance.repository
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Initialize OpenCV
    LaunchedEffect(Unit) {
        try {
            if (!org.opencv.core.Core.NATIVE_LIBRARY_NAME.isNullOrEmpty()) {
                org.opencv.android.OpenCVLoader.initLocal()
                Log.i("Scanner", "OpenCV initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("Scanner", "OpenCV init failed: ${e.message}")
        }
    }

    val rooms by repo.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    // ── Phase ──────────────────────────────────────────────
    var phase by remember { mutableStateOf(Phase.SELECT) }
    var selectedRoom by remember { mutableStateOf<RoomItem?>(null) }

    // ── Camera ─────────────────────────────────────────────
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // ── Permission ─────────────────────────────────────────
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    // ── Heading (0-360 degrees from magnetic north) ────────
    var heading by remember { mutableFloatStateOf(0f) }
    var gyroOn by remember { mutableStateOf(true) }

    // ── Capture data ───────────────────────────────────────
    val capturedPaths = remember { mutableStateListOf<String>() }
    var currentTarget by remember { mutableIntStateOf(0) }

    // ── Gyroscope sensor ───────────────────────────────────
    val sensorMgr = remember { ctx.getSystemService(SensorManager::class.java) }
    val rotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val rotMatrix = FloatArray(9)
    val orientAngles = FloatArray(3)

    DisposableEffect(sensorMgr, gyroOn) {
        if (sensorMgr == null || rotVec == null || !gyroOn) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, e.values)
                        SensorManager.getOrientation(rotMatrix, orientAngles)
                        val yaw = Math.toDegrees(orientAngles[0].toDouble())
                        heading = ((yaw % 360.0) + 360.0).toFloat() % 360f
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            sensorMgr.registerListener(listener, rotVec, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorMgr.unregisterListener(listener) }
        }
    }

    // ── Auto-request permission ────────────────────────────
    LaunchedEffect(Unit) {
        if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── Main UI ────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (phase) {
            Phase.SELECT -> SelectRoom(
                rooms = rooms,
                onSelect = { room ->
                    selectedRoom = room
                    permLauncher.launch(Manifest.permission.CAMERA)
                    phase = Phase.PREVIEW
                },
                onBack = onBack
            )

            Phase.PREVIEW -> PreviewPhase(
                imageCapture = imageCapture,
                onCaptureReady = { imageCapture = it },
                hasCamera = hasCamera,
                roomName = selectedRoom?.displayName ?: "Room",
                gyroOn = gyroOn,
                onToggleGyro = { gyroOn = !gyroOn },
                onStart = { phase = Phase.CAPTURING },
                onBack = { phase = Phase.SELECT },
                lifecycleOwner = lifecycleOwner
            )

            Phase.CAPTURING -> CapturingPhase(
                imageCapture = imageCapture,
                onCaptureReady = { imageCapture = it },
                hasCamera = hasCamera,
                heading = heading,
                targetIndex = currentTarget,
                totalShots = TOTAL_SHOTS,
                capturedCount = capturedPaths.size,
                gyroOn = gyroOn,
                onToggleGyro = { gyroOn = !gyroOn },
                onCapture = {
                    // Take a photo right now
                    val ic = imageCapture ?: return@CapturingPhase
                    val targetAngle = currentTarget * (360 / TOTAL_SHOTS)
                    val file = File(ctx.cacheDir, "frame_${targetAngle}_${System.currentTimeMillis()}.jpg")
                    val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                    Log.i("Scanner", "Capturing frame $targetAngle° (file: ${file.name})")
                    ic.takePicture(
                        opts,
                        ContextCompat.getMainExecutor(ctx),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                // Log frame details
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeFile(file.absolutePath, opts)
                                Log.i("Scanner", "Frame saved: ${opts.outWidth}x${opts.outHeight} at ${file.absolutePath}")
                                capturedPaths.add(file.absolutePath)
                                vibrateShutter(ctx)
                                currentTarget = (currentTarget + 1).coerceAtMost(TOTAL_SHOTS)
                            }
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Scanner", "Capture failed at ${targetAngle}°: ${exc.message}", exc)
                            }
                        }
                    )
                },
                onStop = { phase = Phase.DONE },
                onBack = { phase = Phase.PREVIEW },
                lifecycleOwner = lifecycleOwner
            )

            Phase.DONE -> DonePhase(
                roomName = selectedRoom?.displayName ?: "Room",
                frameCount = capturedPaths.size,
                onSave = {
                    scope.launch {
                        val paths = capturedPaths.toList()
                        // Stitch on IO thread to avoid main thread OOM
                        val stitched = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            stitchFrames(ctx, paths)
                        }
                        val json = buildJson(stitched, paths, selectedRoom?.id ?: "")
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            repo.updateRoom3DScan(selectedRoom?.id ?: "", json)
                        }
                        onScanComplete(selectedRoom?.id ?: "", json)
                    }
                },
                onRetake = {
                    capturedPaths.clear()
                    currentTarget = 0
                    phase = Phase.PREVIEW
                },
                onDiscard = {
                    capturedPaths.clear()
                    currentTarget = 0
                    phase = Phase.SELECT
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 1 — SELECT ROOM
// ═════════════════════════════════════════════════════════════
@Composable
private fun SelectRoom(
    rooms: List<RoomItem>,
    onSelect: (RoomItem) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Select Room to Scan", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("3D Panorama Scanner", color = Accent, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Info
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Accent.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("How it works", color = DorjaColors.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "You'll capture $TOTAL_SHOTS photos around the room.\n" +
                            "Point your phone at each angle and tap the shutter button.\n" +
                            "The photos are stitched into a 360° equirectangular panorama.",
                    color = DorjaColors.Sand300, fontSize = 12.sp, lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (rooms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MeetingRoom, null, tint = DorjaColors.Gray500, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No Rooms Added", color = DorjaColors.White, style = MaterialTheme.typography.titleMedium)
                    Text("Add rooms to your listing first", color = DorjaColors.Sand300)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rooms) { room ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onSelect(room) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (room.has3DScan) Accent.copy(alpha = 0.1f) else DorjaColors.Gray700,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (room.has3DScan) Accent else DorjaColors.Sand300.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (room.has3DScan) Accent.copy(alpha = 0.2f) else DorjaColors.Ink950),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom,
                                    null,
                                    tint = if (room.has3DScan) Accent else DorjaColors.Sand300,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(room.displayName, color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(room.roomType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = DorjaColors.Sand300, fontSize = 11.sp)
                            }
                            if (room.has3DScan) {
                                Badge(containerColor = Accent.copy(alpha = 0.2f)) {
                                    Text("SCANNED", color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 2 — CAMERA PREVIEW (before scanning starts)
// ═════════════════════════════════════════════════════════════
@Composable
private fun PreviewPhase(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    roomName: String,
    gyroOn: Boolean,
    onToggleGyro: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    Box(Modifier.fillMaxSize()) {
        // Camera preview
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)

        // Overlay — cylindrical dots
        ScopeOverlay()

        // Top gradient + back button
        Box(Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))).align(Alignment.TopCenter))

        Row(
            Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SCANNING", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(38.dp))
        }

        // Center instruction card
        Box(Modifier.align(Alignment.Center).padding(32.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.55f), border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 28.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Hold your phone upright\nin portrait mode", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            }
        }

        // Bottom — "PRESS TO START" + green shutter
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PRESS TO START", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.size(68.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Green))
            }
        }

        // Bottom bar — gyro toggle
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center
        ) {
            GyroChip(gyroOn, onToggleGyro)
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 3 — CAPTURING (active scanning)
// ═════════════════════════════════════════════════════════════
@Composable
private fun CapturingPhase(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    heading: Float,
    targetIndex: Int,
    totalShots: Int,
    capturedCount: Int,
    gyroOn: Boolean,
    onToggleGyro: () -> Unit,
    onCapture: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val targetAngle = targetIndex * (360 / totalShots)

    Box(Modifier.fillMaxSize()) {
        // Camera preview
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)

        // Compass ring showing all target angles + current heading
        CompassOverlay(heading, targetIndex, totalShots, capturedCount)

        // Top gradient
        Box(Modifier.fillMaxWidth().height(60.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))).align(Alignment.TopCenter))

        // Alignment banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.65f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp)
        ) {
            Text(
                "TARGET: ${targetAngle}°  •  ${capturedCount}/$totalShots CAPTURED",
                color = Accent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Bottom gradient
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))).align(Alignment.BottomCenter))

        // Progress bar
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(totalShots) { i ->
                val color = when {
                    i < capturedCount -> Green
                    i == targetIndex -> Accent
                    else -> Color.Gray.copy(alpha = 0.4f)
                }
                Box(
                    Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(color)
                )
            }
        }

        // Capture button (red border, camera icon)
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shutter button
            Box(
                Modifier.size(64.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onCapture() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp))
                // Actually, use a camera-like center dot
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
            }

            Spacer(Modifier.width(16.dp))

            // Stop button
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = 0.9f))
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
            }
        }

        // Gyro toggle
        Box(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 130.dp)) {
            GyroChip(gyroOn, onToggleGyro)
        }

        // Hint
        Text(
            "Point at ${targetAngle}° and tap shutter",
            color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 4 — DONE / SAVE
// ═════════════════════════════════════════════════════════════
@Composable
private fun DonePhase(
    roomName: String,
    frameCount: Int,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onDiscard: () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(DorjaColors.Ink950).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = Accent, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Scan Complete", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text("$frameCount photos captured for $roomName", color = DorjaColors.Sand300, textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))

            // Stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("FRAMES", "$frameCount", Modifier.weight(1f))
                StatTile("STITCHED", "360°", Modifier.weight(1f))
                StatTile("TYPE", "EQ.", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            DorjaButton("Save 3D Scan to $roomName", onClick = onSave, modifier = Modifier.fillMaxWidth().height(48.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DorjaOutlinedButton("Retake", onClick = onRetake, modifier = Modifier.weight(1f))
                DorjaOutlinedButton("Discard", onClick = onDiscard, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  SHARED COMPOSABLES
// ═════════════════════════════════════════════════════════════

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    if (!hasCamera) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    onCaptureReady(capture)
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (e: Exception) {
                        Log.e("Scanner", "Camera bind failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScopeOverlay() {
    // Cyan dot grid — cylindrical scope effect
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = 2.dp.toPx()

        // Top edge — dots curve inward
        for (i in 0..40) {
            val x = (i / 40f) * w
            val curve = kotlin.math.abs(i / 40f - 0.5f) * 2f * 40.dp.toPx()
            drawCircle(Accent.copy(alpha = 0.35f), r, Offset(x, 20.dp.toPx() + curve))
        }
        // Bottom edge
        for (i in 0..40) {
            val x = (i / 40f) * w
            val curve = kotlin.math.abs(i / 40f - 0.5f) * 2f * 40.dp.toPx()
            drawCircle(Accent.copy(alpha = 0.35f), r, Offset(x, h - 20.dp.toPx() - curve))
        }
        // Left
        for (i in 0..20) {
            val y = (i / 20f) * h
            val curve = kotlin.math.abs(i / 20f - 0.5f) * 2f * 30.dp.toPx()
            drawCircle(Accent.copy(alpha = 0.25f), r, Offset(12.dp.toPx() + curve, y))
        }
        // Right
        for (i in 0..20) {
            val y = (i / 20f) * h
            val curve = kotlin.math.abs(i / 20f - 0.5f) * 2f * 30.dp.toPx()
            drawCircle(Accent.copy(alpha = 0.25f), r, Offset(w - 12.dp.toPx() - curve, y))
        }
    }
}

@Composable
private fun CompassOverlay(heading: Float, targetIndex: Int, totalShots: Int, capturedCount: Int) {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = size.width * 0.35f

        // Outer ring
        drawCircle(Accent.copy(alpha = 0.15f), radius, Offset(cx, cy), style = Stroke(2.dp.toPx()))

        // Target angle nodes
        repeat(totalShots) { i ->
            val angle = i * (360 / totalShots)
            val rad = Math.toRadians((angle - 90).toDouble())
            val nx = cx + radius * cos(rad).toFloat()
            val ny = cy + radius * sin(rad).toFloat()

            val isCaptured = i < capturedCount
            val isCurrent = i == targetIndex

            val nodeColor = when {
                isCaptured -> Green
                isCurrent -> Accent
                else -> Color.White.copy(alpha = 0.25f)
            }
            val nodeR = if (isCurrent) 8.dp.toPx() else 5.dp.toPx()

            drawCircle(nodeColor, nodeR, Offset(nx, ny))
        }

        // Current heading dot
        val headRad = Math.toRadians((heading - 90).toDouble())
        val hx = cx + radius * cos(headRad).toFloat()
        val hy = cy + radius * sin(headRad).toFloat()

        drawCircle(Accent, 4.dp.toPx(), Offset(hx, hy))
        drawLine(Accent.copy(alpha = 0.3f), Offset(cx, cy), Offset(hx, hy), 1.dp.toPx())
    }
}

@Composable
private fun GyroChip(on: Boolean, toggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (on) Accent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (on) Accent else Color.White.copy(alpha = 0.3f)),
        modifier = Modifier.clickable { toggle() }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) Accent else Color.Gray))
            Spacer(Modifier.width(6.dp))
            Text("GYRO ${if (on) "ON" else "OFF"}", color = if (on) Accent else Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = DorjaColors.Gray700) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(2.dp))
            Text(label, color = DorjaColors.Sand300, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  STITCHING — feature-based panorama reconstruction
//  Uses only core OpenCV: ORB, BFMatcher, Calib3d, Imgproc
// ═════════════════════════════════════════════════════════════

private fun stitchFrames(ctx: android.content.Context, paths: List<String>): String? {
    if (paths.isEmpty()) return null

    Log.i("Stitcher", "=== PANORAMA STITCHING PIPELINE ===")
    Log.i("Stitcher", "Input frames: ${paths.size}")

    // Step 1: Load frames as grayscale Mats + color Mats
    val grayMats = mutableListOf<org.opencv.core.Mat>()
    val colorMats = mutableListOf<org.opencv.core.Mat>()

    for ((i, path) in paths.withIndex()) {
        try {
            val bmp = BitmapFactory.decodeFile(path)
            if (bmp != null && !bmp.isRecycled && bmp.width > 100 && bmp.height > 100) {
                val colorMat = org.opencv.core.Mat()
                org.opencv.android.Utils.bitmapToMat(bmp, colorMat)
                val grayMat = org.opencv.core.Mat()
                org.opencv.imgproc.Imgproc.cvtColor(colorMat, grayMat, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY)
                grayMats.add(grayMat)
                colorMats.add(colorMat)
                Log.i("Stitcher", "Frame $i: ${bmp.width}x${bmp.height}")
                bmp.recycle()
            } else {
                Log.w("Stitcher", "Frame $i: SKIPPED (${bmp?.width}x${bmp?.height})")
                bmp?.recycle()
            }
        } catch (e: Exception) {
            Log.e("Stitcher", "Frame $i: FAILED - ${e.message}")
        }
    }

    if (grayMats.size < 2) {
        Log.e("Stitcher", "Not enough frames (${grayMats.size})")
        grayMats.forEach { it.release() }
        colorMats.forEach { it.release() }
        return null
    }
    Log.i("Stitcher", "Loaded ${grayMats.size} frames")

    // Step 2: Estimate focal length from image width (rough: FOV ~60°)
    val focalLength = grayMats[0].cols() * 1.2
    Log.i("Stitcher", "Estimated focal length: ${"%.0f".format(focalLength)}")

    // Step 3: Warp all frames to cylindrical projection
    val cylGray = mutableListOf<org.opencv.core.Mat>()
    val cylColor = mutableListOf<org.opencv.core.Mat>()
    for (i in grayMats.indices) {
        cylGray.add(cylindricalWarp(grayMats[i], focalLength))
        cylColor.add(cylindricalWarp(colorMats[i], focalLength))
        Log.i("Stitcher", "Cylindrical warp frame $i: ${cylGray.last().cols()}x${cylGray.last().rows()}")
    }

    // Step 4: Detect ORB features + match consecutive pairs
    val orb = org.opencv.features2d.ORB.create(5000)
    val matcher = org.opencv.features2d.BFMatcher(30, false) // NORM_HAMMING = 30

    // Store cumulative homographies: H_cumul[i] maps frame i → frame 0's coordinate space
    val cumulativeH = mutableListOf<org.opencv.core.Mat>()
    cumulativeH.add(org.opencv.core.Mat().eye(3, 3, org.opencv.core.CvType.CV_64F)) // frame 0 = identity

    var totalInliers = 0

    for (i in 0 until grayMats.size - 1) {
        val img1 = cylGray[i]
        val img2 = cylGray[i + 1]

        // Detect keypoints
        val kp1 = org.opencv.core.MatOfKeyPoint()
        val kp2 = org.opencv.core.MatOfKeyPoint()
        val des1 = org.opencv.core.Mat()
        val des2 = org.opencv.core.Mat()
        orb.detectAndCompute(img1, org.opencv.core.Mat(), kp1, des1)
        orb.detectAndCompute(img2, org.opencv.core.Mat(), kp2, des2)

        Log.i("Stitcher", "Pair $i→${i + 1}: kp1=${kp1.toArray().size} kp2=${kp2.toArray().size} des1=${des1.rows()} des2=${des2.rows()}")

        if (des1.empty() || des2.empty() || des1.rows() < 10 || des2.rows() < 10) {
            Log.w("Stitcher", "Pair $i→${i + 1}: NOT ENOUGH FEATURES, skipping")
            // Use identity (no alignment)
            cumulativeH.add(cumulativeH.last().clone())
            kp1.release(); kp2.release(); des1.release(); des2.release()
            continue
        }

        // Match descriptors (k=2 for ratio test)
        val matchList = mutableListOf<org.opencv.core.MatOfDMatch>()
        matcher.knnMatch(des1, des2, matchList, 2)

        // Lowe's ratio test
        val goodMatches = mutableListOf<org.opencv.features2d.DMatch>()
        for (pair in matchList) {
            val m = pair.toArray()
            if (m.size >= 2 && m[0].distance < 0.75f * m[1].distance) {
                goodMatches.add(m[0])
            }
            pair.release()
        }
        Log.i("Stitcher", "Pair $i→${i + 1}: ${goodMatches.size} good matches (ratio test)")

        kp1.release(); kp2.release(); des1.release(); des2.release()

        if (goodMatches.size < 10) {
            Log.w("Stitcher", "Pair $i→${i + 1}: TOO FEW matches, skipping")
            cumulativeH.add(cumulativeH.last().clone())
            continue
        }

        // Compute homography with RANSAC
        val srcPts = org.opencv.core.MatOfPoint2f()
        val dstPts = org.opencv.core.MatOfPoint2f()
        val kp1Arr = kp1.toArray()
        val kp2Arr = kp2.toArray()
        srcPts.from(goodMatches.map { org.opencv.core.Point(kp1Arr[it.queryIdx].pt.x.toDouble(), kp1Arr[it.queryIdx].pt.y.toDouble()) }.toTypedArray())
        dstPts.from(goodMatches.map { org.opencv.core.Point(kp2Arr[it.trainIdx].pt.x.toDouble(), kp2Arr[it.trainIdx].pt.y.toDouble()) }.toTypedArray())

        val inliers = org.opencv.core.Mat()
        val H = org.opencv.calib3d.Calib3d.findHomography(srcPts, dstPts, org.opencv.calib3d.Calib3d.RANSAC, 5.0, inliers, 2000, 0.995)
        val inlierCount = org.opencv.core.Core.countNonZero(inliers)
        totalInliers += inlierCount
        Log.i("Stitcher", "Pair $i→${i + 1}: H computed, $inlierCount inliers")

        srcPts.release(); dstPts.release(); inliers.release()

        if (H.empty() || inlierCount < 10) {
            Log.w("Stitcher", "Pair $i→${i + 1}: BAD HOMOGRAPHY, skipping")
            cumulativeH.add(cumulativeH.last().clone())
            H.release()
            continue
        }

        // Chain: cumulativeH[i+1] = H * cumulativeH[i]
        val chained = org.opencv.core.Mat()
        org.opencv.core.Core.gemm(H, cumulativeH.last(), 1.0, org.opencv.core.Mat(), 0.0, chained)
        cumulativeH.add(chained)
        H.release()
    }

    // Release gray Mats (keep color for blending)
    grayMats.forEach { it.release() }
    cylGray.forEach { it.release() }

    Log.i("Stitcher", "Total inliers across all pairs: $totalInliers")

    // Step 5: Find bounding box of all warped frames
    var minX = 0.0; var minY = 0.0; var maxX = 0.0; var maxY = 0.0
    for (i in cylColor.indices) {
        val corners = arrayOf(
            org.opencv.core.Point(0.0, 0.0),
            org.opencv.core.Point(cylColor[i].cols().toDouble(), 0.0),
            org.opencv.core.Point(cylColor[i].cols().toDouble(), cylColor[i].rows().toDouble()),
            org.opencv.core.Point(0.0, cylColor[i].rows().toDouble())
        )
        val dst = arrayOfNulls<org.opencv.core.Point>(4)
        for (j in 0..3) {
            dst[j] = org.opencv.core.Point()
            val hv = org.opencv.core.Mat(3, 1, org.opencv.core.CvType.CV_64F)
            hv.put(0, 0, corners[j].x, corners[j].y, 1.0)
            val result = org.opencv.core.Mat()
            org.opencv.core.Core.gemm(cumulativeH[i], hv, 1.0, org.opencv.core.Mat(), 0.0, result)
            val data = DoubleArray(3)
            result.get(0, 0, data)
            dst[j]!!.x = data[0] / data[2]
            dst[j]!!.y = data[1] / data[2]
            hv.release(); result.release()
            minX = minOf(minX, dst[j]!!.x); minY = minOf(minY, dst[j]!!.y)
            maxX = maxOf(maxX, dst[j]!!.x); maxY = maxOf(maxY, dst[j]!!.y)
        }
    }

    val canvasW = (maxX - minX).toInt() + 1
    val canvasH = (maxY - minY).toInt() + 1
    Log.i("Stitcher", "Canvas size: ${canvasW}x${canvasH}")

    // Cap canvas to prevent OOM
    if (canvasW > 12000 || canvasH > 6000 || canvasW * canvasH > 40_000_000) {
        Log.e("Stitcher", "Canvas too large, aborting")
        cylColor.forEach { it.release() }
        cumulativeH.forEach { it.release() }
        return null
    }

    // Translation matrix to shift everything to positive coordinates
    val T = org.opencv.core.Mat.eye(3, 3, org.opencv.core.CvType.CV_64F)
    T.put(0, 2, -minX)
    T.put(1, 2, -minY)

    // Step 6: Warp all frames onto the canvas + accumulate weights for blending
    val canvas = org.opencv.core.Mat.zeros(canvasH, canvasW, org.opencv.core.CvType.CV_32FC3)
    val weights = org.opencv.core.Mat.zeros(canvasH, canvasW, org.opencv.core.CvType.CV_32FC1)

    for (i in cylColor.indices) {
        val fullH = org.opencv.core.Mat()
        org.opencv.core.Core.gemm(T, cumulativeH[i], 1.0, org.opencv.core.Mat(), 0.0, fullH)

        val warped = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.warpPerspective(
            cylColor[i], warped, fullH,
            org.opencv.core.Size(canvasW.toDouble(), canvasH.toDouble())
        )

        // Create weight mask: distance from center (feathered)
        val mask = org.opencv.core.Mat.zeros(cylColor[i].size(), org.opencv.core.CvType.CV_32FC1)
        val cx = cylColor[i].cols() / 2.0
        val cy = cylColor[i].rows() / 2.0
        val maxDist = kotlin.math.sqrt(cx * cx + cy * cy)
        for (row in 0 until mask.rows()) {
            for (col in 0 until mask.cols()) {
                val d = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cy) * (row - cy))
                val w = (1.0 - d / maxDist).coerceIn(0.1, 1.0).toFloat()
                mask.put(row, col, w)
            }
        }

        val warpedMask = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.warpPerspective(
            mask, warpedMask, fullH,
            org.opencv.core.Size(canvasW.toDouble(), canvasH.toDouble())
        )

        // Accumulate
        org.opencv.core.Core.add(canvas, warped, canvas, warpedMask)
        org.opencv.core.Core.add(weights, warpedMask, weights)

        fullH.release(); warped.release(); mask.release(); warpedMask.release()
        Log.i("Stitcher", "Frame $i warped and blended onto canvas")
    }

    // Step 7: Normalize by weight
    val panorama = org.opencv.core.Mat()
    org.opencv.core.Core.divide(canvas, weights, panorama, 1.0, org.opencv.core.CvType.CV_8UC3)
    canvas.release(); weights.release(); T.release()
    cylColor.forEach { it.release() }
    cumulativeH.forEach { it.release() }

    Log.i("Stitcher", "Panorama normalized: ${panorama.cols()}x${panorama.rows()}")

    // Step 8: Crop black borders
    val cropped = cropBlackBorders(panorama)
    panorama.release()

    Log.i("Stitcher", "Cropped: ${cropped.cols()}x${cropped.rows()}, ratio: ${"%.2f".format(cropped.cols().toDouble() / cropped.rows())}")

    // Step 9: Save to file
    try {
        val resultBmp = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888)
        org.opencv.android.Utils.matToBitmap(cropped, resultBmp)
        cropped.release()

        val out = File(ctx.cacheDir, "panorama_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { resultBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        resultBmp.recycle()

        Log.i("Stitcher", "Saved: ${out.absolutePath}")
        Log.i("Stitcher", "=== STITCHING COMPLETE ===")
        return out.absolutePath
    } catch (e: Exception) {
        Log.e("Stitcher", "Save failed: ${e.message}")
        cropped.release()
        return null
    }
}

/** Warp an image to cylindrical coordinates */
private fun cylindricalWarp(src: org.opencv.core.Mat, f: Double): org.opencv.core.Mat {
    val h = src.rows()
    val w = src.cols()
    val cx = w / 2.0
    val cy = h / 2.0
    val dst = org.opencv.core.Mat.zeros(h, w, src.type())

    for (y in 0 until h) {
        for (x in 0 until w) {
            val dx = x - cx
            val dy = y - cy
            val denom = kotlin.math.sqrt(dx * dx + f * f)
            val newX = (f * kotlin.math.atan2(dx, f) + cx).toInt()
            val newY = (f * dy / denom + cy).toInt()
            if (newX in 0 until w && newY in 0 until h) {
                dst.put(newY, newX, *src.get(y, x))
            }
        }
    }
    return dst
}

/** Crop black borders from a stitched panorama */
private fun cropBlackBorders(src: org.opencv.core.Mat): org.opencv.core.Mat {
    val gray = org.opencv.core.Mat()
    org.opencv.imgproc.Imgproc.cvtColor(src, gray, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY)
    val thresh = org.opencv.core.Mat()
    org.opencv.imgproc.Imgproc.threshold(gray, thresh, 5.0, 255.0, org.opencv.imgproc.Imgproc.THRESH_BINARY)
    val contours = java.util.ArrayList<org.opencv.core.MatOfPoint>()
    val hierarchy = org.opencv.core.Mat()
    org.opencv.imgproc.Imgproc.findContours(thresh, contours, hierarchy, org.opencv.imgproc.Imgproc.RETR_EXTERNAL, org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE)
    gray.release(); thresh.release(); hierarchy.release()

    if (contours.isEmpty()) return src

    // Find the largest contour (the stitched area)
    val largest = contours.maxByOrNull { org.opencv.imgproc.Imgproc.contourArea(it) } ?: return src
    val rect = org.opencv.imgproc.Imgproc.boundingRect(largest)
    contours.forEach { it.release() }

    // Crop with small padding
    val pad = 2
    val x = (rect.x - pad).coerceAtLeast(0)
    val y = (rect.y - pad).coerceAtLeast(0)
    val w = (rect.width + pad * 2).coerceAtMost(src.cols() - x)
    val h = (rect.height + pad * 2).coerceAtMost(src.rows() - y)
    return org.opencv.core.Mat(src, org.opencv.core.Rect(x, y, w, h)).clone()
}

private fun buildJson(stitchedPath: String?, frames: List<String>, roomId: String): String {
    val json = JSONObject()
    if (stitchedPath != null) json.put("stitchedPanorama", stitchedPath)
    val arr = JSONArray()
    frames.forEach { arr.put(it) }
    json.put("frames", arr)
    json.put("frameCount", frames.size)
    json.put("roomId", roomId)
    json.put("timestamp", System.currentTimeMillis())
    return json.toString()
}

// ═════════════════════════════════════════════════════════════
//  HELPERS
// ═════════════════════════════════════════════════════════════

private fun vibrateShutter(ctx: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.let { v ->
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val v = ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1))
        }
    } catch (_: Exception) {}
}
