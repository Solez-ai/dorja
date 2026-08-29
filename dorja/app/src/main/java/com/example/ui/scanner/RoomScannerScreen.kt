package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
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
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan
import kotlin.math.sqrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.roundToInt

private enum class Phase { SELECT, PREVIEW, CAPTURING, DONE }
private const val TOTAL_SHOTS = 12
private val Accent = Color(0xFF00BCD4)
private val Green = Color(0xFF4CAF50)

data class FrameData(val path: String, val heading: Float)
private const val CAMERA_HFOV_DEG = 63.0 // typical phone horizontal FOV

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
    var selectedRoom by remember { mutableStateOf<RoomItem?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasCamera = granted }

    var heading by remember { mutableFloatStateOf(0f) }
    var gyroOn by remember { mutableStateOf(true) }
    val capturedFrames = remember { mutableStateListOf<FrameData>() }
    var currentTarget by remember { mutableIntStateOf(0) }

    val sensorMgr = remember { ctx.getSystemService(SensorManager::class.java) }
    val rotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val rotMatrix = FloatArray(9)
    val orientAngles = FloatArray(3)

    DisposableEffect(sensorMgr, gyroOn) {
        if (sensorMgr == null || rotVec == null || !gyroOn) { onDispose { } }
        else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, e.values)
                        SensorManager.getOrientation(rotMatrix, orientAngles)
                        heading = ((Math.toDegrees(orientAngles[0].toDouble()) % 360.0) + 360.0).toFloat() % 360f
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            sensorMgr.registerListener(listener, rotVec, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorMgr.unregisterListener(listener) }
        }
    }

    LaunchedEffect(Unit) { if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA) }

    // Haptic buzz when tilt direction changes between shots
    var prevTiltDir by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentTarget) {
        val newTiltDir = when (currentTarget % 6) {
            1, 5 -> -1  // up
            3 -> 1       // down
            else -> 0    // level
        }
        if (newTiltDir != prevTiltDir && currentTarget > 0) {
            vibrateTiltChange(ctx)
        }
        prevTiltDir = newTiltDir
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (phase) {
            Phase.SELECT -> SelectRoom(rooms = rooms, onSelect = { room ->
                selectedRoom = room; permLauncher.launch(Manifest.permission.CAMERA); phase = Phase.PREVIEW
            }, onBack = onBack)

            Phase.PREVIEW -> PreviewPhase(imageCapture, { imageCapture = it }, hasCamera, selectedRoom?.displayName ?: "Room", gyroOn, { gyroOn = !gyroOn }, { phase = Phase.CAPTURING }, { phase = Phase.SELECT }, lifecycleOwner)

            Phase.CAPTURING -> CapturingPhase(imageCapture, { imageCapture = it }, hasCamera, heading, currentTarget, TOTAL_SHOTS, capturedFrames.size, gyroOn, { gyroOn = !gyroOn }, onCapture = {
                val ic = imageCapture ?: return@CapturingPhase
                val angle = currentTarget * (360 / TOTAL_SHOTS)
                val file = File(ctx.cacheDir, "frame_${angle}_${System.currentTimeMillis()}.jpg")
                val capturedHeading = heading // record actual gyro heading at capture time
                ic.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        capturedFrames.add(FrameData(file.absolutePath, capturedHeading))
                        // Log actual captured frame dimensions to diagnose zoom
                        val dimOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, dimOpts)
                        Log.i("Scanner", "Captured frame ${capturedFrames.size}: ${dimOpts.outWidth}×${dimOpts.outHeight} heading=${"%.1f".format(capturedHeading)}° path=${file.name}")
                        vibrateShutter(ctx)
                        currentTarget = (currentTarget + 1).coerceAtMost(TOTAL_SHOTS)
                    }
                    override fun onError(exc: ImageCaptureException) { Log.e("Scanner", "Capture failed", exc) }
                })
            }, onStop = { phase = Phase.DONE }, onBack = { phase = Phase.PREVIEW }, lifecycleOwner)

            Phase.DONE -> DonePhase(selectedRoom?.displayName ?: "Room", capturedFrames.size, onSave = {
                scope.launch {
                    try {
                        val frames = capturedFrames.toList()
                        Log.i("Scanner", "Starting stitch: ${frames.size} frames")
                        val stitched = withContext(Dispatchers.IO) { stitchFrames(ctx, frames) }
                        if (stitched != null) {
                            val json = buildJson(stitched, frames, selectedRoom?.id ?: "")
                            withContext(Dispatchers.IO) { repo.updateRoom3DScan(selectedRoom?.id ?: "", json) }
                            onScanComplete(selectedRoom?.id ?: "", json)
                        } else {
                            Log.e("Scanner", "Stitching returned null — panorama not saved")
                        }
                    } catch (e: Exception) {
                        Log.e("Scanner", "Save failed", e)
                    }
                }
            }, onRetake = { capturedFrames.clear(); currentTarget = 0; phase = Phase.PREVIEW },
                onDiscard = { capturedFrames.clear(); currentTarget = 0; phase = Phase.SELECT })
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 1 — SELECT ROOM
// ═════════════════════════════════════════════════════════════
@Composable
private fun SelectRoom(rooms: List<RoomItem>, onSelect: (RoomItem) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(DorjaColors.Ink950).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Select Room to Scan", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("3D Panorama Scanner", color = Accent, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(12.dp), color = Accent.copy(alpha = 0.12f), border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("How it works", color = DorjaColors.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Capture $TOTAL_SHOTS overlapping photos around the room.\nThey are blended into a 360° panorama.", color = DorjaColors.Sand300, fontSize = 12.sp, lineHeight = 16.sp)
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
                    Surface(Modifier.fillMaxWidth().clickable { onSelect(room) }, shape = RoundedCornerShape(12.dp),
                        color = if (room.has3DScan) Accent.copy(alpha = 0.1f) else DorjaColors.Gray700,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (room.has3DScan) Accent else DorjaColors.Sand300.copy(alpha = 0.3f))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(if (room.has3DScan) Accent.copy(alpha = 0.2f) else DorjaColors.Ink950), contentAlignment = Alignment.Center) {
                                Icon(if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom, null, tint = if (room.has3DScan) Accent else DorjaColors.Sand300, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(room.displayName, color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(room.roomType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }, color = DorjaColors.Sand300, fontSize = 11.sp)
                            }
                            if (room.has3DScan) { Badge(containerColor = Accent.copy(alpha = 0.2f)) { Text("SCANNED", color = Accent, fontSize = 9.sp, fontFamily = FontFamily.Monospace) } }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 2 — PREVIEW
// ═════════════════════════════════════════════════════════════
@Composable
private fun PreviewPhase(imageCapture: ImageCapture?, onCaptureReady: (ImageCapture) -> Unit, hasCamera: Boolean, roomName: String, gyroOn: Boolean, onToggleGyro: () -> Unit, onStart: () -> Unit, onBack: () -> Unit, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)
        ScopeOverlay()
        Box(Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))).align(Alignment.TopCenter))
        Row(Modifier.fillMaxWidth().padding(top = 40.dp, start = 12.dp, end = 12.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("SCANNING", color = Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold); Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
            Spacer(Modifier.size(38.dp))
        }
        Box(Modifier.align(Alignment.Center).padding(32.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.55f), border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.3f))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("📷", fontSize = 28.sp); Spacer(Modifier.height(8.dp)); Text("Hold your phone upright\nin portrait mode", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp) }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PRESS TO START", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).border(3.dp, Color.White, CircleShape).clickable { onStart() }, contentAlignment = Alignment.Center) { Box(Modifier.size(54.dp).clip(CircleShape).background(Green)) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) { GyroChip(gyroOn, onToggleGyro) }
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 3 — CAPTURING
// ═════════════════════════════════════════════════════════════
@Composable
private fun CapturingPhase(imageCapture: ImageCapture?, onCaptureReady: (ImageCapture) -> Unit, hasCamera: Boolean, heading: Float, targetIndex: Int, totalShots: Int, capturedCount: Int, gyroOn: Boolean, onToggleGyro: () -> Unit, onCapture: () -> Unit, onStop: () -> Unit, onBack: () -> Unit, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    val targetAngle = targetIndex * (360 / totalShots)
    Box(Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onCaptureReady, hasCamera, lifecycleOwner)
        CompassOverlay(heading, targetIndex, totalShots, capturedCount)
        Box(Modifier.fillMaxWidth().height(60.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))).align(Alignment.TopCenter))
        Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.65f), modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp)) {
            Text("TARGET: ${targetAngle}°  •  ${capturedCount}/$totalShots CAPTURED", color = Accent, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        Box(Modifier.fillMaxWidth().height(140.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))).align(Alignment.BottomCenter))
        // Elevation chart — shows ideal phone height for each shot
        ElevationChart(
            totalShots = totalShots,
            currentShot = targetIndex,
            capturedCount = capturedCount,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp, start = 24.dp, end = 24.dp).fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).border(3.dp, Color.White, CircleShape).clickable { onCapture() }, contentAlignment = Alignment.Center) { Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))) }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE53935)).border(2.dp, Color.White, CircleShape).clickable { onStop() }, contentAlignment = Alignment.Center) { Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color.White)) }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 130.dp)) { GyroChip(gyroOn, onToggleGyro) }
        Text("Point at ${targetAngle}° and tap shutter", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
        // Vertical tilt guidance with visual indicator
        val tiltDirection = when (targetIndex % 6) {
            0 -> 0f   // level
            1 -> -1f  // up
            2 -> 0f   // level
            3 -> 1f   // down
            4 -> 0f   // level
            else -> -1f // up
        }
        val tiltLabel = when (tiltDirection.toInt()) {
            -1 -> "TILT UP"
            1 -> "TILT DOWN"
            else -> "LEVEL"
        }
        TiltIndicator(tiltDirection, tiltLabel, Modifier.align(Alignment.BottomCenter).padding(bottom = 82.dp))
    }
}

