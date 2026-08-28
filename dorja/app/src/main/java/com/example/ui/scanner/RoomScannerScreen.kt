package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
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
import kotlin.math.cos
import kotlin.math.sin

private enum class Phase { SELECT, PREVIEW, CAPTURING, DONE }
private const val TOTAL_SHOTS = 12
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
    val capturedPaths = remember { mutableStateListOf<String>() }
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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (phase) {
            Phase.SELECT -> SelectRoom(rooms = rooms, onSelect = { room ->
                selectedRoom = room; permLauncher.launch(Manifest.permission.CAMERA); phase = Phase.PREVIEW
            }, onBack = onBack)

            Phase.PREVIEW -> PreviewPhase(imageCapture, { imageCapture = it }, hasCamera, selectedRoom?.displayName ?: "Room", gyroOn, { gyroOn = !gyroOn }, { phase = Phase.CAPTURING }, { phase = Phase.SELECT }, lifecycleOwner)

            Phase.CAPTURING -> CapturingPhase(imageCapture, { imageCapture = it }, hasCamera, heading, currentTarget, TOTAL_SHOTS, capturedPaths.size, gyroOn, { gyroOn = !gyroOn }, onCapture = {
                val ic = imageCapture ?: return@CapturingPhase
                val angle = currentTarget * (360 / TOTAL_SHOTS)
                val file = File(ctx.cacheDir, "frame_${angle}_${System.currentTimeMillis()}.jpg")
                ic.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        capturedPaths.add(file.absolutePath)
                        vibrateShutter(ctx)
                        currentTarget = (currentTarget + 1).coerceAtMost(TOTAL_SHOTS)
                    }
                    override fun onError(exc: ImageCaptureException) { Log.e("Scanner", "Capture failed", exc) }
                })
            }, onStop = { phase = Phase.DONE }, onBack = { phase = Phase.PREVIEW }, lifecycleOwner)

            Phase.DONE -> DonePhase(selectedRoom?.displayName ?: "Room", capturedPaths.size, onSave = {
                scope.launch {
                    val paths = capturedPaths.toList()
                    val stitched = withContext(Dispatchers.IO) { stitchFrames(ctx, paths) }
                    val json = buildJson(stitched, paths, selectedRoom?.id ?: "")
                    withContext(Dispatchers.IO) { repo.updateRoom3DScan(selectedRoom?.id ?: "", json) }
                    onScanComplete(selectedRoom?.id ?: "", json)
                }
            }, onRetake = { capturedPaths.clear(); currentTarget = 0; phase = Phase.PREVIEW },
                onDiscard = { capturedPaths.clear(); currentTarget = 0; phase = Phase.SELECT })
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
        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 120.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(totalShots) { i -> Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(when { i < capturedCount -> Green; i == targetIndex -> Accent; else -> Color.Gray.copy(alpha = 0.4f) })) }
        }
        Row(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).border(3.dp, Color.White, CircleShape).clickable { onCapture() }, contentAlignment = Alignment.Center) { Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))) }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFE53935)).border(2.dp, Color.White, CircleShape).clickable { onStop() }, contentAlignment = Alignment.Center) { Box(Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(Color.White)) }
        }
        Box(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 130.dp)) { GyroChip(gyroOn, onToggleGyro) }
        Text("Point at ${targetAngle}° and tap shutter", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp))
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
    AndroidView(factory = { ctx -> PreviewView(ctx).also { pv -> ProcessCameraProvider.getInstance(ctx).addListener({ val p = pv; val cp = ProcessCameraProvider.getInstance(ctx).get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(p.surfaceProvider) }; val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build(); onCaptureReady(capture); try { cp.unbindAll(); cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture) } catch (e: Exception) { Log.e("Scanner", "Camera bind failed", e) } }, ContextCompat.getMainExecutor(ctx)) } }, modifier = Modifier.fillMaxSize())
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

// ═════════════════════════════════════════════════════════════
//  STITCHING — pure Android, no OpenCV
// ═════════════════════════════════════════════════════════════private fun stitchFrames(ctx: android.content.Context, paths: List<String>): String? {
    if (paths.isEmpty()) return null
    return try {
        stitchFramesInternal(ctx, paths)
    } catch (e: OutOfMemoryError) {
        Log.e("Stitcher", "OOM during stitching", e)
        System.gc()
        null
    } catch (e: Exception) {
        Log.e("Stitcher", "Stitching failed: ${e.message}", e)
        null
    }
}

