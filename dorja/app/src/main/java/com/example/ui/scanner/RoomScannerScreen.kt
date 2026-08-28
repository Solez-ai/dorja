package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.ui.components.BentoCard
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ── Scan phase enum ──────────────────────────────────────────
enum class ScanPhase { ROOM_SELECT, PRE_CAPTURE, SCANNING, RESULT }

// ── Target angle constant ────────────────────────────────────
private const val TOTAL_ANGLES = 12
private const val ANGLE_STEP = 30          // degrees between targets
private const val ALIGNMENT_THRESHOLD = 20 // ± degrees to count as aligned
private const val HOLD_TIME_MS = 350L      // ms to hold alignment before auto-capture

// ── Cyan accent used across scanner overlays ──────────────────
private val CyanAccent = Color(0xFF00BCD4)

@Composable
fun RoomScannerScreen(
    listingId: String,
    onBack: () -> Unit,
    onScanComplete: (roomId: String, panoramaJson: String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // ── Room data ────────────────────────────────────────────
    val rooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    // ── Phase state ──────────────────────────────────────────
    var phase by remember { mutableStateOf(ScanPhase.ROOM_SELECT) }
    var selectedRoom by remember { mutableStateOf<RoomItem?>(null) }

    // ── Camera state ─────────────────────────────────────────
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // ── Gyroscope state ──────────────────────────────────────
    var currentYaw by remember { mutableFloatStateOf(0f) }
    var gyroEnabled by remember { mutableStateOf(true) }
    var currentHeading by remember { mutableStateOf(0f) } // 0-360 device heading

    // ── Scan state ───────────────────────────────────────────
    val capturedAngles = remember { mutableStateListOf<Int>() }
    var currentTargetIndex by remember { mutableIntStateOf(0) }
    var alignmentStatus by remember { mutableStateOf("Rotate to angle 0°") }
    var isAligned by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var captureCount by remember { mutableIntStateOf(0) }

    // ── Panorama data ────────────────────────────────────────
    val framePaths = remember { mutableStateListOf<String>() }

    // ── Target angles (0, 30, 60 ... 330) ────────────────────
    val targetAngles = remember {
        List(TOTAL_ANGLES) { it * ANGLE_STEP }
    }

    // ── Sensor listener ──────────────────────────────────────
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val rotationVector = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    DisposableEffect(sensorManager, gyroEnabled) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR && gyroEnabled) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    // orientationAngles[0] = yaw (azimuth) in radians
                    val yawDegrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    currentYaw = yawDegrees
                    // Normalize to 0..360
                    currentHeading = ((yawDegrees % 360f) + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager?.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // ── Alignment detection + auto-capture during SCANNING ───
    LaunchedEffect(phase, currentTargetIndex, currentHeading, capturedAngles) {
        if (phase != ScanPhase.SCANNING) return@LaunchedEffect
        if (currentTargetIndex >= targetAngles.size) return@LaunchedEffect

        val target = targetAngles[currentTargetIndex]
        if (target in capturedAngles) {
            // Already captured, move to next
            val nextIdx = (currentTargetIndex + 1).coerceAtMost(targetAngles.size)
            currentTargetIndex = nextIdx
            if (nextIdx >= targetAngles.size) {
                phase = ScanPhase.RESULT
            }
            return@LaunchedEffect
        }

        val diff = angleDifference(currentHeading, target.toFloat())
        isAligned = abs(diff) <= ALIGNMENT_THRESHOLD

        if (isAligned) {
            alignmentStatus = "ALIGNED — CAPTURING"
            // Hold timer
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < HOLD_TIME_MS && isAligned) {
                holdProgress = ((System.currentTimeMillis() - startTime).toFloat() / HOLD_TIME_MS).coerceIn(0f, 1f)
                delay(16) // ~60fps
            }
            holdProgress = 0f
            // Verify still aligned after hold
            val recheckDiff = angleDifference(currentHeading, target.toFloat())
            if (abs(recheckDiff) <= ALIGNMENT_THRESHOLD) {
                // AUTO-CAPTURE
                imageCapture?.takePicture(
                    ImageCapture.OutputFileOptions.Builder(
                        File(context.cacheDir, "pano_${target}_${System.currentTimeMillis()}.jpg")
                    ).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri
                            val path = savedUri?.toString()
                                ?: File(
                                    context.cacheDir,
                                    "pano_${target}_${System.currentTimeMillis()}.jpg"
                                ).absolutePath
                            if (path !in framePaths) {
                                framePaths.add(path)
                                capturedAngles.add(target)
                                captureCount++
                                // Haptic feedback — double-tap buzz
                                performCaptureHaptic(context)
                                // Move to next angle
                                val nextIdx = currentTargetIndex + 1
                                if (nextIdx >= targetAngles.size) {
                                    phase = ScanPhase.RESULT
                                } else {
                                    currentTargetIndex = nextIdx
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("RoomScanner", "Capture failed: ${exception.message}", exception)
                        }
                    }
                )
            }
        } else {
            holdProgress = 0f
            alignmentStatus = "Rotate to angle $target°"
        }
    }

    // ── Permission gate ──────────────────────────────────────
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ── Main render ──────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (phase) {
            ScanPhase.ROOM_SELECT -> RoomSelectPhase(
                rooms = rooms,
                onSelect = { room ->
                    selectedRoom = room
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    phase = ScanPhase.PRE_CAPTURE
                },
                onBack = onBack
            )

            ScanPhase.PRE_CAPTURE -> PreCapturePhase(
                imageCapture = imageCapture,
                onImageCaptureReady = { imageCapture = it },
                hasCameraPermission = hasCameraPermission,
                roomName = selectedRoom?.displayName ?: "Room",
                gyroEnabled = gyroEnabled,
                onToggleGyro = { gyroEnabled = !gyroEnabled },
                onStartScan = { phase = ScanPhase.SCANNING },
                onBack = { phase = ScanPhase.ROOM_SELECT }
            )

            ScanPhase.SCANNING -> ScanningPhase(
                imageCapture = imageCapture,
                onImageCaptureReady = { imageCapture = it },
                hasCameraPermission = hasCameraPermission,
                targetAngles = targetAngles,
                currentTargetIndex = currentTargetIndex,
                capturedAngles = capturedAngles,
                captureCount = captureCount,
                currentHeading = currentHeading,
                alignmentStatus = alignmentStatus,
                isAligned = isAligned,
                holdProgress = holdProgress,
                gyroEnabled = gyroEnabled,
                onToggleGyro = { gyroEnabled = !gyroEnabled },
                onStop = { phase = ScanPhase.RESULT },
                onBack = { phase = ScanPhase.PRE_CAPTURE }
            )

            ScanPhase.RESULT -> ResultPhase(
                roomName = selectedRoom?.displayName ?: "Room",
                frameCount = framePaths.size,
                coveragePercent = (framePaths.size.toFloat() / TOTAL_ANGLES * 100f).toInt(),
                onSave = {
                    scope.launch {
                        // Stitch frames into equirectangular panorama
                        val stitchedPath = stitchPanorama(context, framePaths.toList())
                        val json = if (stitchedPath != null) {
                            buildStitchedPanoramaJson(stitchedPath, framePaths.toList(), selectedRoom?.id ?: "")
                        } else {
                            buildPanoramaJson(framePaths.toList(), selectedRoom?.id ?: "")
                        }
                        repository.updateRoom3DScan(selectedRoom?.id ?: "", json)
                        onScanComplete(selectedRoom?.id ?: "", json)
                    }
                },
                onBack = { phase = ScanPhase.PRE_CAPTURE },
                onDiscard = {
                    framePaths.clear()
                    capturedAngles.clear()
                    captureCount = 0
                    currentTargetIndex = 0
                    phase = ScanPhase.ROOM_SELECT
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 1: Room Selection
// ═══════════════════════════════════════════════════════════════
@Composable
private fun RoomSelectPhase(
    rooms: List<RoomItem>,
    onSelect: (RoomItem) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(DorjaColors.Ink950)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(DorjaColors.Gray700)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Select Room to Scan",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("3D Panorama Scanner", style = MaterialTheme.typography.bodySmall, color = CyanAccent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CyanAccent.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ViewInAr, null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Cylindrical 360° Panorama",
                            style = MaterialTheme.typography.titleSmall,
                            color = DorjaColors.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Captures 12 real frames at 30° intervals using CameraX",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Sand300,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (rooms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MeetingRoom,
                            null,
                            tint = DorjaColors.Gray500,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Rooms Added", style = MaterialTheme.typography.titleMedium, color = DorjaColors.White)
                        Text(
                            "Add rooms to your listing first",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Sand300
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rooms) { room ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(room) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (room.has3DScan) CyanAccent.copy(alpha = 0.1f) else DorjaColors.Gray700,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (room.has3DScan) CyanAccent else DorjaColors.Sand300.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                                        .background(if (room.has3DScan) CyanAccent.copy(alpha = 0.2f) else DorjaColors.Ink950),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom,
                                        null,
                                        tint = if (room.has3DScan) CyanAccent else DorjaColors.Sand300,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        room.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DorjaColors.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        room.roomType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Sand300,
                                        fontSize = 11.sp
                                    )
                                }
                                if (room.has3DScan) {
                                    DorjaBadge(
                                        text = "SCANNED",
                                        backgroundColor = CyanAccent.copy(alpha = 0.2f),
                                        textColor = CyanAccent
                                    )
                                } else {
                                    DorjaButton(
                                        text = "Scan 3D",
                                        onClick = { onSelect(room) },
                                        modifier = Modifier.height(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 2: Pre-Capture (Camera Preview with overlay)
// ═══════════════════════════════════════════════════════════════
@Composable
private fun PreCapturePhase(
    imageCapture: ImageCapture?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    hasCameraPermission: Boolean,
    roomName: String,
    gyroEnabled: Boolean,
    onToggleGyro: () -> Unit,
    onStartScan: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize CameraX
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()

            val previewView = PreviewView(context)
            val previewUseCase = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val captureUseCase = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(android.view.Surface.ROTATION_0)
                .build()
            onImageCaptureReady(captureUseCase)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, captureUseCase)
        } catch (e: Exception) {
            Log.e("RoomScanner", "Camera init failed", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setTargetRotation(android.view.Surface.ROTATION_0)
                                .build()
                            onImageCaptureReady(capture)
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                Log.e("RoomScanner", "Bind failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, null, tint = DorjaColors.Warning, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Camera permission required",
                        color = DorjaColors.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DorjaButton(text = "Grant Permission", onClick = { /* handled by launcher */ })
                }
            }
        }

        // Cylindrical dot-grid overlay
        DotGridOverlay()

        // Top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                .align(Alignment.TopCenter)
        )

        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 12.dp, end = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SCANNING",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    roomName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.size(38.dp))
        }

        // Center instruction card
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Phone icon with green badge
                    Box {
                        Icon(
                            Icons.Default.CameraAlt, null,
                            tint = Color.White, modifier = Modifier.size(32.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hold your phone upright\nin portrait mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom instruction
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "PRESS TO START" label with down-caret
            Text(
                "PRESS TO START",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                Icons.Default.Settings, // using as caret-down substitute
                null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Green shutter button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onStartScan() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
            }
        }

        // Bottom bar: gyro toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings icon placeholder
            IconButton(
                onClick = { },
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            // Gyro toggle
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (gyroEnabled) CyanAccent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (gyroEnabled) CyanAccent else Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.clickable { onToggleGyro() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape)
                            .background(if (gyroEnabled) CyanAccent else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "GYRO ${if (gyroEnabled) "ON" else "OFF"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gyroEnabled) CyanAccent else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 3: Active Scanning
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ScanningPhase(
    imageCapture: ImageCapture?,
    onImageCaptureReady: (ImageCapture) -> Unit,
    hasCameraPermission: Boolean,
    targetAngles: List<Int>,
    currentTargetIndex: Int,
    capturedAngles: List<Int>,
    captureCount: Int,
    currentHeading: Float,
    alignmentStatus: String,
    isAligned: Boolean,
    holdProgress: Float,
    gyroEnabled: Boolean,
    onToggleGyro: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize CameraX
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        try {
            val previewView = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(android.view.Surface.ROTATION_0)
                .build()
            onImageCaptureReady(capture)
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        } catch (e: Exception) {
            Log.e("RoomScanner", "Camera init in scanning phase failed", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setTargetRotation(android.view.Surface.ROTATION_0)
                                .build()
                            onImageCaptureReady(capture)
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                Log.e("RoomScanner", "Bind failed in scan", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Compass ring overlay
        CompassRingOverlay(
            currentHeading = currentHeading,
            targetAngles = targetAngles,
            capturedAngles = capturedAngles,
            currentTargetIndex = currentTargetIndex
        )

        // Crosshair (aligned indicator)
        CrosshairOverlay(isAligned = isAligned)

        // Top gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)))
                .align(Alignment.TopCenter)
        )

        // Alignment banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAligned) CyanAccent.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
            ) {
                Text(
                    text = alignmentStatus,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 12.sp
                )
            }
        }

        // Hold progress ring (around center)
        if (holdProgress > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = 40.dp.toPx()
                val center = Offset(size.width / 2, size.height / 2)
                drawArc(
                    color = CyanAccent,
                    startAngle = -90f,
                    sweepAngle = holdProgress * 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
        }

        // Bottom gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .align(Alignment.BottomCenter)
        )

        // Progress strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            targetAngles.forEachIndexed { index, angle ->
                val color = when {
                    angle in capturedAngles -> Color(0xFF4CAF50) // Green = captured
                    index == currentTargetIndex -> CyanAccent     // Blue = current target
                    else -> Color.Gray.copy(alpha = 0.5f)         // Gray = pending
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }

        // Capture count
        Text(
            text = "$captureCount / ${targetAngles.size} FRAMES",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )

        // Stop button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Red stop button
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935))
                ) {
                    Icon(
                        Icons.Default.Stop,
                        "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "PRESS STOP WHEN DONE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }

        // Gyro toggle (bottom right)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (gyroEnabled) CyanAccent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (gyroEnabled) CyanAccent else Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 88.dp, start = 16.dp)
                .clickable { onToggleGyro() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(if (gyroEnabled) CyanAccent else Color.Gray)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "GYRO",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (gyroEnabled) CyanAccent else Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PHASE 4: Result / Save
// ═══════════════════════════════════════════════════════════════
@Composable
private fun ResultPhase(
    roomName: String,
    frameCount: Int,
    coveragePercent: Int,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDiscard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Success icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = CyanAccent, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Scan Complete",
                style = MaterialTheme.typography.headlineLarge,
                color = DorjaColors.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                "Panorama data captured for $roomName",
                style = MaterialTheme.typography.bodyMedium,
                color = DorjaColors.Sand300,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Scan summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryTile(label = "FRAMES", value = "$frameCount", modifier = Modifier.weight(1f))
                SummaryTile(label = "COVERAGE", value = "$coveragePercent%", modifier = Modifier.weight(1f))
                SummaryTile(label = "ANGLES", value = "${TOTAL_ANGLES}", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyanAccent.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ViewInAr, null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "360° REAL PANORAMA • $frameCount FRAMES STITCHED",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Cylindrical panorama saved to $roomName",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Sand300,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action buttons
            DorjaButton(
                text = "Save 3D Scan to $roomName",
                onClick = onSave,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DorjaOutlinedButton(text = "Retake", onClick = onBack, modifier = Modifier.weight(1f))
                DorjaOutlinedButton(text = "Discard", onClick = onDiscard, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Overlay composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DotGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dotRadius = 2.dp.toPx()
        val spacing = 24.dp.toPx()

        // Top edge dots curving inward
        for (i in 0..40) {
            val x = (i / 40f) * w
            val curveOffset = (kotlin.math.abs(i / 40f - 0.5f) * 2f) * 40.dp.toPx()
            drawCircle(CyanAccent.copy(alpha = 0.35f), dotRadius, Offset(x, 20.dp.toPx() + curveOffset))
        }

        // Bottom edge dots curving inward
        for (i in 0..40) {
            val x = (i / 40f) * w
            val curveOffset = (kotlin.math.abs(i / 40f - 0.5f) * 2f) * 40.dp.toPx()
            drawCircle(CyanAccent.copy(alpha = 0.35f), dotRadius, Offset(x, h - 20.dp.toPx() - curveOffset))
        }

        // Left side dots
        for (i in 0..20) {
            val y = (i / 20f) * h
            val curveOffset = (kotlin.math.abs(i / 20f - 0.5f) * 2f) * 30.dp.toPx()
            drawCircle(CyanAccent.copy(alpha = 0.25f), dotRadius, Offset(12.dp.toPx() + curveOffset, y))
        }

        // Right side dots
        for (i in 0..20) {
            val y = (i / 20f) * h
            val curveOffset = (kotlin.math.abs(i / 20f - 0.5f) * 2f) * 30.dp.toPx()
            drawCircle(CyanAccent.copy(alpha = 0.25f), dotRadius, Offset(w - 12.dp.toPx() - curveOffset, y))
        }
    }
}

@Composable
private fun CompassRingOverlay(
    currentHeading: Float,
    targetAngles: List<Int>,
    capturedAngles: List<Int>,
    currentTargetIndex: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = (size.width * 0.38f)

        // Outer ring
        drawCircle(
            color = CyanAccent.copy(alpha = 0.2f),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(2.dp.toPx())
        )

        // Target angle nodes
        targetAngles.forEachIndexed { index, angle ->
            val rad = Math.toRadians((angle - 90).toDouble())
            val nodeX = centerX + radius * cos(rad).toFloat()
            val nodeY = centerY + radius * sin(rad).toFloat()

            val isCaptured = angle in capturedAngles
            val isCurrent = index == currentTargetIndex

            val nodeColor = when {
                isCaptured -> Color(0xFF4CAF50)
                isCurrent -> CyanAccent
                else -> Color.White.copy(alpha = 0.3f)
            }

            // Node circle
            drawCircle(
                color = nodeColor,
                radius = if (isCurrent) 8.dp.toPx() else 5.dp.toPx(),
                center = Offset(nodeX, nodeY)
            )

            // Angle label
            if (isCurrent || isCaptured) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = 14.dp.toPx(),
                    center = Offset(nodeX, nodeY - 18.dp.toPx())
                )
            }
        }

        // Current heading indicator (rotating)
        val headingRad = Math.toRadians((currentHeading - 90).toDouble())
        val indicatorX = centerX + radius * cos(headingRad).toFloat()
        val indicatorY = centerY + radius * sin(headingRad).toFloat()

        drawCircle(
            color = CyanAccent,
            radius = 4.dp.toPx(),
            center = Offset(indicatorX, indicatorY)
        )

        // Connecting line from center to current heading
        drawLine(
            color = CyanAccent.copy(alpha = 0.3f),
            start = Offset(centerX, centerY),
            end = Offset(indicatorX, indicatorY),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
private fun CrosshairOverlay(isAligned: Boolean) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isAligned) 1f else 0.4f,
        animationSpec = tween(200),
        label = "crosshair_alpha"
    )

    // Pulsing glow animation — only runs when aligned
    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val armLength = 30.dp.toPx()
        val gap = 12.dp.toPx()
        val strokeWidth = 2.dp.toPx()
        val color = if (isAligned) CyanAccent else Color.White.copy(alpha = animatedAlpha * 0.5f)

        // ── Pulsing glow when aligned ──────────────────────
        if (isAligned) {
            // Outer glow ring (large, soft)
            val glowRadiusOuter = 60.dp.toPx() * pulseScale
            drawCircle(
                color = CyanAccent.copy(alpha = 0.12f * pulseAlpha),
                radius = glowRadiusOuter,
                center = Offset(cx, cy)
            )
            // Mid glow ring
            val glowRadiusMid = 40.dp.toPx() * pulseScale
            drawCircle(
                color = CyanAccent.copy(alpha = 0.2f * pulseAlpha),
                radius = glowRadiusMid,
                center = Offset(cx, cy)
            )
            // Inner glow (tight, bright)
            val glowRadiusInner = 22.dp.toPx() * pulseScale
            drawCircle(
                color = CyanAccent.copy(alpha = 0.35f * pulseAlpha),
                radius = glowRadiusInner,
                center = Offset(cx, cy)
            )
            // Bright core halo
            drawCircle(
                color = CyanAccent.copy(alpha = 0.5f * pulseAlpha),
                radius = 10.dp.toPx() * pulseScale,
                center = Offset(cx, cy)
            )
        }

        // ── Crosshair arms ─────────────────────────────────
        // Top
        drawLine(color, Offset(cx, cy - gap), Offset(cx, cy - gap - armLength), strokeWidth)
        // Bottom
        drawLine(color, Offset(cx, cy + gap), Offset(cx, cy + gap + armLength), strokeWidth)
        // Left
        drawLine(color, Offset(cx - gap, cy), Offset(cx - gap - armLength, cy), strokeWidth)
        // Right
        drawLine(color, Offset(cx + gap, cy), Offset(cx + gap + armLength, cy), strokeWidth)

        // ── Center dot ─────────────────────────────────────
        val dotRadius = if (isAligned) 4.dp.toPx() * pulseScale else 3.dp.toPx()
        drawCircle(color, dotRadius, Offset(cx, cy))
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DorjaColors.Gray700
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = CyanAccent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Sand300,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════

private fun angleDifference(a: Float, b: Float): Float {
    var diff = a - b
    while (diff > 180f) diff -= 360f
    while (diff < -180f) diff += 360f
    return diff
}

private fun buildPanoramaJson(framePaths: List<String>, roomId: String): String {
    val json = JSONObject()
    val framesArray = JSONArray()
    framePaths.forEach { framesArray.put(it) }
    json.put("frames", framesArray)
    json.put("angleCount", framePaths.size)
    json.put("roomId", roomId)
    json.put("timestamp", System.currentTimeMillis())
    return json.toString()
}

/**
 * Stitches captured frames into a single equirectangular panorama bitmap.
 * Frames are arranged left-to-right based on their capture order.
 * Each frame is stretched to fit the equirectangular grid.
 */
private fun stitchPanorama(context: android.content.Context, framePaths: List<String>): String? {
    if (framePaths.isEmpty()) return null
    try {
        // Load all bitmaps
        val bitmaps = framePaths.mapNotNull { path ->
            try {
                val uri = if (path.startsWith("content://") || path.startsWith("file://")) {
                    android.net.Uri.parse(path)
                } else {
                    android.net.Uri.fromFile(java.io.File(path))
                }
                android.graphics.BitmapFactory.decodeStream(
                    context.contentResolver.openInputStream(uri)
                        ?: java.io.FileInputStream(java.io.File(path))
                )
            } catch (e: Exception) {
                Log.e("RoomScanner", "Failed to load frame: $path", e)
                null
            }
        }
        if (bitmaps.isEmpty()) return null

        // Target: equirectangular panorama
        // Width = sum of frame widths (each frame covers ~30°)
        // Height = max frame height (maintains aspect ratio)
        val frameHeight = bitmaps.maxOf { it.height }
        val totalWidth = bitmaps.sumOf { it.width }

        // Create stitched bitmap
        val stitched = android.graphics.Bitmap.createBitmap(
            totalWidth,
            frameHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(stitched)

        var xOffset = 0f
        bitmaps.forEach { bmp ->
            // Stretch each frame to fill its vertical space
            val destRect = android.graphics.RectF(
                xOffset, 0f,
                xOffset + bmp.width,
                frameHeight.toFloat()
            )
            canvas.drawBitmap(bmp, null, destRect, null)
            xOffset += bmp.width
        }

        // Save stitched panorama to cache
        val outFile = java.io.File(
            context.cacheDir,
            "stitched_panorama_${System.currentTimeMillis()}.jpg"
        )
        java.io.FileOutputStream(outFile).use { out ->
            stitched.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
        }

        // Recycle bitmaps
        bitmaps.forEach { it.recycle() }
        stitched.recycle()

        return outFile.absolutePath
    } catch (e: Exception) {
        Log.e("RoomScanner", "Panorama stitch failed", e)
        return null
    }
}

private fun buildStitchedPanoramaJson(stitchedPath: String, framePaths: List<String>, roomId: String): String {
    val json = JSONObject()
    json.put("stitchedPanorama", stitchedPath)
    val framesArray = JSONArray()
    framePaths.forEach { framesArray.put(it) }
    json.put("frames", framesArray)
    json.put("angleCount", framePaths.size)
    json.put("roomId", roomId)
    json.put("timestamp", System.currentTimeMillis())
    return json.toString()
}

/**
 * Triggers a short double-tap haptic buzz to confirm a frame was captured.
 * Uses VibrationEffect (API 26+) with a falling-intensity pattern,
 * or falls back to the legacy vibrate() call on older devices.
 */
private fun performCaptureHaptic(context: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator?.let { vibrate(it) }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let { vibrate(it) }
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(60)
        }
    } catch (_: Exception) {
        // Vibrator not available — silently skip
    }
}

/**
 * Double-tap buzz pattern: two short pulses with a gap.
 * First pulse is stronger, second is softer — mimics a camera shutter feel.
 */
private fun vibrate(vibrator: Vibrator) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val timings = longArrayOf(0, 30, 60, 20)     // delay, buzz1, pause, buzz2
        val amplitudes = intArrayOf(0, 200, 0, 120)   // strong, then softer
        vibrator.vibrate(
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        )
    }
}
