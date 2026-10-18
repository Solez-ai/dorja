package com.example.ui.scanner

import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.scanner.CameraManager
import com.example.scanner.CaptureState
import com.example.scanner.SensorFusionManager
import com.example.ui.i18n.L
import com.example.ui.i18n.Lf
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Guided-capture card colors (glass over the black viewfinder).
private val GlassBg = Color(0xCC0D1520)
private val AccentAmber = Color(0xFFFFB74D)
private val AccentGreen = Color(0xFF4ADE80)
private val GhostAlpha = 0.35f

/**
 * Room scanner — full replacement of the old sweep system with the
 * PanoramaStitching-Android-OpenCV capture strategy, brought into Dorja:
 *
 * - A guided 360-degree ring (12 targets): the interactive reticle tells the
 *   user exactly where to point, and the shutter auto-fires once they hold it
 *   steady — the interactive "where to point / when to shoot" UX the old app
 *   lacked.
 * - A ghost of the previous frame is blended over the viewfinder at reduced
 *   alpha, exactly like the reference app's overlay, so overlap alignment is
 *   visual too, not just sensor-based.
 * - On ring completion the frames go to the native cv::Stitcher port
 *   (same PANORAMA-mode recipe) and the result is saved as the
 *   viewer-compatible panorama JSON on the listing's first room.
 */