private fun stitchFramesInternal(ctx: android.content.Context, paths: List<String>): String? {
    Log.i("Stitcher", "=== PANORAMA STITCHING PIPELINE ===")
    Log.i("Stitcher", "Input: ${paths.size} frames")

    // ── Step 1: Load frames ──────────────────────────────
    val targetH = 600 // reduced for speed/memory
    val rawFrames = paths.mapNotNull { p ->
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(p, opts)
            Log.i("Stitcher", "  Raw frame: ${opts.outWidth}x${opts.outHeight} — $p")
            val sample = (opts.outHeight / targetH).coerceAtLeast(1)
            BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) { Log.e("Stitcher", "Load failed: $p"); null }
    }.filter { it != null && !it.isRecycled && it.width > 50 && it.height > 50 }.map { it!! }

    if (rawFrames.size < 2) { Log.e("Stitcher", "Not enough: ${rawFrames.size}"); rawFrames.forEach { it.recycle() }; return null }
    Log.i("Stitcher", "Loaded ${rawFrames.size} frames, first: ${rawFrames[0].width}x${rawFrames[0].height}")

    // Save raw frames for debug
    val debugDir = File(ctx.cacheDir, "stitch_debug"); debugDir.mkdirs()
    rawFrames.forEachIndexed { i, f ->
        val out = File(debugDir, "raw_frame_$i.jpg")
        FileOutputStream(out).use { f.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        Log.i("Stitcher", "  Saved raw frame $i: ${f.width}x${f.height} → ${out.absolutePath}")
    }

    // ── Step 2: Detect features in each frame ─────────────
    val f = rawFrames[0].width * 0.84 // estimated focal length
    Log.i("Stitcher", "Focal length: ${"%.0f".format(f)}")

    val allKeypoints = mutableListOf<List<FeaturePoint>>()
    val allDescriptors = mutableListOf<List<IntArray>>()
    for (i in rawFrames.indices) {
        val gray = toGrayscale(rawFrames[i])
        val kp = detectCorners(gray, maxCorners = 150)
        val desc = computeBriefLikeDescriptors(gray, kp)
        allKeypoints.add(kp)
        allDescriptors.add(desc)
        Log.i("Stitcher", "Frame $i: ${kp.size} keypoints detected")
        gray.recycle()
    }

    // ── Step 3: Match consecutive pairs ───────────────────
    // cumulativeTransforms[i] maps frame i → frame 0 coordinate space
    val cumulativeTransforms = mutableListOf(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)) // identity

    for (i in 0 until rawFrames.size - 1) {
        val matches = matchFeatures(allKeypoints[i], allDescriptors[i], allKeypoints[i + 1], allDescriptors[i + 1])
        Log.i("Stitcher", "Pair $i→${i + 1}: ${matches.size} feature matches")

        if (matches.size < 10) {
            Log.w("Stitcher", "Pair $i→${i + 1}: TOO FEW matches, using identity")
            cumulativeTransforms.add(cumulativeTransforms.last().clone())
            continue
        }

        // Compute homography with RANSAC
        val srcPts = matches.map { allKeypoints[i][it.first] }
        val dstPts = matches.map { allKeypoints[i + 1][it.second] }
        val homography = ransacHomography(srcPts, dstPts, iterations = 500, threshold = 5.0)

        if (homography == null) {
            Log.w("Stitcher", "Pair $i→${i + 1}: RANSAC failed, using identity")
            cumulativeTransforms.add(cumulativeTransforms.last().clone())
            continue
        }

        // Chain: cumulative[i+1] = H * cumulative[i]
        val chained = multiplyHomographies(homography, cumulativeTransforms.last())
        cumulativeTransforms.add(chained)
        Log.i("Stitcher", "Pair $i→${i + 1}: homography computed and chained")
    }

    // ── Step 4: Find canvas bounds ────────────────────────
    var minX = 0.0; var minY = 0.0; var maxX = 0.0; var maxY = 0.0
    for (i in rawFrames.indices) {
        val w = rawFrames[i].width.toDouble()
        val h = rawFrames[i].height.toDouble()
        val corners = arrayOf(doubleArrayOf(0.0, 0.0, 1.0), doubleArrayOf(w, 0.0, 1.0), doubleArrayOf(w, h, 1.0), doubleArrayOf(0.0, h, 1.0))
        val H = cumulativeTransforms[i]
        for (c in corners) {
            val x = H[0] * c[0] + H[1] * c[1] + H[2] * c[2]
            val y = H[3] * c[0] + H[4] * c[1] + H[5] * c[2]
            val z = H[6] * c[0] + H[7] * c[1] + H[8] * c[2]
            val px = x / z; val py = y / z
            minX = minOf(minX, px); minY = minOf(minY, py)
            maxX = maxOf(maxX, px); maxY = maxOf(maxY, py)
        }
    }
    val canvasW = (maxX - minX + 1).toInt()
    val canvasH = (maxY - minY + 1).toInt()
    Log.i("Stitcher", "Canvas bounds: ${canvasW}x${canvasH}")

    if (canvasW <= 0 || canvasH <= 0 || canvasW * canvasH > 40_000_000) {
        Log.e("Stitcher", "Canvas too large (${canvasW}x${canvasH}), falling back to simple concat")
        rawFrames.forEach { it.recycle() }
        return simpleConcatStitch(ctx, paths)
    }

    // Translation to shift all frames to positive coordinates
    val Tx = -minX; val Ty = -minY

    // ── Step 5: Warp and composite ───────────────────────
    System.gc() // free memory before large allocation
    Log.i("Stitcher", "Creating canvas: ${canvasW}x${canvasH} (${canvasW * canvasH * 4 / 1024 / 1024}MB)")
    val canvas = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
    val g = Canvas(canvas)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)

    for (i in rawFrames.indices) {
        val H = cumulativeTransforms[i]
        // Full transform: Translation * Homography
        val fullH = doubleArrayOf(
            H[0], H[1], H[2] + Tx,
            H[3], H[4], H[5] + Ty,
            H[6], H[7], H[8]
        )
        val matrix = android.graphics.Matrix()
        matrix.setValues(fullH.map { it.toFloat() }.toFloatArray())

        g.drawBitmap(rawFrames[i], matrix, paint)
        Log.i("Stitcher", "Frame $i warped onto canvas")
    }

    rawFrames.forEach { it.recycle() }

    // Save debug intermediate
    val debugInter = File(debugDir, "stitched_intermediate.jpg")
    FileOutputStream(debugInter).use { canvas.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    Log.i("Stitcher", "Debug intermediate: ${canvasW}x${canvasH} → ${debugInter.absolutePath}")

    // ── Step 6: Crop black borders ───────────────────────
    val cropped = cropBlackBorders(canvas)
    canvas.recycle()
    Log.i("Stitcher", "Cropped: ${cropped.width}x${cropped.height}, ratio: ${"%.2f".format(cropped.width.toFloat() / cropped.height)}")

    // ── Step 7: Resize to 2:1 equirectangular ────────────
    val finalW = 2048; val finalH = 1024
    val panorama = Bitmap.createScaledBitmap(cropped, finalW, finalH, true)
    cropped.recycle()
    Log.i("Stitcher", "Final: ${panorama.width}x${panorama.height} (ratio: ${"%.2f".format(panorama.width.toFloat() / panorama.height)})")

    // Save debug final
    val debugFinal = File(debugDir, "panorama_final.jpg")
    FileOutputStream(debugFinal).use { panorama.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    Log.i("Stitcher", "Debug final: ${debugFinal.absolutePath}")

    // ── Step 8: Save to cache ────────────────────────────
    try {
        val out = File(ctx.cacheDir, "panorama_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { panorama.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        panorama.recycle()
        Log.i("Stitcher", "Saved: ${out.absolutePath}")
        Log.i("Stitcher", "=== STITCHING COMPLETE ===")
        return out.absolutePath
    } catch (e: Exception) { Log.e("Stitcher", "Save failed", e); panorama.recycle(); return null }
}

// ═════════════════════════════════════════════════════════════
//  FEATURE DETECTION + MATCHING + HOMOGRAPHY
// ═════════════════════════════════════════════════════════════

data class FeaturePoint(val x: Double, val y: Double)

/** Convert bitmap to grayscale pixel array */
private fun toGrayscale(bmp: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
    val g = Canvas(result)
    val paint = android.graphics.Paint()
    val cm = android.graphics.ColorMatrix().apply { setSaturation(0f) }
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
    g.drawBitmap(bmp, 0f, 0f, paint)
    return result
}

/** Get grayscale pixel value (0-255) */
private fun getGrayPixel(pixels: IntArray, w: Int, x: Int, y: Int): Int {
    if (x < 0 || x >= w || y < 0) return 0
    val px = pixels[y * w + x]
    return (AndroidColor.red(px) + AndroidColor.green(px) + AndroidColor.blue(px)) / 3
}

/** Detect corner features using a simplified FAST-like detector */
private fun detectCorners(gray: Bitmap, maxCorners: Int): List<FeaturePoint> {
    val w = gray.width; val h = gray.height
    val pixels = IntArray(w * h)
    gray.getPixels(pixels, 0, w, 0, 0, w, h)

    val threshold = 30
    val candidates = mutableListOf<FeaturePoint>()

    // Sample a grid of candidate positions
    val step = 8
    for (y in 16 until h - 16 step step) {
        for (x in 16 until w - 16 step step) {
            val center = getGrayPixel(pixels, w, x, y)
            // Check 16 surrounding points (FAST-16 pattern)
            var brighter = 0; var darker = 0
            val offsets = intArrayOf(-8, -4, 0, 4, 8) // simplified: check 8 points on axes + diagonals
            for (dx in offsets) {
                for (dy in offsets) {
                    if (dx == 0 && dy == 0) continue
                    val neighbor = getGrayPixel(pixels, w, x + dx, y + dy)
                    if (neighbor > center + threshold) brighter++
                    if (neighbor < center - threshold) darker++
                }
            }
            // Corner: enough points significantly brighter OR darker
            if (brighter >= 6 || darker >= 6) {
                candidates.add(FeaturePoint(x.toDouble(), y.toDouble()))
            }
        }
    }

    // Non-maximum suppression: keep strongest corners
    // Sort by "corner strength" (simplified: count of extreme neighbors)
    val scored = candidates.map { pt ->
        val cx = pt.x.toInt(); val cy = pt.y.toInt()
        val center = getGrayPixel(pixels, w, cx, cy)
        var score = 0
        for (dx in -4..4 step 2) {
            for (dy in -4..4 step 2) {
                val n = getGrayPixel(pixels, w, cx + dx, cy + dy)
                if (kotlin.math.abs(n - center) > threshold) score++
            }
        }
        Pair(pt, score)
    }.sortedByDescending { it.second }

    // Keep top N with minimum distance
    val selected = mutableListOf<FeaturePoint>()
    val minDist = 15.0
    for ((pt, _) in scored) {
        if (selected.size >= maxCorners) break
        val tooClose = selected.any { kotlin.math.sqrt((it.x - pt.x) * (it.x - pt.x) + (it.y - pt.y) * (it.y - pt.y)) < minDist }
        if (!tooClose) selected.add(pt)
    }

    return selected
}

/** Compute simple binary descriptor (BRIEF-like) for each keypoint */
private fun computeBriefLikeDescriptors(gray: Bitmap, keypoints: List<FeaturePoint>): List<IntArray> {
    val w = gray.width; val h = gray.height
    val pixels = IntArray(w * h)
    gray.getPixels(pixels, 0, w, 0, 0, w, h)

    val descriptorSize = 32 // 32 bytes = 256 bits
    val sampleRadius = 12

    return keypoints.map { kp ->
        val cx = kp.x.toInt(); val cy = kp.y.toInt()
        val desc = IntArray(descriptorSize)
        for (byteIdx in 0 until descriptorSize) {
            var bits = 0
            for (bitIdx in 0 until 8) {
                // Random-ish sample pairs (deterministic from keypoint position)
                val seed = cx * 31 + cy * 17 + byteIdx * 7 + bitIdx * 13
                val dx1 = (seed % (sampleRadius * 2)) - sampleRadius
                val dy1 = ((seed / sampleRadius) % (sampleRadius * 2)) - sampleRadius
                val dx2 = ((seed * 3) % (sampleRadius * 2)) - sampleRadius
                val dy2 = (((seed * 3) / sampleRadius) % (sampleRadius * 2)) - sampleRadius
                val p1 = getGrayPixel(pixels, w, cx + dx1, cy + dy1)
                val p2 = getGrayPixel(pixels, w, cx + dx2, cy + dy2)
                if (p1 > p2) bits = bits or (1 shl bitIdx)
            }
            desc[byteIdx] = bits
        }
        desc
    }
}

/** Match features between two frames using descriptor hamming distance */
private fun matchFeatures(
    kp1: List<FeaturePoint>, desc1: List<IntArray>,
    kp2: List<FeaturePoint>, desc2: List<IntArray>
): List<Pair<Int, Int>> {
    if (desc1.isEmpty() || desc2.isEmpty()) return emptyList()

    val matches = mutableListOf<Pair<Int, Int>>()
    for (i in desc1.indices) {
        var bestDist = Int.MAX_VALUE; var bestJ = -1
        var secondDist = Int.MAX_VALUE
        for (j in desc2.indices) {
            var dist = 0
            for (k in desc1[i].indices) {
                dist += Integer.bitCount(desc1[i][k] xor desc2[j][k])
            }
            if (dist < bestDist) { secondDist = bestDist; bestDist = dist; bestJ = j }
            else if (dist < secondDist) { secondDist = dist }
        }
        // Lowe's ratio test
        if (bestJ >= 0 && bestDist < 0.75f * secondDist && bestDist < 60) {
            matches.add(Pair(i, bestJ))
        }
    }
    return matches
}

/** Compute homography using RANSAC + DLT */
private fun ransacHomography(src: List<FeaturePoint>, dst: List<FeaturePoint>, iterations: Int, threshold: Double): DoubleArray? {
    if (src.size < 4) return null

    var bestH = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
    var bestInliers = 0

    val rng = java.util.Random(42)
    for (iter in 0 until iterations) {
        // Pick 4 random correspondences
        val idx = (0 until src.size).shuffled(rng).take(4)
        val s = idx.map { src[it] }
        val d = idx.map { dst[it] }

        val H = computeHomographyDLT(s, d) ?: continue

        // Count inliers
        var inliers = 0
        for (i in src.indices) {
            val px = H[0] * src[i].x + H[1] * src[i].y + H[2]
            val py = H[3] * src[i].x + H[4] * src[i].y + H[5]
            val pz = H[6] * src[i].x + H[7] * src[i].y + H[8]
            val projX = px / pz; val projY = py / pz
            val err = kotlin.math.sqrt((projX - dst[i].x) * (projX - dst[i].x) + (projY - dst[i].y) * (projY - dst[i].y))
            if (err < threshold) inliers++
        }

        if (inliers > bestInliers) { bestInliers = inliers; bestH = H }
    }

    Log.i("Stitcher", "  RANSAC: best inliers = $bestInliers/${src.size}")
    return if (bestInliers >= 4) bestH else null
}

/** Compute homography from 4 point correspondences using DLT */
private fun computeHomographyDLT(src: List<FeaturePoint>, dst: List<FeaturePoint>): DoubleArray? {
    if (src.size != 4 || dst.size != 4) return null

    // Build 8x9 matrix A from the 4 correspondences
    val A = Array(8) { DoubleArray(9) }
    for (i in 0..3) {
        val sx = src[i].x; val sy = src[i].y
        val dx = dst[i].x; val dy = dst[i].y
        A[i * 2] = doubleArrayOf(sx, sy, 1.0, 0.0, 0.0, 0.0, -dx * sx, -dx * sy, -dx)
        A[i * 2 + 1] = doubleArrayOf(0.0, 0.0, 0.0, sx, sy, 1.0, -dy * sx, -dy * sy, -dy)
    }

    // Solve via Gaussian elimination
    val n = 8; val m = 9
    for (col in 0 until n) {
        // Find pivot
        var maxRow = col
        for (row in col + 1 until n) { if (kotlin.math.abs(A[row][col]) > kotlin.math.abs(A[maxRow][col])) maxRow = row }
        val tmp = A[col]; A[col] = A[maxRow]; A[maxRow] = tmp
        if (kotlin.math.abs(A[col][col]) < 1e-10) return null
        // Eliminate below
        for (row in col + 1 until n) {
            val factor = A[row][col] / A[col][col]
            for (j in col until m) { A[row][j] -= factor * A[col][j] }
        }
    }
    // Back-substitute
    val H = DoubleArray(9)
    for (i in n - 1 downTo 0) {
        H[i] = A[i][n]
        for (j in i + 1 until n) { H[i] -= A[i][j] * H[j] }
        H[i] /= A[i][i]
    }
    return H
}

/** Multiply two 3x3 homography matrices */
private fun multiplyHomographies(a: DoubleArray, b: DoubleArray): DoubleArray {
    val result = DoubleArray(9)
    for (row in 0..2) {
        for (col in 0..2) {
            result[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col]
        }
    }
    return result
}

// ═════════════════════════════════════════════════════════════
//  LEGACY: SAD overlap detection (kept as fallback reference)
// ═════════════════════════════════════════════════════════════

/** Find overlap: how many pixels from right of A match left of B via SAD */
private fun findOverlap(a: Bitmap, b: Bitmap): Int {
    val maxSearch = (a.width * 0.5).toInt().coerceAtMost(400)
    val h = minOf(a.height, b.height)
    val step = 4
    var bestScore = Long.MAX_VALUE
    var bestOffset = 0
    val sampleRows = (0 until h step step).toList()

    for (offset in 50 until maxSearch step 2) {
        var totalDiff = 0L; var count = 0
        for (row in sampleRows) {
            val colA = a.width - offset; if (colA < 0) continue
            for (dy in 0 until h step h / 5 + 1) {
                val px = a.getPixel(colA, dy); val py = b.getPixel(0, dy)
                totalDiff += kotlin.math.abs(AndroidColor.red(px) - AndroidColor.red(py))
                totalDiff += kotlin.math.abs(AndroidColor.green(px) - AndroidColor.green(py))
                totalDiff += kotlin.math.abs(AndroidColor.blue(px) - AndroidColor.blue(py))
                count++
            }
        }
        if (count > 0) { val avg = totalDiff / count; if (avg < bestScore) { bestScore = avg; bestOffset = offset } }
    }
    Log.i("Stitcher", "  Best overlap: ${bestOffset}px (score=$bestScore)")
    return if (bestScore < 100) bestOffset else (a.width / 3)
}

/** Crop black borders */
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

/** Simple concatenation fallback when feature-based stitching fails */
private fun simpleConcatStitch(ctx: android.content.Context, paths: List<String>): String? {
    Log.w("Stitcher", "Using simple concatenation fallback")
    try {
        val targetH = 600
        val frames = paths.mapNotNull { p ->
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(p, opts)
                val sample = (opts.outHeight / targetH).coerceAtLeast(1)
                BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = sample })
            } catch (_: Exception) { null }
        }.filter { it != null && !it.isRecycled }.map { it!! }
        if (frames.isEmpty()) return null

        var totalW = frames.sumOf { it.width }
        if (totalW > 8000) totalW = 8000
        val h = frames[0].height
        val scale = totalW.toFloat() / frames.sumOf { it.width }

        val canvas = Bitmap.createBitmap(totalW, h, Bitmap.Config.ARGB_8888)
        val g = Canvas(canvas)
        var x = 0f
        frames.forEach { f ->
            val drawW = f.width * scale
            g.drawBitmap(f, null, RectF(x, 0f, x + drawW, h.toFloat()), null)
            x += drawW
        }
        frames.forEach { it.recycle() }

        val out = File(ctx.cacheDir, "panorama_fallback_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { canvas.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        canvas.recycle()
        Log.i("Stitcher", "Fallback saved: ${out.absolutePath}")
        return out.absolutePath
    } catch (e: Exception) { Log.e("Stitcher", "Fallback failed", e); return null }
}

private fun buildJson(stitchedPath: String?, frames: List<String>, roomId: String): String {
    val json = JSONObject()
    if (stitchedPath != null) json.put("stitchedPanorama", stitchedPath)
    val arr = JSONArray(); frames.forEach { arr.put(it) }
    json.put("frames", arr); json.put("frameCount", frames.size)
    json.put("roomId", roomId); json.put("timestamp", System.currentTimeMillis())
    return json.toString()
}

private fun vibrateShutter(ctx: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1)) }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { @Suppress("DEPRECATION") (ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 20), intArrayOf(0, 200, 0, 120), -1)) }
    } catch (_: Exception) {}
}
