@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@file:SuppressLint("UnsafeOptInUsageError")

package com.example.ui.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.data.repository.DorjaRepository
import com.example.scanner.photosphere.CaptureFeedback
import com.example.scanner.photosphere.CapturedFrame
import com.example.scanner.photosphere.FieldOfView
import com.example.scanner.photosphere.FocusMode
import com.example.scanner.photosphere.OrientationData
import com.example.scanner.photosphere.PhotoSphereStitcher
import com.example.scanner.photosphere.PivotModel
import com.example.scanner.photosphere.SphereCaptureCoordinator
import com.example.scanner.photosphere.SphereCaptureProfile
import com.example.scanner.photosphere.SphereCaptureScope
import com.example.scanner.photosphere.SphereCaptureSessionState
import com.example.scanner.photosphere.SphereDeviceProfile
import com.example.scanner.photosphere.SphereOptics
import com.example.scanner.photosphere.SphereProjection
import com.example.scanner.photosphere.SphereTarget
import com.example.scanner.photosphere.TargetOverlay
import com.example.scanner.photosphere.applySphereCaptureOptions
import com.example.scanner.photosphere.applyStillImageOptions
import com.example.scanner.photosphere.captureBackCameraId
import com.example.scanner.photosphere.rememberCaptureFeedback
import com.example.scanner.photosphere.rememberOrientationTracker
import com.example.scanner.photosphere.rememberSphereOptics
import com.example.scanner.photosphere.resolveSphereCaptureProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val TAG = "RoomScannerScreen"
private val TargetTapRadius = 36.dp
private val FocusRingRadius = 28.dp
private const val FOCUS_RING_VISIBLE_MILLIS = 900L

/**
 * Dorja 3D Room Scanner powered by the `360-photo-app` Horizon Ring (`SphereCaptureScope.Ring`)
 * guided capture flow and spherical equirectangular stitcher.
 */
@Composable
fun RoomScannerScreen(
    listingId: String,
    onBack: () -> Unit,
    onScanComplete: (roomId: String, panoramaJson: String) -> Unit = { _, _ -> },
    repo: DorjaRepository = DorjaApp.instance.repository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rooms by remember { mutableStateOf<List<RoomItem>>(emptyList()) }
    var selectedRoomIndex by remember { mutableIntStateOf(0) }
    var isLoadingRooms by remember { mutableStateOf(true) }

    LaunchedEffect(listingId) {
        var loaded = repo.getRoomsByListingSync(listingId)
        if (loaded.isEmpty()) {
            // Auto-create a default room if none exist so scanning works immediately
            val fallbackRoom = RoomItem(
                id = "room_${UUID.randomUUID().toString().take(8)}",
                listingId = listingId,
                roomType = "LIVING_ROOM",
                displayName = "Living Room",
                dimensions = "16x14 ft",
                has3DScan = false,
                description = "Main living space",
            )
            runCatching { repo.addRoom(fallbackRoom) }
            loaded = repo.getRoomsByListingSync(listingId).ifEmpty { listOf(fallbackRoom) }
        }
        rooms = loaded
        val firstUnscanned = loaded.indexOfFirst { !it.has3DScan }
        selectedRoomIndex = if (firstUnscanned >= 0) firstUnscanned else 0
        isLoadingRooms = false
    }

    if (isLoadingRooms) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0D12)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF4FC3F7))
        }
        return
    }

    val currentRoom = rooms.getOrNull(selectedRoomIndex)

    HorizonGuidedScannerContent(
        roomTitle = currentRoom?.displayName ?: "Room",
        rooms = rooms,
        selectedRoomIndex = selectedRoomIndex,
        onSelectRoom = { idx -> selectedRoomIndex = idx },
        onBack = onBack,
        onPanoramaStitched = { panoramaJson ->
            val targetRoom = currentRoom
            if (targetRoom != null) {
                scope.launch {
                    runCatching { repo.updateRoom3DScan(targetRoom.id, panoramaJson) }
                    val refreshed = repo.getRoomsByListingSync(listingId)
                    if (refreshed.isNotEmpty()) rooms = refreshed
                    onScanComplete(targetRoom.id, panoramaJson)
                }
            } else {
                onBack()
            }
        },
    )
}

