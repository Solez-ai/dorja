package com.example.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.scanner.*
import kotlinx.coroutines.launch

@Composable
fun RoomScannerScreen(
    listingId: String,
    onBack: () -> Unit = {},
    onScanComplete: (String, String) -> Unit = { _, _ -> },
    viewModel: ScannerViewModel? = null,
    cameraManager: CameraManager? = null,
    sensorFusion: SensorFusionManager? = null
) {
    val vm = viewModel ?: ScannerViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // NEW: Sweep panorama state
    val sweepState = vm.sweepState.collectAsState()
    val sweepProgress = vm.sweepProgress.collectAsState()
    val sweepDirection = vm.sweepDirection.collectAsState()
    val motionVelocity = vm.motionVelocity.collectAsState()
    val stripsCaptured = vm.stripsCaptured.collectAsState()
    val orientation = vm.orientation.collectAsState()
    val currentZoom = vm.currentZoom.collectAsState()

    // Initialize managers
    var continuousCaptureManager by remember { mutableStateOf<ContinuousCaptureManager?>(null) }
    var motionAnalyzer by remember { mutableStateOf<MotionAnalyzer?>(null) }
    var sweepConfig by remember { mutableStateOf(SweepConfig()) }

    LaunchedEffect(cameraManager, sensorFusion) {
        // Start sensor fusion
        sensorFusion?.start()
        sensorFusion?.onOrientationUpdate = { newOrientation, _ ->
            vm.updateOrientation(newOrientation)
            
            // Analyze motion
            motionAnalyzer?.let { analyzer ->
                val analysis = analyzer.analyzeMotion(newOrientation)
                vm.updateMotionVelocity(analysis.angularVelocity)
                
                // Update sweep state based on motion
                when (sweepState.value) {
                    SweepCaptureState.READY_TO_START -> {
                        if (analysis.angularVelocity > 5f) {
                            vm.updateSweepState(SweepCaptureState.SWEEPING_IN_PROGRESS)
                            continuousCaptureManager?.startContinuousCapture()
                        }
                    }
                    SweepCaptureState.SWEEPING_IN_PROGRESS -> {
                        // Update progress
                        val progress = sweepConfig.calculateProgress(newOrientation.heading)
                        vm.updateSweepProgress(progress)
                        
                        // Pause if motion is too erratic
                        if (analysis.motionState == MotionState.ERRATIC || 
                            analysis.motionState == MotionState.STATIONARY) {
                            vm.pauseSweep()
                        }
                        
                        // Complete if reached target angle
                        if (progress >= 1f) {
                            vm.completeSweep()
                            continuousCaptureManager?.stopCapture()
                        }
                    }
                    SweepCaptureState.PAUSED -> {
                        if (analysis.motionState == MotionState.SMOOTH) {
                            vm.resumeSweep()
                        }
                    }
                    SweepCaptureState.SWEEP_COMPLETE -> {
                        vm.updateSweepState(SweepCaptureState.STITCHING)
                        scope.launch {
                            // TODO: Trigger stitching
                            kotlinx.coroutines.delay(2000) // Simulate stitching
                            vm.updateSweepState(SweepCaptureState.COMPLETED)
                            onScanComplete(listingId, "panorama_result")
                        }
                    }
                    else -> { /* No action needed */ }
                }
            }
        }

        // Initialize motion analyzer
        motionAnalyzer = MotionAnalyzer()

        // Initialize continuous capture manager
        cameraManager?.let { cam ->
            val config = SweepConfig(
                sweepAngle = 120f, // Default 120° sweep
                direction = SweepDirection.LEFT_TO_RIGHT
            )
            sweepConfig = config
            vm.updateSweepDirection(config.direction)
            
            continuousCaptureManager = ContinuousCaptureManager(
                context = context,
                cameraManager = cam,
                config = config,
                onStripCaptured = { strip ->
                    vm.incrementStripsCaptured()
                },
                onError = { error ->
                    vm.setError(error)
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            continuousCaptureManager?.stopCapture()
            sensorFusion?.stop()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Camera Preview (full screen)
            CameraPreview(
                modifier = Modifier.weight(1f),
                cameraManager = cameraManager
            )

            // Overlay guidance UI
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // NEW: Sweep guidance instead of circular ring
                SweepGuidance(
                    state = sweepState.value,
                    direction = sweepDirection.value,
                    progress = sweepProgress.value,
                    velocity = motionVelocity.value,
                    modifier = Modifier
                        .padding(32.dp)
                        .align(Alignment.TopCenter)
                )
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Zoom slider
                ZoomSlider(cameraManager) { zoom ->
                    vm.updateZoom(zoom)
                }

                // Debug info: strips captured
                if (sweepState.value == SweepCaptureState.SWEEPING_IN_PROGRESS) {
                    androidx.compose.material3.Text(
                        text = "Strips captured: ${stripsCaptured.value}",
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}
