package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private enum class Phase { SELECT, PREVIEW, CAPTURING, LEGACY_CAPTURE, DONE }

private val Accent = Color(0xFF00BCD4)
private val Green = Color(0xFF4CAF50)
private val TargetYellow = Color(0xFFFFC107)

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
    var scanMode by remember { mutableStateOf(ScanGeometry.ScanMode.FULL_SPHERE) }
    var selectedRoom by remember { mutableStateOf<RoomItem?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
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

    val sensorMgr = remember { ctx.getSystemService(SensorManager::class.java) }
    val rotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val gameRotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR) }
    val accel = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    val mag = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) }

    val rotMatrix = remember { FloatArray(9) }
    val remappedMatrix = remember { FloatArray(9) }
    val orientAngles = remember { FloatArray(3) }
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
                            heading = ((Math.toDegrees(orientAngles[0].toDouble()) % 360.0) + 360.0).toFloat() % 360f
                            pitch = Math.toDegrees(orientAngles[1].toDouble()).toFloat()
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
            SphericalStitcher.cleanupFrameCache(ctx, capturedFrames)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (phase) {
            Phase.SELECT -> SelectRoom(
                rooms = rooms,
                scanMode = scanMode,
                onModeToggle = { mode -> scanMode = mode },
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
                gyroOn = gyroOn,
                onToggleGyro = { gyroOn = !gyroOn },
                onStart = {
                    // Full Sphere runs the real-AR flow; if ARCore is missing the
                    // user can still fall back to motion guidance from the AR screen.
                    phase = Phase.CAPTURING
                },
                onBack = { phase = Phase.SELECT },
                lifecycleOwner = lifecycleOwner
            )

            Phase.LEGACY_CAPTURE -> {
                // Chosen explicitly from the AR screen when the device can't run ARCore.
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

            Phase.CAPTURING -> {
                if (scanMode == ScanGeometry.ScanMode.FULL_SPHERE) {
                    // Real-AR guided spherical capture (ARCore). Clean guidance:
                    // what the room is understood, where to turn, auto-shutter.
                    ArSphereCapturePhase(
                        roomName = selectedRoom?.displayName ?: "Room",
                        scanMode = scanMode,
                        scanTargets = scanTargets,
                        currentTargetIdx = currentTargetIdx,
                        capturedFrames = capturedFrames,
                        gyroHeading = heading,
                        gyroPitch = pitch,
                        onFrameCaptured = { frame ->
                            val existingIdx = capturedFrames.indexOfFirst { it.col == frame.col }
                            if (existingIdx >= 0) {
                                capturedFrames[existingIdx] = frame
                            } else {
                                capturedFrames.add(frame)
                            }
                            vibrateShutter(ctx)
                            val next = scanTargets.indexOfFirst { t -> capturedFrames.none { it.col == t.targetIndex } }
                            if (next >= 0) {
                                currentTargetIdx = next
                            }
                        },
                        onRequestLegacyFallback = { phase = Phase.LEGACY_CAPTURE },
                        onStop = { phase = Phase.DONE },
                        onBack = { phase = Phase.PREVIEW }
                    )
                } else if (scanMode == ScanGeometry.ScanMode.AR_CORNER_SCAN) {
                    ArCornerScannerPhase(
                        imageCapture = imageCapture,
                        onCaptureReady = { imageCapture = it },
                        hasCamera = hasCamera,
                        heading = heading,
                        currentPitch = pitch,
                        roomName = selectedRoom?.displayName ?: "Room",
                        onSaveArModel = { jsonStr ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    repo.updateRoom3DScan(selectedRoom?.id ?: "", jsonStr)
                                }
                                onScanComplete(selectedRoom?.id ?: "", jsonStr)
                            }
                        },
                        onBack = { phase = Phase.PREVIEW },
                        lifecycleOwner = lifecycleOwner
                    )
                } else {
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
            }

            Phase.DONE -> DonePhase(
                roomName = selectedRoom?.displayName ?: "Room",
                frameCount = capturedFrames.size,
                scanMode = scanMode,
                capturedFrames = capturedFrames,
                stitchingStatus = stitchingStatus,
                stitchingPreviewBmp = stitchingPreviewBmp,
                onSave = {
                    scope.launch {
                        try {
                            val frames = capturedFrames.toList()
                            val stitched = withContext(Dispatchers.IO) {
                                SphericalStitcher.stitch(ctx, frames, scanMode) { statusMsg, liveBmp ->
                                    stitchingStatus = statusMsg
                                    stitchingPreviewBmp = liveBmp
                                }
                            }
                            if (stitched != null) {
                                val json = buildJson(stitched, frames, selectedRoom?.id ?: "", scanMode)
                                withContext(Dispatchers.IO) {
                                    repo.updateRoom3DScan(selectedRoom?.id ?: "", json)
                                }
                                onScanComplete(selectedRoom?.id ?: "", json)
                            } else {
                                Log.e("Scanner", "360 Stitching returned null")
                                stitchingStatus = "Stitching failed. Retake frames."
                            }
                        } catch (e: Exception) {
                            Log.e("Scanner", "Save failed", e)
                            stitchingStatus = "Error: ${e.message}"
                        }
                    }
                },
                onRetake = {
                    SphericalStitcher.cleanupFrameCache(ctx, capturedFrames)
                    capturedFrames.clear()
                    currentTargetIdx = 0
                    stitchingStatus = null
                    stitchingPreviewBmp = null
                    phase = Phase.PREVIEW
                },
                onDiscard = {
                    SphericalStitcher.cleanupFrameCache(ctx, capturedFrames)
                    capturedFrames.clear()
                    currentTargetIdx = 0
                    stitchingStatus = null
                    stitchingPreviewBmp = null
                    phase = Phase.SELECT
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 1 — SELECT ROOM & SCAN MODE
// ═════════════════════════════════════════════════════════════
@Composable
private fun SelectRoom(
    rooms: List<RoomItem>,
    scanMode: ScanGeometry.ScanMode,
    onModeToggle: (ScanGeometry.ScanMode) -> Unit,
    onSelect: (RoomItem) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(DorjaColors.Ink950).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Select Room to Scan", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("DORJA 360° × 180° Spherical Scanner", color = Accent, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DorjaColors.Gray700.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("SCAN MODE", color = DorjaColors.Sand300, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeOptionChip(
                            title = "AR Full Sphere 360°×180°",
                            subtitle = "AR-guided: floor/ceiling/walls detected • 62 shots",
                            timeHint = "~2 min",
                            icon = Icons.Default.RotateRight,
                            isSelected = scanMode == ScanGeometry.ScanMode.FULL_SPHERE,
                            onClick = { onModeToggle(ScanGeometry.ScanMode.FULL_SPHERE) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeOptionChip(
                            title = "Quick Panorama",
                            subtitle = "12 shots • 360° horizon",
                            timeHint = "~25-30 s",
                            icon = Icons.Default.Speed,
                            isSelected = scanMode == ScanGeometry.ScanMode.QUICK_SCAN,
                            onClick = { onModeToggle(ScanGeometry.ScanMode.QUICK_SCAN) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModeOptionChip(
                        title = "AR 3D Room Corner Scanner",
                        subtitle = "Tilt-and-turn: point at each dot & tap the shutter",
                        timeHint = "AR-guided",
                        icon = Icons.Default.CheckCircle,
                        isSelected = scanMode == ScanGeometry.ScanMode.AR_CORNER_SCAN,
                        onClick = { onModeToggle(ScanGeometry.ScanMode.AR_CORNER_SCAN) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

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
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (room.has3DScan) Accent else DorjaColors.Sand300.copy(alpha = 0.3f))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(if (room.has3DScan) Accent.copy(alpha = 0.2f) else DorjaColors.Ink950),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom, null, tint = if (room.has3DScan) Accent else DorjaColors.Sand300, modifier = Modifier.size(20.dp))
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

@Composable
private fun ModeOptionChip(
    title: String,
    subtitle: String,
    timeHint: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Accent.copy(alpha = 0.2f) else DorjaColors.Ink950,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) Accent else DorjaColors.Sand300.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (isSelected) Accent else DorjaColors.Sand300, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = DorjaColors.Sand300, fontSize = 10.sp)
            Text(timeHint, color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
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
    gyroOn: Boolean,
    onToggleGyro: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)
        ScopeOverlay()
        Box(Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))).align(Alignment.TopCenter))
        Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DORJA 360° SPHERICAL", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(38.dp))
        }

        // Clear 3-Step Visual Guidance Card
        Box(Modifier.align(Alignment.Center).padding(24.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.75f), border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.4f))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HOW TO CAPTURE A SPHERE", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("1. Stand in middle of room (pivot like a tripod)", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("2. Move phone to align center into green AR target ring", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("3. Follow 3D arrows from ceiling down to floor", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

    val coveragePercent = remember(capturedFrames.size) {
        ScanGeometry.computeCoveragePercent(capturedFrames.map { Pair(it.heading, it.pitchDeg) })
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
                    "SHOT ${currentTargetIdx + 1}/${scanTargets.size} • RING ${target.ringIndex} • ${"%.0f".format(coveragePercent)}% SPHERE",
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
    scanMode: ScanGeometry.ScanMode,
    capturedFrames: List<FrameData>,
    stitchingStatus: String?,
    stitchingPreviewBmp: Bitmap?,
    onSave: () -> Unit,
    onRetake: () -> Unit,
    onDiscard: () -> Unit
) {
    val coverage = remember(capturedFrames) {
        ScanGeometry.computeCoveragePercent(capturedFrames.map { Pair(it.heading, it.pitchDeg) })
    }

    Box(Modifier.fillMaxSize().background(DorjaColors.Ink950).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = Accent, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("Spherical Scan Complete", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("$frameCount photos captured for $roomName (${"%.0f".format(coverage)}% sphere coverage)", color = DorjaColors.Sand300, textAlign = TextAlign.Center, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            // LIVE EQUIRRECTANGULAR STITCHING CANVAS PREVIEW
            if (stitchingPreviewBmp != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Accent),
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 12.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = stitchingPreviewBmp.asImageBitmap(),
                            contentDescription = "Live 360 Panorama Stitching Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        Badge(
                            containerColor = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Text("LIVE 360° CANVAS PREVIEW", color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            if (stitchingStatus != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Accent.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(stitchingStatus, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            DorjaButton(
                "Save 360° Sphere to $roomName",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(46.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DorjaOutlinedButton("Retake", onClick = onRetake, modifier = Modifier.weight(1f))
                DorjaOutlinedButton("Discard", onClick = onDiscard, modifier = Modifier.weight(1f))
            }
        }
    }
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

    AndroidView(factory = { ctx ->
        PreviewView(ctx).also { pv ->
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
                    cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                } catch (e: Exception) {
                    Log.e("Scanner", "Camera bind failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))
        }
    }, modifier = Modifier.fillMaxSize())
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

private fun buildJson(
    stitchedPath: String?,
    frames: List<FrameData>,
    roomId: String,
    mode: ScanGeometry.ScanMode
): String {
    val json = JSONObject()
    json.put("version", 2)
    json.put("projection", "equirectangular")
    json.put("coverage", "360x180")
    json.put("mode", if (mode == ScanGeometry.ScanMode.FULL_SPHERE) "full" else "quick")
    json.put("cameraHfovDeg", ScanGeometry.DEFAULT_HFOV_DEG)
    json.put("cameraVfovDeg", ScanGeometry.DEFAULT_VFOV_DEG)

    val coveragePercent = ScanGeometry.computeCoveragePercent(
        capturedFrames = frames.map { Pair(it.heading, it.pitchDeg) }
    )
    json.put("coveragePercent", coveragePercent.toDouble())

    if (stitchedPath != null) json.put("stitchedPanorama", stitchedPath)

    val arr = JSONArray()
    frames.forEach { fd ->
        val fObj = JSONObject()
        fObj.put("path", fd.path)
        fObj.put("heading", fd.heading.toDouble())
        fObj.put("pitchDeg", fd.pitchDeg.toDouble())
        fObj.put("row", fd.row)
        fObj.put("col", fd.col)
        if (fd.isCap) {
            fObj.put("isCap", true)
            fObj.put("capType", fd.capType)
        }
        arr.put(fObj)
    }
    json.put("frames", arr)
    json.put("frameCount", frames.size)
    json.put("roomId", roomId)
    json.put("timestamp", System.currentTimeMillis())

    return json.toString()
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

private data class CornerAnchor3D(
    val id: Int,
    val headingDeg: Float,
    val pitchDeg: Float,
    val xMeters: Float,
    val yMeters: Float,
    val zMeters: Float
)

// ═════════════════════════════════════════════════════════════
//  AR 3D ROOM CORNER SCANNER PHASE
// ═════════════════════════════════════════════════════════════
@Composable
private fun ArCornerScannerPhase(
    imageCapture: ImageCapture?,
    onCaptureReady: (ImageCapture) -> Unit,
    hasCamera: Boolean,
    heading: Float,
    currentPitch: Float,
    roomName: String,
    onSaveArModel: (jsonStr: String) -> Unit,
    onBack: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val ctx = LocalContext.current
    val corners = remember { mutableStateListOf<CornerAnchor3D>() }

    // Estimate 3D vector coordinates based on device posture (-1.5m eye height above floor)
    val eyeHeight = 1.5f
    val pitchRad = Math.toRadians(currentPitch.toDouble()).toFloat()
    val headingRad = Math.toRadians(heading.toDouble()).toFloat()

    val tanPitch = tan(pitchRad.coerceAtMost(-0.1f))
    val groundDist = if (tanPitch < -0.05f) abs(eyeHeight / tanPitch) else 2.5f

    val currentX = groundDist * sin(headingRad)
    val currentZ = groundDist * cos(headingRad)

    val totalPerimeter = remember(corners.size) {
        if (corners.size < 2) 0f
        else {
            var sum = 0f
            for (i in corners.indices) {
                val c1 = corners[i]
                val c2 = corners[(i + 1) % corners.size]
                val dx = c2.xMeters - c1.xMeters
                val dz = c2.zMeters - c1.zMeters
                sum += Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()
            }
            sum
        }
    }

    val estimatedAreaSqM = remember(corners.size) {
        if (corners.size < 3) 0f
        else {
            var area = 0f
            for (i in corners.indices) {
                val c1 = corners[i]
                val c2 = corners[(i + 1) % corners.size]
                area += (c1.xMeters * c2.zMeters) - (c2.xMeters * c1.zMeters)
            }
            abs(area) / 2f
        }
    }

    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)

        // Interactive 3D AR Vector Overlay
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val pxPerDeg = size.width / 60f

            // Screen Center Crosshair
            drawCircle(Color.White.copy(alpha = 0.3f), 36.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
            drawCircle(TargetYellow, 4.dp.toPx(), Offset(cx, cy))

            // Project 3D corner anchors onto 2D camera view
            val projectedPoints = corners.map { c ->
                val relH = ((c.headingDeg - heading + 540) % 360) - 180
                val relP = c.pitchDeg - currentPitch
                val px = cx + (relH * pxPerDeg)
                val py = cy - (relP * pxPerDeg)
                Offset(px, py)
            }

            // Draw connecting AR floor lines
            for (i in projectedPoints.indices) {
                val p1 = projectedPoints[i]
                val p2 = projectedPoints[(i + 1) % projectedPoints.size]
                if (i < projectedPoints.size - 1 || projectedPoints.size >= 3) {
                    drawLine(Accent, p1, p2, 3.dp.toPx())
                    val midX = (p1.x + p2.x) / 2f
                    val midY = (p1.y + p2.y) / 2f
                    drawCircle(Accent, 4.dp.toPx(), Offset(midX, midY))
                }
            }

            // Draw 3D Corner Markers
            for (i in projectedPoints.indices) {
                val p = projectedPoints[i]
                drawCircle(Green.copy(alpha = 0.35f), 22.dp.toPx(), p)
                drawCircle(Green, 14.dp.toPx(), p, style = Stroke(2.5.dp.toPx()))
                drawCircle(Color.White, 5.dp.toPx(), p)
            }
        }

        // Header
        Box(Modifier.fillMaxWidth().height(70.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))).align(Alignment.TopCenter))
        Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AR 3D ROOM CORNER SCANNER", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(38.dp))
        }

        // Live Dimension HUD Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.5f)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("CORNERS", color = DorjaColors.Sand300, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("${corners.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("PERIMETER", color = DorjaColors.Sand300, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("${"%.2f".format(totalPerimeter)} m", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column {
                    Text("EST. AREA", color = DorjaColors.Sand300, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    Text("${"%.1f".format(estimatedAreaSqM)} m²", color = Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Bottom Action Controls
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TargetYellow),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    if (corners.isEmpty()) "AIM AT FLOOR CORNER & TAP ADD CORNER" else "AIM AT NEXT CORNER ALONG THE WALL",
                    color = TargetYellow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DorjaOutlinedButton("Reset", onClick = { corners.clear() }, modifier = Modifier.weight(1f))

                DorjaButton(
                    "+ Add 3D Corner Anchor",
                    onClick = {
                        vibrateShutter(ctx)
                        val anchor = CornerAnchor3D(
                            id = corners.size + 1,
                            headingDeg = heading,
                            pitchDeg = currentPitch,
                            xMeters = currentX,
                            yMeters = -eyeHeight,
                            zMeters = currentZ
                        )
                        corners.add(anchor)
                    },
                    modifier = Modifier.weight(2f)
                )
            }

            if (corners.size >= 3) {
                Spacer(Modifier.height(8.dp))
                DorjaButton(
                    "Save 3D Room Model (${corners.size} Corners)",
                    onClick = {
                        val root = JSONObject()
                        root.put("mode", "AR_CORNER_MAP")
                        root.put("cornerCount", corners.size)
                        root.put("perimeterMeters", totalPerimeter.toDouble())
                        root.put("areaSqMeters", estimatedAreaSqM.toDouble())
                        root.put("timestamp", System.currentTimeMillis())

                        val arr = JSONArray()
                        corners.forEach { c ->
                            val obj = JSONObject()
                            obj.put("id", c.id)
                            obj.put("heading", c.headingDeg.toDouble())
                            obj.put("pitch", c.pitchDeg.toDouble())
                            obj.put("x", c.xMeters.toDouble())
                            obj.put("y", c.yMeters.toDouble())
                            obj.put("z", c.zMeters.toDouble())
                            arr.put(obj)
                        }
                        root.put("corners", arr)
                        onSaveArModel(root.toString())
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                )
            }
        }
    }
}