@Composable
fun RoomScannerScreen(
    listingId: String,
    onBack: () -> Unit = {},
    onScanComplete: (String, String) -> Unit = { _, _ -> },
    viewModel: ScannerViewModel? = null,
    cameraManager: CameraManager? = null,
    sensorFusion: SensorFusionManager? = null
) {
    val vm = remember { viewModel ?: ScannerViewModel() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val captureState by vm.captureState.collectAsState()
    val orientation by vm.orientation.collectAsState()
    val targetIndex by vm.targetIndex.collectAsState()
    val capturedCount by vm.capturedCount.collectAsState()
    val stitchStage by vm.stitchStage.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val ghostTick by vm.ghostTick.collectAsState()

    var frameCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var panoCameraManager by remember { mutableStateOf<CameraManager?>(null) }
    var fusion by remember { mutableStateOf<SensorFusionManager?>(null) }

    // Camera + sensor bring-up
    LaunchedEffect(Unit) {
        frameCapture = ImageCapture.Builder().build()
        fusion = sensorFusion ?: SensorFusionManager(context)
        fusion?.start()
        fusion?.onOrientationUpdate = { newOrientation, _ ->
            vm.updateOrientation(newOrientation)
        }
    }

    DisposableEffect(Unit) {
        onDispose { fusion?.stop() }
    }

    // Camera bring-up happens inside CameraPreview; this capture handle lets
    // us take real JPEG frames once the manager reports ready.
    val shutterScope = rememberCoroutineScope()
    LaunchedEffect(vm) {
        vm.captureTick.collect { tick ->
            if (tick == 0) return@collect
            val manager = panoCameraManager ?: return@collect
            val dir = File(context.cacheDir, "pano_frames").apply { mkdirs() }
            val file = File(dir, "frame_${System.currentTimeMillis()}.jpg")
            val ok = manager.takePhotoTo(file)
            withContext(Dispatchers.Main) {
                if (ok) {
                    vm.onFrameCaptured(file.path)
                } else {
                    vm.captureStateCaptureFailed()
                }
            }
        }
    }

    // Full ring captured -> native stitch -> save panorama JSON
    LaunchedEffect(captureState) {
        if (vm.captureState.value == CaptureState.COMPLETED && vm.stitchStage.value == null) {
            vm.setStitchStage("stitching")
            scope.launch {
                val frames = vm.captured.value
                val engine = com.example.scanner.StitchingEngine()
                val outDir = File(context.filesDir, "panoramas")
                val out = File(outDir, "pano_${listingId}_${System.currentTimeMillis()}.jpg")
                val path = engine.stitchFrames(frames, out)
                withContext(Dispatchers.Main) {
                    if (path != null) {
                        val json = JSONObject().apply {
                            put("version", 2)
                            put("stitchedPanorama", path)
                            put("capturedAt", System.currentTimeMillis())
                        }
                        try {
                            val repo = DorjaApp.instance.repository
                            val roomId = repo.getRoomsByListingSync(listingId)
                                .firstOrNull()?.id
                            if (roomId != null) {
                                repo.updateRoom3DScan(roomId, json.toString())
                            }
                            vm.setStitchStage(null)
                            onScanComplete(listingId, roomId ?: "")
                        } catch (e: Exception) {
                            vm.setError("scan_error_sys")
                            vm.setStitchStage(null)
                        }
                    } else {
                        vm.setError("scan_error_stitch")
                        vm.setStitchStage(null)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Live camera feed with the ImageCapture use case bound for real shots
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            cameraManager = cameraManager,
            imageCapture = frameCapture,
            onManagerReady = { panoCameraManager = it }
        )

        // Ghost overlay: previous frame at reduced alpha (reference-app UX)
        val ghostPath = vm.lastFramePath
        if (ghostPath != null && ghostTick > 0 &&
            captureState != CaptureState.WAITING_FOR_INITIAL_ANCHOR
        ) {
            val ghost = remember(ghostTick) {
                BitmapFactory.decodeFile(
                    ghostPath,
                    BitmapFactory.Options().apply { inSampleSize = 2 }
                )
            }
            if (ghost != null) {
                Image(
                    bitmap = ghost.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = GhostAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Interactive guidance: ring targets + reticle alignment
        RingTargetsCanvas(
            headingDeg = orientation.headingDeg,
            targets = vm.targets,
            activeIndex = targetIndex,
            capturedCount = capturedCount,
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp)
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(L("scan_back"), color = Color.White) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    L("scan_title"),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (captureState != CaptureState.WAITING_FOR_INITIAL_ANCHOR) {
                    Text(
                        Lf("scan_frame_progress", capturedCount, ScannerViewModel.TARGET_COUNT),
                        color = Color(0xFF9FB3C8),
                        fontSize = 12.sp
                    )
                }
            }
            Row(modifier = Modifier.width(64.dp)) {}
        }

        // Bottom guidance / controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (captureState) {
                CaptureState.WAITING_FOR_INITIAL_ANCHOR -> {
                    Button(
                        onClick = { vm.startScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentAmber,
                            contentColor = Color(0xFF101418)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            L("scan_start"),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        L("scan_start_hint"),
                        color = Color(0xFF9FB3C8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                CaptureState.MOVING_TO_TARGET -> {
                    Text(
                        L("scan_point_guide"),
                        color = AccentAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    // Manual fallback shutter for devices without a usable gyro
                    TextButton(onClick = { vm.manualCapture() }) {
                        Text(L("scan_capture_now"), color = Color.White)
                    }
                }
                CaptureState.STABILIZING -> {
                    Text(
                        L("scan_hold_steady"),
                        color = AccentGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                CaptureState.COMPLETED, CaptureState.CAPTURING_INITIAL_FRAME -> {
                    // Stitching overlay covers the screen; nothing extra here.
                }
                CaptureState.ERROR -> {}
            }
        }

        // Full-screen stitching overlay
        if (captureState == CaptureState.COMPLETED && stitchStage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                StitchingCard()
            }
        }

        // Full-screen error overlay
        if (captureState == CaptureState.ERROR) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                ErrorCard(message = errorMessage, onRetry = { vm.retryScan() })
            }
        }
    }
}

/**
 * The guidance reticle: a circle of 12 dots representing the full 360 ring.
 * The ring rotates with the device heading so the live aim is always the top
 * marker; the next target glows amber, captured ones are green.
 */
@Composable
private fun RingTargetsCanvas(
    headingDeg: Float,
    targets: List<Float>,
    activeIndex: Int,
    capturedCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f * 0.82f
        val center = Offset(size.width / 2f, size.height / 2f)
        val dotR = 7.dp.toPx()
        val ringStroke = 1.5.dp.toPx()

        drawCircle(
            color = Color(0x55FFFFFF),
            radius = radius,
            center = center,
            style = Stroke(width = ringStroke)
        )

        targets.forEachIndexed { index, targetYaw ->
            // Rotate the ring so the device's current aim points up.
            val rel = (targetYaw - headingDeg + 360f) % 360f - 180f
            val angle = Math.toRadians((rel + 0.0) - 90.0)
            val pos = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat()
            )
            val isCaptured = index < capturedCount
            val isActive = index == activeIndex
            val color = when {
                isActive -> AccentAmber
                isCaptured -> AccentGreen
                else -> Color(0x88FFFFFF)
            }
            val r = if (isActive) dotR * 1.6f else dotR
            drawCircle(color = color, radius = r, center = pos)
            if (isActive) {
                drawCircle(
                    color = AccentAmber.copy(alpha = 0.35f),
                    radius = r * 2.0f,
                    center = pos
                )
            }
        }

        // Reticle marker at the top of the ring (the phone's current aim)
        val aim = Offset(center.x, center.y - radius)
        drawCircle(color = Color.White, radius = dotR * 0.7f, center = aim)
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = dotR * 1.6f,
            center = aim,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun StitchingCard() {
    Column(
        modifier = Modifier
            .background(GlassBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = AccentAmber,
            modifier = Modifier.size(38.dp)
        )
        Text(
            L("scan_stitching"),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            L("scan_stitching_hint"),
            color = Color(0xFF9FB3C8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ErrorCard(message: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .background(GlassBg, RoundedCornerShape(20.dp))
            .padding(horizontal = 28.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            L("scan_error_title"),
            color = Color(0xFFF87171),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            if (message != null) L(message) else L("scan_error_generic"),
            color = Color(0xFF9FB3C8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentAmber,
                contentColor = Color(0xFF101418)
            )
        ) { Text(L("scan_retry")) }
    }
}