/**
 * Full-screen modal wrapper so `CreateListingScreen` can also launch the Horizon 360° Scanner
 * for draft rooms before the listing has been published.
 */
@Composable
fun HorizonRoomScannerModal(
    roomName: String,
    onCompleted: (panoramaJson: String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        HorizonGuidedScannerContent(
            roomTitle = roomName,
            rooms = emptyList(),
            selectedRoomIndex = 0,
            onSelectRoom = {},
            onBack = onDismiss,
            onPanoramaStitched = onCompleted,
        )
    }
}

@Composable
private fun HorizonGuidedScannerContent(
    roomTitle: String,
    rooms: List<RoomItem>,
    selectedRoomIndex: Int,
    onSelectRoom: (Int) -> Unit,
    onBack: () -> Unit,
    onPanoramaStitched: (panoramaJson: String) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // Lock scanner to portrait for consistent horizon ring geometry and keep screen awake
    DisposableEffect(context, view) {
        val activity = context.findActivity()
        val prevOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val prevKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = prevKeepScreenOn
            if (prevOrientation != null) {
                activity.requestedOrientation = prevOrientation
            }
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        PermissionRequiredView(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = onBack,
        )
        return
    }

    val deviceProfile = remember(context) { SphereDeviceProfile.forDevice(context) }
    var boundCameraId by remember { mutableStateOf<String?>(null) }
    var streamAspectRatio by remember { mutableFloatStateOf(4f / 3f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    val optics = rememberSphereOptics(
        cameraId = boundCameraId,
        streamAspectRatio = streamAspectRatio,
    )
    val captureProfile = remember(context, deviceProfile, boundCameraId) {
        resolveSphereCaptureProfile(context, deviceProfile, boundCameraId)
    }

    val orientationTracker = rememberOrientationTracker(
        fixedDisplayRotation = android.view.Surface.ROTATION_0,
    )
    val orientation by orientationTracker.orientation.collectAsState()
    val captureFeedback = rememberCaptureFeedback()

    // Coordinator defaults to SphereCaptureScope.FullSphere (guides horizon ring first,
    // then upper ceiling ring and lower floor ring so the entire 360° × 180° sphere is captured)
    val coordinator = remember(selectedRoomIndex) {
        SphereCaptureCoordinator(
            fieldOfView = optics.fieldOfView,
            defaultScope = SphereCaptureScope.FullSphere,
        )
    }
    LaunchedEffect(optics.fieldOfView) {
        coordinator.onFieldOfViewChanged(optics.fieldOfView)
    }

    val sessionState by coordinator.state.collectAsState()
    val currentOrientation by rememberUpdatedState(orientation)

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusLocked by remember { mutableStateOf(false) }
    var isExposureLocked by remember { mutableStateOf(false) }
    var isStitching by remember { mutableStateOf(false) }
    var stitchProgress by remember { mutableFloatStateOf(0f) }
    var stitchError by remember { mutableStateOf<String?>(null) }

    val flashAlpha = remember { Animatable(0f) }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }

    // Halfway dwell haptic tick so the user feels the dwell filling
    var previousDwellProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(sessionState.alignment.dwellProgress) {
        val current = sessionState.alignment.dwellProgress
        if (previousDwellProgress < 0.5f && current >= 0.5f) {
            captureFeedback.playDwellTick()
        }
        previousDwellProgress = current
    }

    // Helper to trigger stitching and save result
    val runStitchAndSave: (List<CapturedFrame>, SphereCaptureScope) -> Unit = { framesToStitch, scopeMode ->
        if (!isStitching && framesToStitch.isNotEmpty()) {
            isStitching = true
            stitchProgress = 0f
            stitchError = null
            coroutineScope.launch {
                val result = runCatching {
                    val outDir = File(context.filesDir, "panoramas").apply { mkdirs() }
                    val timestamp = System.currentTimeMillis()
                    val outFile = File(outDir, "horizon_pano_${timestamp}.jpg")
                    val stitcher = PhotoSphereStitcher()
                    val stitchRes = stitcher.stitchToFile(
                        frames = framesToStitch,
                        optics = optics,
                        scope = scopeMode,
                        outputFile = outFile,
                        deviceProfile = deviceProfile,
                        pivotModel = PivotModel.HandheldBodySwivel,
                        onProgress = { p -> stitchProgress = p },
                    )
                    buildPanoramaMetadataJson(
                        stitchResult = stitchRes,
                        scope = scopeMode,
                        roomTitle = roomTitle,
                    )
                }
                isStitching = false
                result.onSuccess { json ->
                    onPanoramaStitched(json)
                }.onFailure { err ->
                    Log.e(TAG, "Horizon stitch failed", err)
                    stitchError = err.message ?: "Could not stitch panorama. Please try again."
                }
            }
        }
    }

    // Feed orientation samples into the Horizon Ring coordinator and fire the shutter when dwell completes
    LaunchedEffect(orientation, isStitching) {
        if (isStitching) return@LaunchedEffect
        val triggeredTarget = coordinator.onOrientationSample(orientation) ?: return@LaunchedEffect
        val imageCapture = imageCaptureRef
        if (imageCapture == null) {
            coordinator.onFrameCaptureFailed()
            return@LaunchedEffect
        }

        val baos = ByteArrayOutputStream()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(baos).build()
        imageCapture.takePicture(
            outputOptions,
            captureExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bytes = baos.toByteArray()
                    val poseAtShutter = currentOrientation
                    coroutineScope.launch {
                        if (bytes.isNotEmpty()) {
                            captureFeedback.onFrameCaptured()
                            launch {
                                flashAlpha.snapTo(0.65f)
                                flashAlpha.animateTo(0f, animationSpec = tween(180))
                            }
                            coordinator.onFrameCaptured(
                                target = triggeredTarget,
                                imageBytes = bytes,
                                orientationAtCapture = poseAtShutter,
                            )
                            val updated = coordinator.state.value
                            if (updated.isComplete && updated.capturedFrames.isNotEmpty()) {
                                delay(220L)
                                runStitchAndSave(updated.capturedFrames, updated.scope)
                            }
                        } else {
                            coordinator.onFrameCaptureFailed()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.w(TAG, "Frame capture failed", exception)
                    coroutineScope.launch {
                        coordinator.onFrameCaptureFailed()
                    }
                }
            }
        )
    }

    // Clear tap-to-focus reticle after a brief moment
    LaunchedEffect(tapFocusPoint) {
        if (tapFocusPoint != null) {
            delay(FOCUS_RING_VISIBLE_MILLIS)
            tapFocusPoint = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { viewportSize = it }
            .pointerInput(sessionState, optics.fieldOfView, boundCamera, previewViewRef) {
                detectTapGestures { tapOffset ->
                    // 1. Check if the user tapped an uncaptured target dot to claim it
                    val hitTarget = findTappedTarget(
                        tapOffset = tapOffset,
                        viewportSize = viewportSize,
                        orientation = currentOrientation,
                        sessionState = sessionState,
                        fieldOfView = optics.fieldOfView,
                        hitRadiusPx = with(density) { TargetTapRadius.toPx() },
                    )
                    if (hitTarget != null) {
                        coordinator.selectTarget(hitTarget.id)
                        return@detectTapGestures
                    }

                    // 2. Otherwise trigger tap-to-focus at the tapped point and lock focus for the sweep
                    val cam = boundCamera ?: return@detectTapGestures
                    val pv = previewViewRef ?: return@detectTapGestures
                    if (captureProfile.focusMode == FocusMode.FOCUS_POINT) {
                        val point = pv.meteringPointFactory.createPoint(tapOffset.x, tapOffset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .disableAutoCancel()
                            .build()
                        cam.cameraControl.startFocusAndMetering(action)
                        tapFocusPoint = tapOffset
                        isFocusLocked = true
                    }
                }
            }
    ) {
        // 1. Live CameraX Preview with 3A convergence lock
        HorizonCameraPreview(
            deviceProfile = deviceProfile,
            captureProfile = captureProfile,
            onCameraBound = { camera, previewView, imageCapture, camId ->
                boundCamera = camera
                previewViewRef = previewView
                imageCaptureRef = imageCapture
                boundCameraId = camId
                isExposureLocked = false
                // Lock initial focus on the center of the first scene
                if (captureProfile.focusMode == FocusMode.FOCUS_POINT && previewView.width > 0 && previewView.height > 0) {
                    val centerPt = previewView.meteringPointFactory.createPoint(
                        previewView.width / 2f,
                        previewView.height / 2f,
                    )
                    val action = FocusMeteringAction.Builder(centerPt, FocusMeteringAction.FLAG_AF)
                        .disableAutoCancel()
                        .build()
                    camera.cameraControl.startFocusAndMetering(action)
                    isFocusLocked = true
                }
            },
            onAeAwbConverged = {
                val camera = boundCamera ?: return@HorizonCameraPreview
                if (!isExposureLocked) {
                    isExposureLocked = true
                    runCatching {
                        val c2Control = Camera2CameraControl.from(camera.cameraControl)
                        val optsBuilder = CaptureRequestOptions.Builder()
                        if (captureProfile.aeLockSupported) {
                            optsBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
                        }
                        if (captureProfile.awbLockSupported) {
                            optsBuilder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
                        }
                        c2Control.addCaptureRequestOptions(optsBuilder.build())
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 2. 3D Pinhole-Projected Horizon Ring Target Overlay (center reticle, dwell arc, active dot, guide line, edge chevron)
        TargetOverlay(
            orientation = orientation,
            sessionState = sessionState,
            fieldOfView = optics.fieldOfView,
            showFrameFootprints = true,
            modifier = Modifier.fillMaxSize(),
        )

        // 3. Tap-to-focus ring indicator
        tapFocusPoint?.let { focusOffset ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radiusPx = FocusRingRadius.toPx()
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = radiusPx,
                    center = focusOffset,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = 0.25f),
                    radius = 4.dp.toPx(),
                    center = focusOffset,
                )
            }
        }

        // 4. White shutter flash overlay
        if (flashAlpha.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }

        // 5. Top HUD: Back button, Room chips, Horizon 360° badge, Shot counter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }

                // 360° Sphere / Horizon Mode Toggle Pill + Progress Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val canSwitchScope = sessionState.capturedFrames.isEmpty() && !isStitching
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF4FC3F7).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.65f)),
                        modifier = Modifier.clickable(enabled = canSwitchScope) {
                            val nextScope = if (sessionState.scope == SphereCaptureScope.FullSphere) {
                                SphereCaptureScope.Ring
                            } else {
                                SphereCaptureScope.FullSphere
                            }
                            coordinator.setScope(nextScope)
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (sessionState.scope == SphereCaptureScope.FullSphere) {
                                    "FULL 360° SPHERE"
                                } else {
                                    "HORIZON 360°"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            )
                        }
                    }

                    val capturedCount = sessionState.capturedFrames.size
                    val totalCount = sessionState.targets.size
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    ) {
                        Text(
                            text = "$capturedCount / $totalCount",
                            color = if (sessionState.isComplete) Color(0xFF81C784) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }

                // Restart button
                IconButton(
                    onClick = { coordinator.restart() },
                    enabled = sessionState.capturedFrames.isNotEmpty() && !isStitching,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Horizon Scan",
                        tint = if (sessionState.capturedFrames.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.35f),
                    )
                }
            }

            // Room selector pills when scanning a listing with rooms
            if (rooms.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rooms.forEachIndexed { index, room ->
                        val isSelected = index == selectedRoomIndex
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isSelected -> Color(0xFF4FC3F7)
                                room.has3DScan -> Color(0xFF81C784).copy(alpha = 0.22f)
                                else -> Color.Black.copy(alpha = 0.55f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> Color(0xFF4FC3F7)
                                    room.has3DScan -> Color(0xFF81C784)
                                    else -> Color.White.copy(alpha = 0.22f)
                                },
                            ),
                            modifier = Modifier.clickable(enabled = !isStitching) {
                                onSelectRoom(index)
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (room.has3DScan) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else Color(0xFF81C784),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                                Text(
                                    text = room.displayName,
                                    color = if (isSelected) Color(0xFF0A0D12) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Bottom HUD: Live Horizon Guidance Pill + Focus/Exposure Status + Undo & Finish/Stitch CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Error banner if stitching failed
            stitchError?.let { errorMsg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD32F2F).copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = errorMsg,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { stitchError = null },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }

            // Live step-by-step 360° Sphere guidance pill (guides Horizon -> Upper/Ceiling -> Lower/Floor)
            val activeElevation = sessionState.activeTarget?.elevationDegrees ?: 0f
            val guidanceText = when {
                !orientation.hasFix ->
                    "Initializing gyroscope sensor… Hold phone upright in portrait."
                sessionState.isComplete ->
                    "Full 360° Sphere captured! Completing stitch…"
                sessionState.alignment.isWithinThreshold ->
                    "Hold steady… (${(sessionState.alignment.dwellProgress * 100).roundToInt()}%)"
                sessionState.capturedFrames.isEmpty() ->
                    "Stand in the center of $roomTitle. Align the center ring with the glowing blue dot on the horizon."
                activeElevation >= 65f ->
                    "Tilt straight UP to the ceiling dot (${sessionState.capturedFrames.size} of ${sessionState.targets.size} captured)"
                activeElevation <= -65f ->
                    "Tilt straight DOWN to the floor dot (${sessionState.capturedFrames.size} of ${sessionState.targets.size} captured)"
                activeElevation > 12f ->
                    "Tilt UP to capture the upper wall & ceiling ring (${sessionState.capturedFrames.size} of ${sessionState.targets.size} captured)"
                activeElevation < -12f ->
                    "Tilt DOWN to capture the lower wall & floor ring (${sessionState.capturedFrames.size} of ${sessionState.targets.size} captured)"
                else ->
                    "Pan smoothly along the horizon to the next blue dot (${sessionState.capturedFrames.size} of ${sessionState.targets.size} captured)"
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (sessionState.alignment.isWithinThreshold) {
                    Color(0xFF81C784).copy(alpha = 0.25f)
                } else {
                    Color.Black.copy(alpha = 0.7f)
                },
                border = BorderStroke(
                    1.dp,
                    if (sessionState.alignment.isWithinThreshold) {
                        Color(0xFF81C784)
                    } else {
                        Color.White.copy(alpha = 0.2f)
                    },
                ),
            ) {
                Text(
                    text = guidanceText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            // Focus & 3A lock status pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = if (isFocusLocked) Color(0xFF81C784) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = if (isFocusLocked) "Focus locked (tap to refocus)" else "Tap screen to focus",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                        )
                    }
                }

                if (isExposureLocked) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4FC3F7).copy(alpha = 0.16f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "AE/AWB Locked",
                                color = Color(0xFF4FC3F7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Bottom Action Row: Undo Last Shot + Finish & Stitch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { coordinator.undoLastCapture() },
                    enabled = sessionState.canUndo && !isStitching,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (sessionState.canUndo) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.35f),
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo Last Shot",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Undo", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        runStitchAndSave(sessionState.capturedFrames, sessionState.scope)
                    },
                    enabled = (sessionState.canFinishEarly || sessionState.isComplete) && !isStitching,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (sessionState.isComplete) Color(0xFF81C784) else Color(0xFF4FC3F7),
                        contentColor = Color(0xFF0A0D12),
                        disabledContainerColor = Color.White.copy(alpha = 0.14f),
                        disabledContentColor = Color.White.copy(alpha = 0.4f),
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val btnLabel = when {
                        sessionState.isComplete -> "Finish & Stitch 360°"
                        sessionState.canFinishEarly -> "Finish & Stitch (${sessionState.capturedFrames.size} shots)"
                        else -> "Capture at least ${SphereCaptureSessionState.MIN_FRAMES_TO_STITCH} horizon shots"
                    }
                    Text(
                        text = btnLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        // 7. Stitching Progress Modal Dialog
        if (isStitching) {
            StitchingProgressDialog(
                progress = stitchProgress,
                frameCount = sessionState.capturedFrames.size,
                roomTitle = roomTitle,
            )
        }
    }
}