// ═════════════════════════════════════════════════════════════
//  PHASE 4 — DONE
// ═════════════════════════════════════════════════════════════
@Composable
private fun DonePhase(roomName: String, frameCount: Int, onSave: () -> Unit, onRetake: () -> Unit, onDiscard: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DorjaColors.Ink950).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Accent.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, tint = Accent, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Scan Complete", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(6.dp))
            Text("$frameCount photos captured for $roomName", color = DorjaColors.Sand300, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatTile("FRAMES", "$frameCount", Modifier.weight(1f)); StatTile("STITCHED", "360°", Modifier.weight(1f)); StatTile("TYPE", "EQ.", Modifier.weight(1f)) }
            Spacer(Modifier.height(20.dp))
            DorjaButton("Save 3D Scan to $roomName", onClick = onSave, modifier = Modifier.fillMaxWidth().height(48.dp))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { DorjaOutlinedButton("Retake", onClick = onRetake, modifier = Modifier.weight(1f)); DorjaOutlinedButton("Discard", onClick = onDiscard, modifier = Modifier.weight(1f)) }
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  SHARED COMPOSABLES
// ═════════════════════════════════════════════════════════════

@Composable
private fun CameraPreview(imageCapture: ImageCapture?, onCaptureReady: (ImageCapture) -> Unit, hasCamera: Boolean, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
    if (!hasCamera) { Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium) } }; return }
    AndroidView(factory = { ctx ->
        PreviewView(ctx).also { pv ->
            ProcessCameraProvider.getInstance(ctx).addListener({
                val cp = ProcessCameraProvider.getInstance(ctx).get()
                // Use 4:3 aspect ratio to get widest sensor FOV.
                // Most phone sensors are natively 4:3; 16:9 is a crop.
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()
                onCaptureReady(capture)
                try {
                    cp.unbindAll()
                    val camera = cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    Log.i("Scanner", "Camera bound successfully. ImageCapture: 4:3 aspect ratio (widest sensor FOV)")
                } catch (e: Exception) { Log.e("Scanner", "Camera bind failed", e) }
            }, ContextCompat.getMainExecutor(ctx))
        }
    }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ScopeOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height; val r = 2.dp.toPx()
        for (i in 0..40) { val x = (i / 40f) * w; val curve = kotlin.math.abs(i / 40f - 0.5f) * 2f * 40.dp.toPx(); drawCircle(Accent.copy(alpha = 0.35f), r, Offset(x, 20.dp.toPx() + curve)) }
        for (i in 0..40) { val x = (i / 40f) * w; val curve = kotlin.math.abs(i / 40f - 0.5f) * 2f * 40.dp.toPx(); drawCircle(Accent.copy(alpha = 0.35f), r, Offset(x, h - 20.dp.toPx() - curve)) }
        for (i in 0..20) { val y = (i / 20f) * h; val curve = kotlin.math.abs(i / 20f - 0.5f) * 2f * 30.dp.toPx(); drawCircle(Accent.copy(alpha = 0.25f), r, Offset(12.dp.toPx() + curve, y)) }
        for (i in 0..20) { val y = (i / 20f) * h; val curve = kotlin.math.abs(i / 20f - 0.5f) * 2f * 30.dp.toPx(); drawCircle(Accent.copy(alpha = 0.25f), r, Offset(w - 12.dp.toPx() - curve, y)) }
    }
}

