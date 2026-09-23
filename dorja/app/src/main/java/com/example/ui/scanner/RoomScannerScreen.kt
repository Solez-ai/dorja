package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as PanoCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Rect
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
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private enum class Phase { SELECT, PREVIEW, CAPTURING, DONE }

private val Accent = Color(0xFF00BCD4)
private val Green = Color(0xFF4CAF50)
private val TargetYellow = Color(0xFFFFC107)

/**
 * Zoom choices the picker offers, based on the bound camera's minimum zoom
 * ratio: 0.5x only when the device exposes an ultra-wide range, 0.8x when a
 * wide range exists, and 1x always. Ordered widest-first.
 */
private fun zoomChoices(minZoomRatio: Float): List<Float> = buildList {
    if (minZoomRatio <= 0.55f) add(0.5f)
    if (minZoomRatio <= 0.85f) add(0.8f)
    add(1f)
}

data class FrameData(
    val path: String,
    val heading: Float,
    val pitchDeg: Float = 0f,
    val row: Int = 0,
    val col: Int = 0,
    val isCap: Boolean = false,
    val capType: String? = null
)

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
    val rooms by repo.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    var phase by remember { mutableStateOf(Phase.SELECT) }
    var scanMode by remember { mutableStateOf(ScanGeometry.ScanMode.QUICK_SCAN) }
    var selectedRoom by remember { mutableStateOf<RoomItem?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    // Lens / zoom picker: 1.0x = main lens, 0.8x/0.5x = wider fields of view when
    // the device exposes them (digital zoom range of the bound logical camera).
    // Applied by CameraPreview; minZoomRatio feeds the supported-choice filter.
    var zoomRatio by remember { mutableStateOf(1f) }
    var minZoomRatio by remember { mutableStateOf(1f) }
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCamera = granted }

    var heading by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var gyroOn by remember { mutableStateOf(true) }
    val capturedFrames = remember { mutableStateListOf<FrameData>() }
    var currentTargetIdx by remember { mutableIntStateOf(0) }

    val scanTargets = remember(scanMode) { ScanGeometry.generateScanTargets(scanMode) }

    // Stitching progress & live preview states
    var stitchingStatus by remember { mutableStateOf<String?>(null) }
    var stitchingPreviewBmp by remember { mutableStateOf<Bitmap?>(null) }
    var stitchedPath by remember { mutableStateOf<String?>(null) }

    val sensorMgr = remember { ctx.getSystemService(SensorManager::class.java) }
    val rotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val gameRotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) }
    val accel = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val mag = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }

    val rotMatrix = remember { FloatArray(9) }
    val remappedMatrix = remember { FloatArray(9) }
    val orientAngles = remember { FloatArray(3) }
    // Smoothed orientation for stable AR-arrow anchoring (fast-out low-pass filter).
    var smoothHeading by remember { mutableStateOf<Float?>(null) }
    var smoothPitch by remember { mutableStateOf<Float?>(null) }
    val accelVals = remember { FloatArray(3) }
    val magVals = remember { FloatArray(3) }
    var hasAccel by remember { mutableStateOf(false) }
    var hasMag by remember { mutableStateOf(false) }

    DisposableEffect(sensorMgr, gyroOn) {
        if (sensorMgr == null || !gyroOn) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    val event = e ?: return
                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                            SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                            // REMAP COORDINATES FOR PORTRAIT CAMERA ORIENTATION (AXIS_X, AXIS_Z)
                            SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)
                            SensorManager.getOrientation(remappedMatrix, orientAngles)
                            val rawHeading = ((Math.toDegrees(orientAngles[0].toDouble()) % 360.0) + 360.0).toFloat() % 360f
                            val rawPitch = Math.toDegrees(orientAngles[1].toDouble()).toFloat()
                            // ARROW ANCHORING FIX: raw sensor frames jitter a few degrees,
                            // which made the projected target dot/arrow bounce up and down.
                            // Fuse readings through a light low-pass filter; fast motion
                            // snaps instantly so guidance still feels responsive.
                            val headingJump = abs(((rawHeading - heading + 540f) % 360f) - 180f)
                            val snapped = abs(rawPitch - pitch) > 10f || headingJump > 12f || smoothHeading == null
                            if (snapped) {
                                smoothHeading = rawHeading
                                smoothPitch = rawPitch
                            } else {
                                val sh = smoothHeading!!
                                val sp = smoothPitch!!
                                smoothHeading = sh + 0.25f * (((rawHeading - sh + 540f) % 360f) - 180f)
                                smoothPitch = sp + 0.25f * (rawPitch - sp)
                            }
                            heading = ((smoothHeading!! % 360f) + 360f) % 360f
                            pitch = smoothPitch!!
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, accelVals, 0, 3)
                            hasAccel = true
                            processAccelMagFallback()
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, magVals, 0, 3)
                            hasMag = true
                            processAccelMagFallback()
                        }
                    }
                }

                private fun processAccelMagFallback() {
                    if (rotVec == null && gameRotVec == null && hasAccel && hasMag) {
                        if (SensorManager.getRotationMatrix(rotMatrix, null, accelVals, magVals)) {
                            SensorManager.remapCoordinateSystem(rotMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)
                            SensorManager.getOrientation(remappedMatrix, orientAngles)
                            heading = ((Math.toDegrees(orientAngles[0].toDouble()) % 360.0) + 360.0).toFloat() % 360f
                            pitch = Math.toDegrees(orientAngles[1].toDouble()).toFloat()
                        }
                    }
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }

            if (rotVec != null) {
                sensorMgr.registerListener(listener, rotVec, SensorManager.SENSOR_DELAY_GAME)
            } else if (gameRotVec != null) {
                sensorMgr.registerListener(listener, gameRotVec, SensorManager.SENSOR_DELAY_GAME)
            } else {
                accel?.let { sensorMgr.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
                mag?.let { sensorMgr.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            }

            onDispose { sensorMgr.unregisterListener(listener) }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            cleanupFrameFiles(ctx, capturedFrames)
        }
    }

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
                scanMode = scanMode,
                zoomRatio = zoomRatio,
                minZoomRatio = minZoomRatio,
                onZoomPicked = { zoomRatio = it },
                gyroOn = gyroOn,
                onToggleGyro = { gyroOn = !gyroOn },
                onStart = {
                    phase = Phase.CAPTURING
                },
                onBack = { phase = Phase.SELECT },
                onCameraBound = { _, minZoom ->
                    if (minZoom > 0f) {
                        minZoomRatio = minZoom
                        zoomRatio = minZoom // default to widest supported lens
                    }
                },
                lifecycleOwner = lifecycleOwner
            )

            Phase.CAPTURING -> {
                CapturingPhase(
                        imageCapture = imageCapture,
                        onCaptureReady = { imageCapture = it },
                        hasCamera = hasCamera,
                        heading = heading,
                        currentPitch = pitch,
                        scanMode = scanMode,
                        scanTargets = scanTargets,
                        currentTargetIdx = currentTargetIdx,
                        capturedFrames = capturedFrames,
                        gyroOn = gyroOn,
                        onToggleGyro = { gyroOn = !gyroOn },
                        onRetakeTarget = { targetIndex -> currentTargetIdx = targetIndex },
                        onCapture = {
                            val ic = imageCapture ?: return@CapturingPhase
                            val target = scanTargets.getOrNull(currentTargetIdx) ?: return@CapturingPhase
                            val file = File(ctx.cacheDir, "frame_r${target.ringIndex}_c${target.targetIndex}_${System.currentTimeMillis()}.jpg")
                            val capturedHeading = heading
                            val capturedPitch = pitch

                            ic.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(),
                                ContextCompat.getMainExecutor(ctx),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val frame = FrameData(
                                            path = file.absolutePath,
                                            heading = capturedHeading,
                                            pitchDeg = capturedPitch,
                                            row = target.ringIndex,
                                            col = target.targetIndex,
                                            isCap = target.isCap,
                                            capType = target.capType
                                        )

                                        val existingIdx = capturedFrames.indexOfFirst { it.col == target.targetIndex }
                                        if (existingIdx >= 0) {
                                            capturedFrames[existingIdx] = frame
                                        } else {
                                            capturedFrames.add(frame)
                                        }

                                        vibrateShutter(ctx)
                                        if (currentTargetIdx < scanTargets.size - 1) {
                                            currentTargetIdx++
                                        }
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("Scanner", "Capture failed", exc)
                                    }
                                }
                            )
                        },
                        onStop = { phase = Phase.DONE },
                        onBack = { phase = Phase.PREVIEW },
                        lifecycleOwner = lifecycleOwner
                )
            }

            Phase.DONE -> DonePhase(
                roomName = selectedRoom?.displayName ?: "Room",
                frameCount = capturedFrames.size,
                capturedFrames = capturedFrames.toList(),
                stitchingStatus = stitchingStatus,
                stitchingPreviewBmp = stitchingPreviewBmp,
                stitchedPath = stitchedPath,
                onStitch = { frames ->
                    scope.launch {
                        stitchingStatus = "Stitching ${frames.size} frames on-device…"
                        val stitched = withContext(Dispatchers.IO) {
                            stitchFrames(ctx, frames)
                        }
                        if (stitched != null) {
                            stitchedPath = stitched
                            stitchingPreviewBmp = withContext(Dispatchers.IO) {
                                BitmapFactory.decodeFile(stitched)
                            }
                            stitchingStatus = "Panorama ready — tune lighting below if needed"
                        } else {
                            stitchingStatus = "Stitching failed — retake the scan and pause at every target"
                        }
                    }
                },
                onApplyLighting = { path, brightness, contrast, warmth ->
                    scope.launch {
                        stitchingStatus = "Applying lighting adjustments…"
                        val tuned = withContext(Dispatchers.IO) {
                            applyLightingAdjustments(path, brightness, contrast, warmth)
                        }
                        if (tuned != null) {
                            stitchedPath = tuned
                            stitchingPreviewBmp = withContext(Dispatchers.IO) {
                                BitmapFactory.decodeFile(tuned)
                            }
                            stitchingStatus = "Lighting applied"
                        } else {
                            stitchingStatus = "Could not apply adjustments"
                        }
                    }
                },
                onSave = {
                    scope.launch {
                        try {
                            val path = stitchedPath
                            if (path == null) {
                                stitchingStatus = "Panorama is not ready yet."
                                return@launch
                            }
                            val frames = capturedFrames.toList()
                            val json = buildJson(path, frames, selectedRoom?.id ?: "")
                            withContext(Dispatchers.IO) {
                                repo.updateRoom3DScan(selectedRoom?.id ?: "", json)
                            }
                            onScanComplete(selectedRoom?.id ?: "", json)
                        } catch (e: Exception) {
                            Log.e("Scanner", "Save failed", e)
                            stitchingStatus = "Error: ${e.message}"
                        }
                    }
                },
                onRetake = {
                    cleanupFrameFiles(ctx, capturedFrames)
                    if (stitchedPath != null) File(stitchedPath!!).delete()
                    capturedFrames.clear()
                    currentTargetIdx = 0
                    stitchingStatus = null
                    stitchingPreviewBmp = null
                    stitchedPath = null
                    phase = Phase.PREVIEW
                },
                onDiscard = {
                    cleanupFrameFiles(ctx, capturedFrames)
                    if (stitchedPath != null) File(stitchedPath!!).delete()
                    capturedFrames.clear()
                    currentTargetIdx = 0
                    stitchingStatus = null
                    stitchingPreviewBmp = null
                    stitchedPath = null
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
    Column(Modifier.fillMaxSize().background(DorjaColors.CanvasBg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.Ink950)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Select Room to Scan", color = DorjaColors.Ink950, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("DORJA 360° Panorama Scanner", color = DorjaColors.Jol600, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(14.dp))

        if (rooms.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MeetingRoom, null, tint = DorjaColors.Gray500, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No Rooms Added", color = DorjaColors.Ink950, style = MaterialTheme.typography.titleMedium)
                    Text("Add rooms to your listing first", color = DorjaColors.Gray600)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rooms) { room ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { onSelect(room) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (room.has3DScan) DorjaColors.Jol100 else DorjaColors.BentoCardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (room.has3DScan) DorjaColors.Jol600 else DorjaColors.BentoCardBorder)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(if (room.has3DScan) Accent.copy(alpha = 0.2f) else DorjaColors.Sand100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom, null, tint = if (room.has3DScan) DorjaColors.Jol600 else DorjaColors.Gray500, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(room.displayName, color = DorjaColors.Ink950, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(room.roomType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = DorjaColors.Gray600, fontSize = 11.sp)
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
//  PHASE 2 — PREVIEW & TUTORIAL
// ═════════════════════════════════════════════════════════════
@Composable
private fun PreviewPhase(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    roomName: String,
    scanMode: ScanGeometry.ScanMode,
    zoomRatio: Float,
    minZoomRatio: Float,
    onZoomPicked: (Float) -> Unit,
    gyroOn: Boolean,
    onToggleGyro: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    onCameraBound: (Camera, Float) -> Unit = { _, _ -> },
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner, zoomRatio, onCameraBound)
        ScopeOverlay()
        Box(Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))).align(Alignment.TopCenter))
        Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DORJA 360° PANORAMA", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(38.dp))
        }

        // Clear 3-Step Visual Guidance Card
        Box(Modifier.align(Alignment.Center).padding(24.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.75f), border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.4f))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HOW TO CAPTURE A PANORAMA", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("1. Stand in middle of room (pivot like a tripod)", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("2. Move phone to align center into green AR target ring", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("3. Follow the 3D arrows all the way around", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Lens zoom picker — chosen BEFORE the scan starts so every frame
            // shares the same field of view. Options reflect what the bound
            // camera actually supports (min zoom ratio of the device).
            Text("LENS ZOOM — PICK BEFORE SCANNING", color = Accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                zoomChoices(minZoomRatio).forEach { z ->
                    val selected = abs(zoomRatio - z) < 0.01f
                    Surface(
                        onClick = { onZoomPicked(z) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) Accent.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (selected) Accent else Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            if (z == 1f) "1x" else "${z}x",
                            color = if (selected) Accent else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("PRESS TO START SCAN", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).border(3.dp, Color.White, CircleShape).clickable { onStart() }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Green))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) {
            GyroChip(gyroOn, onToggleGyro)
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 3 — CAPTURING (INTERACTIVE 3D AR GUIDANCE)
// ═════════════════════════════════════════════════════════════
@Composable
private fun CapturingPhase(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    heading: Float,
    currentPitch: Float,
    scanMode: ScanGeometry.ScanMode,
    scanTargets: List<ScanGeometry.ScanTarget>,
    currentTargetIdx: Int,
    capturedFrames: SnapshotStateList<FrameData>,
    gyroOn: Boolean,
    onToggleGyro: () -> Unit,
    onRetakeTarget: (Int) -> Unit,
    onCapture: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val target = scanTargets.getOrNull(currentTargetIdx) ?: scanTargets.last()

    val pitchError = currentPitch - target.pitchDeg
    val headingError = ((heading - target.headingDeg + 540) % 360) - 180
    val normalizedDelta = abs(minOf(abs(pitchError), abs(headingError)))

    val onPitchTarget = abs(pitchError) <= 12f
    val onHeadingTarget = target.isCap || abs(headingError) <= 12f
    val isLocked = onPitchTarget && onHeadingTarget

    val guidanceText = when {
        target.isCap && target.capType == "zenith" -> if (currentPitch > 75f) "LOCK AT CEILING" else "POINT AT CEILING (+90°)"
        target.isCap && target.capType == "nadir" -> if (currentPitch < -75f) "LOCK AT NADIR" else "POINT AT FLOOR (-90°)"
        pitchError < -12f -> "TILT UP ${"%.0f".format(abs(pitchError))}°"
        pitchError > 12f -> "TILT DOWN ${"%.0f".format(abs(pitchError))}°"
        headingError > 12f -> "TURN LEFT ${"%.0f".format(abs(headingError))}°"
        headingError < -12f -> "TURN RIGHT ${"%.0f".format(abs(headingError))}°"
        else -> "ALIGNED — TAP SHUTTER"
    }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)

        // INTERACTIVE 3D AR TARGET RETICLE & DIRECTIONAL ARROWS OVERLAY
        ArTargetOverlay(
            heading = heading,
            currentPitch = currentPitch,
            targetHeading = target.headingDeg,
            targetPitch = target.pitchDeg,
            isLocked = isLocked,
            isCap = target.isCap
        )

        // Top Status Header
        Box(Modifier.fillMaxWidth().height(65.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))).align(Alignment.TopCenter))
        Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.65f), modifier = Modifier.align(Alignment.TopCenter).padding(top = 45.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SHOT ${currentTargetIdx + 1}/${scanTargets.size} • RING ${target.ringIndex} • TURN ${"%.0f".format(normalizedDelta)}°",
                    color = if (isLocked) Green else Accent,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }

        // Vertical Ring Rail (Right Edge)
        VerticalRingRail(
            scanMode = scanMode,
            currentRing = target.ringIndex,
            scanTargets = scanTargets,
            capturedFrames = capturedFrames,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
        )

        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))).align(Alignment.BottomCenter))

        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            LastShotThumbnail(capturedFrames, capturedFrames.size, Modifier.align(Alignment.CenterVertically))
            Spacer(Modifier.width(18.dp))
            Box(
                Modifier.size(68.dp).clip(CircleShape)
                    .background(if (isLocked) Green.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f))
                    .border(3.dp, if (isLocked) Green else Color.White, CircleShape)
                    .clickable { onCapture() },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(if (isLocked) Green else Color.White.copy(alpha = 0.9f)))
            }
            Spacer(Modifier.width(18.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE53935)).border(2.dp, Color.White, CircleShape).clickable { onStop() }, contentAlignment = Alignment.Center) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
            }
        }

        Box(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 130.dp)) {
            GyroChip(gyroOn, onToggleGyro)
        }

        // Guidance Badge Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isLocked) Green else TargetYellow),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 95.dp)
        ) {
            Text(
                guidanceText,
                color = if (isLocked) Green else TargetYellow,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Interactive 3D AR Target Overlay:
 * Projects the target sphere angle (heading, pitch) onto the 2D camera preview.
 * Draws an AR target ring that smoothly glides into center crosshair as phone aligns,
 * plus a 3D directional arrow pointing toward off-screen targets!
 */
@Composable
private fun ArTargetOverlay(
    heading: Float,
    currentPitch: Float,
    targetHeading: Float,
    targetPitch: Float,
    isLocked: Boolean,
    isCap: Boolean
) {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Projection scale mapping degrees to preview pixels (assuming ~60° hFOV)
        val pxPerDeg = size.width / 60f

        val relHeading = ((targetHeading - heading + 540) % 360) - 180
        val relPitch = targetPitch - currentPitch

        val targetX = cx + (relHeading * pxPerDeg)
        val targetY = cy - (relPitch * pxPerDeg)

        val isTargetOnScreen = targetX in 0f..size.width && targetY in 0f..size.height

        val ringColor = if (isLocked) Green else TargetYellow
        val ringRadius = if (isLocked) 34.dp.toPx() else 28.dp.toPx()

        // 1. Center Screen Crosshair
        drawCircle(Color.White.copy(alpha = 0.3f), 40.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
        drawCircle(if (isLocked) Green else Color.White, 4.dp.toPx(), Offset(cx, cy))

        if (isTargetOnScreen || isCap) {
            // 2. Projected 3D AR Target Ring
            val drawX = if (isCap) cx else targetX
            val drawY = if (isCap) (cy - relPitch * pxPerDeg).coerceIn(40.dp.toPx(), size.height - 40.dp.toPx()) else targetY

            // Outer ring
            drawCircle(ringColor.copy(alpha = 0.3f), ringRadius * 1.4f, Offset(drawX, drawY))
            drawCircle(ringColor, ringRadius, Offset(drawX, drawY), style = Stroke(2.5.dp.toPx()))
            drawCircle(ringColor, 6.dp.toPx(), Offset(drawX, drawY))

            // Line connecting center to target
            if (!isLocked) {
                drawLine(ringColor.copy(alpha = 0.5f), Offset(cx, cy), Offset(drawX, drawY), 1.5.dp.toPx())
            }
        } else {
            // 3. 3D Directional AR Arrow pointing off-screen toward target
            val dx = targetX - cx
            val dy = targetY - cy
            val angle = atan2(dy, dx)

            val arrowDist = size.width * 0.38f
            val arrowX = cx + arrowDist * cos(angle)
            val arrowY = cy + arrowDist * sin(angle)

            val arrowSize = 16.dp.toPx()

            // Pointer Arrow head
            drawContext.canvas.save()
            drawContext.canvas.translate(arrowX, arrowY)
            drawContext.canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat() + 90f)

            val p1 = Offset(0f, -arrowSize)
            val p2 = Offset(-arrowSize * 0.6f, arrowSize * 0.6f)
            val p3 = Offset(arrowSize * 0.6f, arrowSize * 0.6f)

            drawLine(TargetYellow, p1, p2, 3.dp.toPx())
            drawLine(TargetYellow, p2, Offset(0f, 0f), 3.dp.toPx())
            drawLine(TargetYellow, Offset(0f, 0f), p3, 3.dp.toPx())
            drawLine(TargetYellow, p3, p1, 3.dp.toPx())

            drawContext.canvas.restore()

            // Line connecting center to arrow
            drawLine(TargetYellow.copy(alpha = 0.4f), Offset(cx, cy), Offset(arrowX, arrowY), 2.dp.toPx())
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 4 — DONE / STITCHING & LIVE PREVIEW
// ═════════════════════════════════════════════════════════════
@Composable
private fun DonePhase(
    roomName: String,
    frameCount: Int,
    capturedFrames: List<FrameData>,
    stitchingStatus: String?,
    stitchingPreviewBmp: Bitmap?,
    stitchedPath: String?,
    onStitch: (List<FrameData>) -> Unit,
    onApplyLighting: (String, Float, Float, Float) -> Unit,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onDiscard: () -> Unit
) {
    // Post-scan lighting tuning — the user adjusts the stitched panorama's
    // exposure, contrast and warmth before it is saved. 1f = no change.
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(1f) }
    var showTuning by remember { mutableStateOf(false) }
    var lastApplied by remember { mutableStateOf(Triple(1f, 1f, 1f)) }

    // Live tuning feedback: while the tuning panel is open the preview is
    // rendered with a transient ColorFilter scoped to THIS image only. On
    // Apply the adjustments are baked into the file and the filter is removed,
    // so the saved panorama is exactly what the user confirmed. (The old
    // blue-filter bug was an unscoped filter that leaked past Apply.)
    val liveTuningFilter: ColorFilter? = remember(showTuning, brightness, contrast, warmth) {
        if (showTuning && (brightness != 1f || contrast != 1f || warmth != 1f)) {
            val cm = android.graphics.ColorMatrix(
                floatArrayOf(
                    brightness, 0f, 0f, 0f, 0f,
                    0f, brightness, 0f, 0f, 0f,
                    0f, 0f, brightness, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val contrastMatrix = android.graphics.ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, 128f * (1f - contrast),
                    0f, contrast, 0f, 0f, 128f * (1f - contrast),
                    0f, 0f, contrast, 0f, 128f * (1f - contrast),
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.setConcat(contrastMatrix, cm)
            val warmMatrix = android.graphics.ColorMatrix(
                floatArrayOf(
                    warmth, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 2f - warmth, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.setConcat(warmMatrix, cm)
            android.graphics.ColorMatrixColorFilter(cm)
        } else null
    }

    // Auto-stitch once the frames are in — the user lands on a preview, not a blank page.
    LaunchedEffect(capturedFrames.size) {
        if (capturedFrames.isNotEmpty()) onStitch(capturedFrames)
    }

    Box(Modifier.fillMaxSize().background(DorjaColors.CanvasBg).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(DorjaColors.Jol100), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = DorjaColors.Jol600, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Panorama Complete", color = DorjaColors.Ink950, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("$frameCount photos captured for $roomName", color = DorjaColors.Gray600, textAlign = TextAlign.Center, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            // LIVE EQUIRRECTANGULAR STITCHING CANVAS PREVIEW
            if (stitchingPreviewBmp != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DorjaColors.Gray300,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Jol600),
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 12.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = stitchingPreviewBmp.asImageBitmap(),
                            contentDescription = "Live 360 Panorama Stitching Preview",
                            contentScale = ContentScale.Fit,
                            colorFilter = liveTuningFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                        Badge(
                            containerColor = DorjaColors.InverseBg.copy(alpha = 0.78f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Text("LIVE 360° CANVAS PREVIEW", color = DorjaColors.InverseFg, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            if (stitchingStatus != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DorjaColors.Jol100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Jol600.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (stitchedPath == null) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(stitchingStatus!!, color = DorjaColors.Ink950, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // ── Post-scan lighting tuning — live preview, no color filter bug ──
            // The preview bitmap shown above is recomputed from the tuned file
            // whenever Apply fires, so what you see is exactly what gets saved.
            if (showTuning) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DorjaColors.Sand100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Jol600.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text("TUNE LIGHTING — CHANGES APPLY LIVE", color = DorjaColors.Jol600, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        TuningSlider("Brightness", brightness, 0.5f, 1.6f) { brightness = it }
                        TuningSlider("Contrast", contrast, 0.6f, 1.5f) { contrast = it }
                        TuningSlider("Warmth", warmth, 0.7f, 1.3f) { warmth = it }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DorjaOutlinedButton(
                                "Reset",
                                onClick = {
                                    brightness = 1f; contrast = 1f; warmth = 1f
                                    lastApplied = Triple(1f, 1f, 1f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DorjaButton(
                                "Apply",
                                onClick = {
                                    lastApplied = Triple(brightness, contrast, warmth)
                                    showTuning = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                DorjaOutlinedButton(
                    "Tune lighting",
                    onClick = { showTuning = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            // Apply lighting when the sliders are confirmed with a change. The
            // apply path rewrites the panorama file itself (single ColorMatrix
            // bake) — the on-screen preview is re-decoded from that file, so no
            // runtime ColorFilter is ever drawn over the image. That was the
            // "blue filter" bug: a stale filter stayed composited on the
            // preview after tuning.
            LaunchedEffect(lastApplied, stitchedPath) {
                val (b, c, w) = lastApplied
                if (stitchedPath != null && (b != 1f || c != 1f || w != 1f)) {
                    onApplyLighting(stitchedPath, b, c, w)
                    lastApplied = Triple(1f, 1f, 1f) // consume; avoid re-apply loops
                }
            }

            DorjaButton(
                "Save 360° Panorama to $roomName",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DorjaOutlinedButton("Retake", onClick = onRetake, modifier = Modifier.weight(1f))
                DorjaOutlinedButton("Discard", onClick = onDiscard, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TuningSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DorjaColors.Gray600, fontSize = 11.sp, modifier = Modifier.width(84.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f).height(28.dp)
        )
        Text(
            "${((value - 1f) * 100).roundToInt().let { if (it >= 0) "+$it%" else "$it%" }}",
            color = DorjaColors.Ink950,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(52.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════
//  STITCHING — gyro-based cylindrical-to-equirectangular
//
//  The proven pipeline from the first releases, restored verbatim:
//  for a phone rotating around a fixed point the recorded gyro heading
//  tells us exactly where each frame sits on the panorama. Every panorama
//  column is sampled from the frame whose heading is closest, using the
//  pinhole model — no feature matching, no OpenCV, instant and reliable.
// ═════════════════════════════════════════════════════════════

private const val CAMERA_HFOV_DEG = 63.0 // typical phone horizontal FOV

private fun stitchFrames(ctx: android.content.Context, frames: List<FrameData>): String? {
    if (frames.isEmpty()) return null
    return try {
        stitchFramesInternal(ctx, frames)
    } catch (e: OutOfMemoryError) {
        Log.e("Stitcher", "OOM during stitching", e)
        System.gc()
        null
    } catch (e: Exception) {
        Log.e("Stitcher", "Stitching failed: ${e.message}", e)
        null
    }
}

private fun stitchFramesInternal(ctx: android.content.Context, frameDataList: List<FrameData>): String? {
    Log.i("Stitcher", "=== PANORAMA STITCHING PIPELINE ===")
    Log.i("Stitcher", "Input: ${frameDataList.size} frames")

    // ── Step 1: Load frames with consistent scaling ──────
    val targetH = 800
    data class LoadedFrame(val bmp: Bitmap, val heading: Float, val path: String)

    val loadedFrames = frameDataList.mapNotNull { fd ->
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(fd.path, opts)
            Log.i("Stitcher", "  Frame: ${opts.outWidth}×${opts.outHeight} heading=${"%.1f".format(fd.heading)}° — ${fd.path}")
            val sample = (opts.outHeight / targetH).coerceAtLeast(1)
            val bmp = BitmapFactory.decodeFile(fd.path, BitmapFactory.Options().apply { inSampleSize = sample })
            if (bmp != null && !bmp.isRecycled && bmp.width > 100 && bmp.height > 100) {
                LoadedFrame(bmp, fd.heading, fd.path)
            } else {
                Log.w("Stitcher", "  Frame SKIPPED (too small or null): ${bmp?.width}×${bmp?.height}")
                bmp?.recycle()
                null
            }
        } catch (e: Exception) {
            Log.e("Stitcher", "  Frame FAILED to load: ${e.message}")
            null
        }
    }

    if (loadedFrames.size < 2) {
        Log.e("Stitcher", "Not enough frames: ${loadedFrames.size}")
        loadedFrames.forEach { it.bmp.recycle() }
        return null
    }
    Log.i("Stitcher", "Loaded ${loadedFrames.size} frames, first: ${loadedFrames[0].bmp.width}×${loadedFrames[0].bmp.height}")

    // ── Step 2: Compute equirectangular geometry ─────────
    val panoW = 4096
    val panoH = 2048
    val hFOV = Math.toRadians(CAMERA_HFOV_DEG)

    // ── Step 3: Create panorama canvas ───────────────────
    val panorama = Bitmap.createBitmap(panoW, panoH, Bitmap.Config.ARGB_8888)
    val canvas = PanoCanvas(panorama)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    // ── Step 4: Column-by-column cylindrical warp ────────
    // For each panorama column, determine the longitude angle it represents,
    // find the best source frame, and draw the corresponding source column.
    for (panoX in 0 until panoW) {
        val lon = (panoX.toDouble() / panoW) * 2.0 * PI

        var bestFrame: LoadedFrame? = null
        var bestDist = Double.MAX_VALUE

        for (frame in loadedFrames) {
            val headingRad = Math.toRadians(frame.heading.toDouble())
            var dist = abs(lon - headingRad)
            if (dist > PI) dist = 2.0 * PI - dist
            if (dist < hFOV / 2.0 && dist < bestDist) {
                bestFrame = frame
                bestDist = dist
            }
        }

        if (bestFrame == null) continue

        val frame = bestFrame
        val headingRad = Math.toRadians(frame.heading.toDouble())

        var relLon = lon - headingRad
        while (relLon > PI) relLon -= 2.0 * PI
        while (relLon < -PI) relLon += 2.0 * PI

        // Pinhole model: pixel x corresponds to angle atan((x - cx) / f)
        val f = frame.bmp.width / (2.0 * tan(hFOV / 2.0))
        val cx = frame.bmp.width / 2.0
        val srcX = (f * tan(relLon) + cx).toInt()

        if (srcX < 0 || srcX >= frame.bmp.width) continue

        val srcRect = Rect(srcX, 0, srcX + 1, frame.bmp.height)
        val dstRect = RectF(panoX.toFloat(), 0f, (panoX + 1).toFloat(), panoH.toFloat())
        canvas.drawBitmap(frame.bmp, srcRect, dstRect, paint)
    }

    Log.i("Stitcher", "Panorama composited: ${panoW}×${panoH}")

    // ── Step 5: Crop black borders (partial 360° coverage) ──
    val cropped = cropBlackBorders(panorama)
    panorama.recycle()
    Log.i("Stitcher", "After crop: ${cropped.width}×${cropped.height}")

    // ── Step 6: Ensure 2:1 equirectangular aspect ratio ──
    val finalBmp = if (cropped.width != 2 * cropped.height || cropped.width != panoW) {
        Log.i("Stitcher", "Resizing to exact 2:1 equirectangular: ${panoW}×${panoH}")
        val scaled = Bitmap.createScaledBitmap(cropped, panoW, panoH, true)
        cropped.recycle()
        scaled
    } else {
        cropped
    }

    // ── Step 7: Save ────────────────────────────────────
    return try {
        val out = File(ctx.cacheDir, "panorama_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        finalBmp.recycle()
        loadedFrames.forEach { it.bmp.recycle() }
        Log.i("Stitcher", "Saved: ${out.absolutePath}")
        Log.i("Stitcher", "=== STITCHING COMPLETE ===")
        out.absolutePath
    } catch (e: Exception) {
        Log.e("Stitcher", "Save failed", e)
        finalBmp.recycle()
        loadedFrames.forEach { it.bmp.recycle() }
        null
    }
}

/** Crop black (near-zero) borders from a bitmap */
private fun cropBlackBorders(bitmap: Bitmap): Bitmap {
    val w = bitmap.width; val h = bitmap.height
    val pixels = IntArray(w * h); bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    fun isBlack(px: Int) = AndroidColor.red(px) < 15 && AndroidColor.green(px) < 15 && AndroidColor.blue(px) < 15
    var top = 0; var bottom = h - 1; var left = 0; var right = w - 1
    for (y in 0 until h) { var found = false; for (x in 0 until w step 10) { if (!isBlack(pixels[y * w + x])) { found = true; break } }; if (found) { top = y; break } }
    for (y in h - 1 downTo top) { var found = false; for (x in 0 until w step 10) { if (!isBlack(pixels[y * w + x])) { found = true; break } }; if (found) { bottom = y; break } }
    for (x in 0 until w) { var found = false; for (y in top until bottom step 10) { if (!isBlack(pixels[y * w + x])) { found = true; break } }; if (found) { left = x; break } }
    for (x in w - 1 downTo left) { var found = false; for (y in top until bottom step 10) { if (!isBlack(pixels[y * w + x])) { found = true; break } }; if (found) { right = x; break } }
    val cropW = (right - left + 1).coerceAtLeast(1); val cropH = (bottom - top + 1).coerceAtLeast(1)
    return Bitmap.createBitmap(bitmap, left, top, cropW, cropH)
}

// ═════════════════════════════════════════════════════════════
//  SHARED OVERLAYS & UI COMPOSABLES
// ═════════════════════════════════════════════════════════════

@Composable
private fun VerticalRingRail(
    scanMode: ScanGeometry.ScanMode,
    currentRing: Int,
    scanTargets: List<ScanGeometry.ScanTarget>,
    capturedFrames: List<FrameData>,
    modifier: Modifier = Modifier
) {
    val rings = remember(scanMode) {
        scanTargets.map { it.ringIndex }.distinct().sorted()
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("RING", color = Accent, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

            for (ring in rings) {
                val targetsInRing = scanTargets.filter { it.ringIndex == ring }
                val capturedInRing = capturedFrames.count { f -> targetsInRing.any { t -> t.targetIndex == f.col } }
                val ringComplete = capturedInRing >= targetsInRing.size
                val isCurrent = ring == currentRing

                val dotColor = when {
                    ringComplete -> Green
                    isCurrent -> TargetYellow
                    else -> Color.White.copy(alpha = 0.3f)
                }

                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(if (isCurrent) 2.dp else 0.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    zoomRatio: Float = 1f,
    onCameraBound: (Camera, Float) -> Unit = { _, _ -> }
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

    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    AndroidView(factory = { ctx ->
        PreviewView(ctx).also { pv ->
            // Fill the whole viewport — no letterbox bands above/below the feed.
            pv.scaleType = PreviewView.ScaleType.FILL_CENTER
            ProcessCameraProvider.getInstance(ctx).addListener({
                val cp = ProcessCameraProvider.getInstance(ctx).get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                onCaptureReady(capture)
                try {
                    cp.unbindAll()
                    // Bind the default (logical) back camera so the zoom picker can
                    // switch between main / wide / ultra-wide lenses via zoom ratio.
                    val camera = cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    boundCamera = camera
                } catch (e: Exception) {
                    Log.e("Scanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
        }
    }, modifier = Modifier.fillMaxSize())

    // Report the bound camera + its supported zoom range ONCE per bind so the
    // preview can offer only the lens options this device actually has.
    // zoomState is a LiveData; its value is populated shortly after binding.
    var reportedCameraBound by remember { mutableStateOf(false) }
    LaunchedEffect(boundCamera) {
        val cam = boundCamera ?: return@LaunchedEffect
        var zs = cam.cameraInfo.zoomState.value
        var tries = 0
        while (zs == null && tries < 50) {
            delay(100)
            zs = cam.cameraInfo.zoomState.value
            tries++
        }
        if (!reportedCameraBound && zs != null) {
            reportedCameraBound = true
            onCameraBound(cam, zs.minZoomRatio)
        }
    }

    // Apply the chosen zoom to preview AND capture use cases.
    LaunchedEffect(boundCamera, zoomRatio) {
        boundCamera?.cameraControl?.setZoomRatio(zoomRatio)
    }
}

@Composable
private fun ScopeOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = 2.dp.toPx()
        for (i in 0..40) {
            val x = (i / 40f) * w
            val curve = abs(i / 40f - 0.5f) * 2f * 40.dp.toPx()
            drawCircle(Accent.copy(alpha = 0.35f), r, Offset(x, 20.dp.toPx() + curve))
        }
        for (i in 0..40) {
            val x = (i / 40f) * w
            val curve = abs(i / 40f - 0.5f) * 2f * 40.dp.toPx()
            drawCircle(Accent.copy(alpha = 0.35f), r, Offset(x, h - 20.dp.toPx() - curve))
        }
    }
}

@Composable
private fun LastShotThumbnail(
    frames: SnapshotStateList<FrameData>,
    capturedCount: Int,
    modifier: Modifier = Modifier
) {
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(frames.size) {
        if (frames.isNotEmpty()) {
            popScale.snapTo(1.35f)
            popScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(modifier.size(52.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (frames.isEmpty()) Color.White.copy(alpha = 0.25f) else Green),
            modifier = Modifier.fillMaxSize().scale(popScale.value)
        ) {
            if (frames.isEmpty()) {
                Box(contentAlignment = Alignment.Center) {
                    Text("0", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                AsyncImage(
                    model = frames.last().path,
                    contentDescription = "Last captured frame",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (frames.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Green)
                    .border(2.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$capturedCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
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

private fun buildJson(stitchedPath: String?, frames: List<FrameData>, roomId: String): String {
    val json = JSONObject()
    if (stitchedPath != null) json.put("stitchedPanorama", stitchedPath)
    val arr = JSONArray(); frames.forEach { arr.put(it.path) }
    json.put("frames", arr)
    json.put("frameCount", frames.size)
    json.put("roomId", roomId)
    json.put("timestamp", System.currentTimeMillis())
    // Store headings for debug
    val headings = JSONArray(); frames.forEach { headings.put(it.heading.toDouble()) }
    json.put("headings", headings)
    return json.toString()
}

/** Deletes captured frame files + debug intermediates (old scanner behavior). */
private fun cleanupFrameFiles(ctx: android.content.Context, frames: List<FrameData> = emptyList()) {
    try {
        for (f in frames) {
            val file = File(f.path)
            if (file.exists()) file.delete()
        }
        ctx.cacheDir.listFiles { _, name -> name.startsWith("frame_") && name.endsWith(".jpg") }?.forEach { it.delete() }
        File(ctx.cacheDir, "stitch_debug").deleteRecursively()
    } catch (e: Exception) {
        Log.w("Scanner", "Frame cleanup failed: ${e.message}")
    }
}

/**
 * Bakes brightness/contrast/warmth multipliers (1f = neutral) into a copy of
 * the panorama and returns the new file path. Uses a single ColorMatrix pass
 * so the full-size image is re-processed in one draw.
 */
private fun applyLightingAdjustments(srcPath: String, brightness: Float, contrast: Float, warmth: Float): String? {
    try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(srcPath, opts)
        if (opts.outWidth <= 0) return null
        val sample = (opts.outWidth / 2048).coerceAtLeast(1)
        val bmp = BitmapFactory.decodeFile(srcPath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null

        val cm = android.graphics.ColorMatrix(
            floatArrayOf(
                brightness, 0f, 0f, 0f, 0f,
                0f, brightness, 0f, 0f, 0f,
                0f, 0f, brightness, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        // Contrast pivots around mid-gray (128).
        val contrastMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 128f * (1f - contrast),
                0f, contrast, 0f, 0f, 128f * (1f - contrast),
                0f, 0f, contrast, 0f, 128f * (1f - contrast),
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.setConcat(contrastMatrix, cm)
        // Warmth: >1 boosts red / cools blue; <1 cools red / boosts blue.
        val warmMatrix = android.graphics.ColorMatrix(
            floatArrayOf(
                warmth, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 2f - warmth, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.setConcat(warmMatrix, cm)

        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        PanoCanvas(out).drawBitmap(bmp, 0f, 0f, paint)
        bmp.recycle()

        val outFile = File(File(srcPath).parent, "panorama_tuned_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        out.recycle()
        File(srcPath).delete()
        Log.i("Scanner", "Lighting baked: brightness=$brightness contrast=$contrast warmth=$warmth → ${outFile.absolutePath}")
        return outFile.absolutePath
    } catch (e: Exception) {
        Log.e("Scanner", "Lighting adjustment failed", e)
        return null
    }
}

private fun vibrateShutter(ctx: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            (ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1))
        }
    } catch (_: Exception) {}
}