@Composable
private fun HorizonCameraPreview(
    deviceProfile: SphereDeviceProfile,
    captureProfile: SphereCaptureProfile,
    onCameraBound: (Camera, PreviewView, ImageCapture, String?) -> Unit,
    onAeAwbConverged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCameraBound by rememberUpdatedState(onCameraBound)
    val currentOnConverged by rememberUpdatedState(onAeAwbConverged)

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    DisposableEffect(lifecycleOwner, deviceProfile, captureProfile) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val listener = Runnable {
            val provider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@Runnable
            val targetCameraId = captureBackCameraId(context, deviceProfile)

            val previewBuilder = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(android.view.Surface.ROTATION_0)
            val previewExtender = Camera2Interop.Extender(previewBuilder)
            applySphereCaptureOptions(previewExtender, captureProfile)

            var frameCounter = 0
            var converged = false
            previewExtender.setSessionCaptureCallback(
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult,
                    ) {
                        if (converged) return
                        frameCounter++
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        val awbState = result.get(CaptureResult.CONTROL_AWB_STATE)
                        val aeReady = aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED ||
                            aeState == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            aeState == CaptureResult.CONTROL_AE_STATE_LOCKED
                        val awbReady = awbState == null ||
                            awbState == CaptureResult.CONTROL_AWB_STATE_CONVERGED ||
                            awbState == CaptureResult.CONTROL_AWB_STATE_LOCKED
                        if ((aeReady && awbReady && frameCounter >= 12) || frameCounter >= 75) {
                            converged = true
                            mainExecutor.execute { currentOnConverged() }
                        }
                    }
                }
            )

            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(android.view.Surface.ROTATION_0)
                .setJpegQuality(95)
            val captureExtender = Camera2Interop.Extender(imageCaptureBuilder)
            applySphereCaptureOptions(captureExtender, captureProfile)
            applyStillImageOptions(captureExtender, captureProfile)
            val imageCapture = imageCaptureBuilder.build()

            val selector = if (targetCameraId != null) {
                CameraSelector.Builder()
                    .addCameraFilter { cameras ->
                        cameras.filter {
                            runCatching { Camera2CameraInfo.from(it).cameraId == targetCameraId }
                                .getOrDefault(false)
                        }.ifEmpty { cameras }
                    }
                    .build()
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            runCatching {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                )
                val boundId = runCatching { Camera2CameraInfo.from(camera.cameraInfo).cameraId }.getOrNull()
                currentOnCameraBound(camera, previewView, imageCapture, boundId)
            }.onFailure { err ->
                Log.w(TAG, "Preferred camera bind failed, falling back to DEFAULT_BACK_CAMERA", err)
                runCatching {
                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    val boundId = runCatching { Camera2CameraInfo.from(camera.cameraInfo).cameraId }.getOrNull()
                    currentOnCameraBound(camera, previewView, imageCapture, boundId)
                }
            }
        }

        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