@Composable
private fun CompassOverlay(heading: Float, targetIndex: Int, totalShots: Int, capturedCount: Int) {
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2; val cy = size.height / 2; val radius = size.width * 0.35f
        drawCircle(Accent.copy(alpha = 0.15f), radius, Offset(cx, cy), style = Stroke(2.dp.toPx()))
        repeat(totalShots) { i ->
            val angle = i * (360 / totalShots); val rad = Math.toRadians((angle - 90).toDouble())
            val nx = cx + radius * cos(rad).toFloat(); val ny = cy + radius * sin(rad).toFloat()
            val isCaptured = i < capturedCount; val isCurrent = i == targetIndex
            val nodeColor = when { isCaptured -> Green; isCurrent -> Accent; else -> Color.White.copy(alpha = 0.25f) }
            drawCircle(nodeColor, if (isCurrent) 8.dp.toPx() else 5.dp.toPx(), Offset(nx, ny))
        }
        val headRad = Math.toRadians((heading - 90).toDouble()); val hx = cx + radius * cos(headRad).toFloat(); val hy = cy + radius * sin(headRad).toFloat()
        drawCircle(Accent, 4.dp.toPx(), Offset(hx, hy)); drawLine(Accent.copy(alpha = 0.3f), Offset(cx, cy), Offset(hx, hy), 1.dp.toPx())
    }
}

@Composable
private fun GyroChip(on: Boolean, toggle: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = if (on) Accent.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.4f), border = androidx.compose.foundation.BorderStroke(1.dp, if (on) Accent else Color.White.copy(alpha = 0.3f)), modifier = Modifier.clickable { toggle() }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) Accent else Color.Gray)); Spacer(Modifier.width(6.dp))
            Text("GYRO ${if (on) "ON" else "OFF"}", color = if (on) Accent else Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = DorjaColors.Gray700) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(2.dp)); Text(label, color = DorjaColors.Sand300, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }
    }
}

@Composable
private fun TiltIndicator(direction: Float, label: String, modifier: Modifier = Modifier) {
    // direction: -1 = UP, 0 = LEVEL, +1 = DOWN
    // Animate the phone rotation smoothly
    val infiniteTransition = rememberInfiniteTransition(label = "tilt")
    val animatedTilt by infiniteTransition.animateFloat(
        initialValue = direction * 8f - 2f,
        targetValue = direction * 8f + 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltAngle"
    )
    val phoneRotation = if (direction == 0f) 0f else animatedTilt

    val labelColor = when (direction.toInt()) {
        -1 -> Color(0xFFFF9800) // orange for up
        1 -> Color(0xFF2196F3)  // blue for down
        else -> Green           // green for level
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, labelColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Visual phone icon with tilt rotation
            Canvas(Modifier.size(24.dp)) {
                val w = size.width * 0.5f
                val h = size.height * 0.85f
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Draw tilted phone rectangle
                drawContext.canvas.save()
                drawContext.canvas.rotate(phoneRotation, cx, cy)
                drawRoundRect(
                    color = labelColor,
                    topLeft = Offset(cx - w / 2, cy - h / 2),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.2f)
                )
                // Screen area
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(cx - w * 0.35f, cy - h * 0.3f),
                    size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
                drawContext.canvas.restore()
            }

            // Directional arrows
            if (direction != 0f) {
                Canvas(Modifier.size(10.dp, 16.dp)) {
                    val arrowColor = labelColor
                    val arrowLen = size.width * 0.4f
                    val cx = size.width / 2f
                    if (direction < 0) {
                        // Up arrow
                        drawLine(arrowColor, Offset(cx, 2.dp.toPx()), Offset(cx, size.height - 2.dp.toPx()), 2.dp.toPx())
                        drawLine(arrowColor, Offset(cx, 2.dp.toPx()), Offset(cx - arrowLen, 6.dp.toPx()), 2.dp.toPx())
                        drawLine(arrowColor, Offset(cx, 2.dp.toPx()), Offset(cx + arrowLen, 6.dp.toPx()), 2.dp.toPx())
                    } else {
                        // Down arrow
                        drawLine(arrowColor, Offset(cx, 2.dp.toPx()), Offset(cx, size.height - 2.dp.toPx()), 2.dp.toPx())
                        drawLine(arrowColor, Offset(cx, size.height - 2.dp.toPx()), Offset(cx - arrowLen, size.height - 6.dp.toPx()), 2.dp.toPx())
                        drawLine(arrowColor, Offset(cx, size.height - 2.dp.toPx()), Offset(cx + arrowLen, size.height - 6.dp.toPx()), 2.dp.toPx())
                    }
                }
            }

            Text(
                label,
                color = labelColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Mini elevation chart showing ideal phone height across all shots.
 *
 * Each shot has an elevation level:
 *   0 = center (level)
 *  +1 = up
 *  -1 = down
 *
 * The chart draws dots at different vertical positions connected by a
 * polyline, with the current shot pulsing and captured shots solid.
 */
@Composable
private fun ElevationChart(
    totalShots: Int,
    currentShot: Int,
    capturedCount: Int,
    modifier: Modifier = Modifier
) {
    // Elevation pattern: alternates level/up/level/down to ensure vertical coverage
    val elevations = remember(totalShots) {
        List(totalShots) { i ->
            when (i % 6) {
                0 -> 0f   // level
                1 -> 1f   // up
                2 -> 0f   // level
                3 -> -1f  // down
                4 -> 0f   // level
                else -> 1f // up
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "elevationPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cy = h / 2f          // center baseline
        val maxLift = h * 0.35f  // max vertical displacement
        val dotR = 5.dp.toPx()
        val spacing = w / (totalShots - 1).coerceAtLeast(1)

        // Compute dot positions
        val points = elevations.mapIndexed { i, elev ->
            Offset(
                x = i * spacing,
                y = cy - elev * maxLift
            )
        }

        // Draw connecting polyline (thin, behind dots)
        for (i in 0 until points.size - 1) {
            val color = when {
                i < capturedCount - 1 -> Green.copy(alpha = 0.5f)
                i == currentShot - 1 && currentShot <= capturedCount -> Accent.copy(alpha = 0.5f)
                else -> Color.White.copy(alpha = 0.15f)
            }
            drawLine(color, points[i], points[i + 1], 1.5.dp.toPx())
        }

        // Draw center baseline (faint)
        drawLine(Color.White.copy(alpha = 0.1f), Offset(0f, cy), Offset(w, cy), 0.5.dp.toPx())

        // Draw elevation labels (tiny text not possible in Canvas, use dots only)
        // Draw dots
        points.forEachIndexed { i, pt ->
            val isCaptured = i < capturedCount
            val isCurrent = i == currentShot

            when {
                isCurrent -> {
                    // Pulsing current dot — larger and brighter
                    val r = dotR * pulse * 1.4f
                    drawCircle(Accent.copy(alpha = 0.25f), r * 2f, pt) // glow
                    drawCircle(Accent, r, pt)
                }
                isCaptured -> {
                    drawCircle(Green, dotR * 0.9f, pt)
                }
                else -> {
                    drawCircle(Color.White.copy(alpha = 0.3f), dotR * 0.7f, pt)
                }
            }
        }

        // Draw elevation direction labels beside first & last dots
        // (tiny up/down arrows at the edges)
        val arrowSize = 4.dp.toPx()
        // Up arrow at first elevated shot
        val upIdx = elevations.indexOfFirst { it > 0f }
        if (upIdx >= 0) {
            val p = points[upIdx]
            drawLine(Accent, Offset(p.x, p.y - dotR - 2.dp.toPx()), Offset(p.x, p.y - dotR - 2.dp.toPx() - arrowSize), 1.dp.toPx())
        }
        // Down arrow at first down shot
        val downIdx = elevations.indexOfFirst { it < 0f }
        if (downIdx >= 0) {
            val p = points[downIdx]
            drawLine(Color(0xFF2196F3), Offset(p.x, p.y + dotR + 2.dp.toPx()), Offset(p.x, p.y + dotR + 2.dp.toPx() + arrowSize), 1.dp.toPx())
        }
    }
}

// ═════════════════════════════════════════════════════════════
//  STITCHING — gyro-based cylindrical-to-equirectangular
//
//  Correct pipeline:
//  1. Load frames + record actual gyro heading at capture time
//  2. For each frame, use heading to place it on the equirectangular canvas
//  3. Column-by-column: for each panorama column, find the best source
//     frame and sample from it using proper cylindrical projection
//  4. Output: 4096×2048 (2:1 equirectangular) JPEG
//
//  Key insight: for a phone rotating around a fixed point, the gyro heading
//  tells us exactly where each frame sits on the panorama. No feature matching
//  needed — the gyroscope IS the alignment mechanism.
// ═════════════════════════════════════════════════════════════

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
    Log.i("Stitcher", "Target output: 4096×2048 (2:1 equirectangular)")

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

    // Save raw frames for debug
    val debugDir = File(ctx.cacheDir, "stitch_debug")
    debugDir.mkdirs()
    loadedFrames.forEachIndexed { i, f ->
        val out = File(debugDir, "raw_frame_$i.jpg")
        FileOutputStream(out).use { f.bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        Log.i("Stitcher", "  Saved raw frame $i: heading=${"%.1f".format(f.heading)}° ${f.bmp.width}×${f.bmp.height} → ${out.absolutePath}")
    }

    // ── Step 2: Compute equirectangular geometry ─────────
    // Output is ALWAYS 4096×2048 regardless of input frame sizes
    val panoW = 4096
    val panoH = 2048
    val hFOV = Math.toRadians(CAMERA_HFOV_DEG) // horizontal FOV in radians

    Log.i("Stitcher", "Equirectangular canvas: ${panoW}×${panoH}")
    Log.i("Stitcher", "Camera hFOV: ${CAMERA_HFOV_DEG}°")
    Log.i("Stitcher", "Headings: ${loadedFrames.joinToString { "${"%.1f".format(it.heading)}°" }}")

    // ── Step 3: Create panorama canvas ───────────────────
    val panorama = Bitmap.createBitmap(panoW, panoH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(panorama)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    // ── Step 4: Column-by-column cylindrical warp ────────
    // For each panorama column, determine the longitude angle it represents,
    // find the best source frame, and draw the corresponding source column.

    for (panoX in 0 until panoW) {
        // This column's longitude angle (0 to 2π)
        val lon = (panoX.toDouble() / panoW) * 2.0 * PI

        // Find the frame whose heading is closest to this longitude
        var bestFrame: LoadedFrame? = null
        var bestDist = Double.MAX_VALUE

        for (frame in loadedFrames) {
            val headingRad = Math.toRadians(frame.heading.toDouble())
            // Angular distance, accounting for wrap-around at 0°/360°
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

        // Relative longitude from frame center
        var relLon = lon - headingRad
        // Normalize to [-π, π]
        while (relLon > PI) relLon -= 2.0 * PI
        while (relLon < -PI) relLon += 2.0 * PI

        // Focal length in pixels (derived from hFOV and frame width)
        val f = frame.bmp.width / (2.0 * Math.tan(hFOV / 2.0))
        val cx = frame.bmp.width / 2.0

        // Source column via cylindrical projection:
        // In a pinhole camera, pixel x corresponds to angle atan((x - cx) / f)
        // Inverse: x = f * tan(angle) + cx
        val srcX = (f * Math.tan(relLon) + cx).toInt()

        if (srcX < 0 || srcX >= frame.bmp.width) continue

        // Draw 1-pixel-wide column from source to panorama
        val srcRect = Rect(srcX, 0, srcX + 1, frame.bmp.height)
        val dstRect = RectF(panoX.toFloat(), 0f, (panoX + 1).toFloat(), panoH.toFloat())
        canvas.drawBitmap(frame.bmp, srcRect, dstRect, paint)
    }

    // Log diagnostic info
    Log.i("Stitcher", "Panorama composited: ${panoW}×${panoH}")
    Log.i("Stitcher", "Aspect ratio: ${"%.2f".format(panoW.toFloat() / panoH)} (target: 2.00)")

    // Save debug intermediate
    val debugInter = File(debugDir, "stitched_intermediate.jpg")
    FileOutputStream(debugInter).use { panorama.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    Log.i("Stitcher", "Debug intermediate: ${debugInter.absolutePath}")

    // ── Step 5: Crop black borders (if user didn't capture full 360°) ──
    val cropped = cropBlackBorders(panorama)
    panorama.recycle()
    Log.i("Stitcher", "After crop: ${cropped.width}×${cropped.height}")

    // ── Step 6: Ensure 2:1 equirectangular aspect ratio ──
    // If cropped panorama isn't 2:1, resize to exactly 4096×2048
    val finalBmp = if (cropped.width != 2 * cropped.height || cropped.width != panoW) {
        Log.i("Stitcher", "Resizing to exact 2:1 equirectangular: ${panoW}×${panoH}")
        val scaled = Bitmap.createScaledBitmap(cropped, panoW, panoH, true)
        cropped.recycle()
        scaled
    } else {
        cropped
    }

    Log.i("Stitcher", "Final panorama: ${finalBmp.width}×${finalBmp.height} (ratio: ${"%.2f".format(finalBmp.width.toFloat() / finalBmp.height)})")

    // ── Step 7: Save ────────────────────────────────────
    try {
        val out = File(ctx.cacheDir, "panorama_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        // Save debug final
        val debugFinal = File(debugDir, "panorama_final.jpg")
        FileOutputStream(debugFinal).use { finalBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }

        finalBmp.recycle()
        loadedFrames.forEach { it.bmp.recycle() }

        Log.i("Stitcher", "Saved: ${out.absolutePath}")
        Log.i("Stitcher", "Debug final: ${debugFinal.absolutePath}")
        Log.i("Stitcher", "=== STITCHING COMPLETE ===")
        return out.absolutePath
    } catch (e: Exception) {
        Log.e("Stitcher", "Save failed", e)
        finalBmp.recycle()
        loadedFrames.forEach { it.bmp.recycle() }
        return null
    }
}

// ═════════════════════════════════════════════════════════════
//  UTILITY FUNCTIONS
// ═════════════════════════════════════════════════════════════

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

private fun vibrateShutter(ctx: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1)) }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { @Suppress("DEPRECATION") (ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1)) }
    } catch (_: Exception) {}
}

/** Short double-tap buzz when tilt direction changes between shots */
private fun vibrateTiltChange(ctx: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 40), intArrayOf(0, 100, 0, 100), -1)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            (ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 40), intArrayOf(0, 100, 0, 100), -1)
            )
        }
    } catch (_: Exception) {}
}