@Composable
private fun StitchingProgressDialog(
    progress: Float,
    frameCount: Int,
    roomTitle: String,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF121722),
            border = BorderStroke(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = Color(0xFF4FC3F7),
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = "Stitching 360° Photo Sphere",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                val stageText = when {
                    progress < 0.20f -> "Decoding $frameCount high-res frames & locking lens intrinsics…"
                    progress < 0.35f -> "Balancing exposure & building Laplacian frequency pyramids…"
                    progress < 0.90f -> "Blending multi-band 360° × 180° equirectangular sphere…"
                    else -> "Sharpening room details & writing GPano 360° metadata for $roomTitle…"
                }
                Text(
                    text = stageText,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = Color(0xFF4FC3F7),
                    trackColor = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    text = "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
                    color = Color(0xFF4FC3F7),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredView(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = "Camera Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Dorja's Horizon 360° Room Scanner needs camera access to capture and stitch panoramic room views.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                ) {
                    Text("Back", color = Color.White)
                }
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4FC3F7),
                        contentColor = Color(0xFF0A0D12),
                    ),
                ) {
                    Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun findTappedTarget(
    tapOffset: Offset,
    viewportSize: IntSize,
    orientation: OrientationData,
    sessionState: SphereCaptureSessionState,
    fieldOfView: FieldOfView,
    hitRadiusPx: Float,
): SphereTarget? {
    if (!orientation.hasFix || viewportSize.width <= 0 || viewportSize.height <= 0) return null
    val focalPx = SphereProjection.focalLengthPx(
        viewportSize.width.toFloat(),
        viewportSize.height.toFloat(),
        fieldOfView,
    )
    if (focalPx <= 0f) return null
    val centerX = viewportSize.width / 2f
    val centerY = viewportSize.height / 2f

    var bestTarget: SphereTarget? = null
    var bestDistance = hitRadiusPx
    for (target in sessionState.targets) {
        if (sessionState.isCaptured(target)) continue
        val projected = SphereProjection.project(orientation, target)
        if (!projected.isInFront) continue
        val sx = centerX + (projected.x / projected.z) * focalPx
        val sy = centerY - (projected.y / projected.z) * focalPx
        val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
        if (dist <= bestDistance) {
            bestDistance = dist
            bestTarget = target
        }
    }
    return bestTarget
}

private fun buildPanoramaMetadataJson(
    stitchResult: com.example.scanner.photosphere.StitchResult,
    scope: SphereCaptureScope,
    roomTitle: String,
): String {
    val json = JSONObject()
    json.put("version", 3)
    json.put("type", "equirectangular_360")
    json.put("captureScope", if (scope == SphereCaptureScope.Ring) "horizon_ring" else "full_sphere")
    json.put("roomTitle", roomTitle)
    json.put("stitchedPanorama", stitchResult.outputFile.absolutePath)
    json.put("widthPx", stitchResult.widthPx)
    json.put("heightPx", stitchResult.heightPx)
    json.put("frameCount", stitchResult.frameCount)
    json.put("longitudeSpanDegrees", stitchResult.longitudeSpanDegrees.toDouble())
    json.put("latitudeSpanDegrees", stitchResult.latitudeSpanDegrees.toDouble())
    json.put("centerLongitudeDegrees", stitchResult.centerLongitudeDegrees.toDouble())
    json.put("centerLatitudeDegrees", stitchResult.centerLatitudeDegrees.toDouble())
    json.put("capturedAt", System.currentTimeMillis())
    val framesArray = JSONArray()
    framesArray.put(stitchResult.outputFile.absolutePath)
    json.put("frames", framesArray)
    return json.toString()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
