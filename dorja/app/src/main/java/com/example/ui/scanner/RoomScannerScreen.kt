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
    var motionAnalyzer by remember { mutableStateOf<MotionAnalyzer?>(null) }
    var sweepConfig by remember { mutableStateOf(SweepConfig(startHeading = 0f)) }

    LaunchedEffect(cameraManager, sensorFusion) {
        // Start sensor fusion
        sensorFusion?.start()
        
        // Initialize motion analyzer with config
        val initialHeading = orientation.value.headingDeg
        sweepConfig = SweepConfig(startHeading = initialHeading)
        motionAnalyzer = MotionAnalyzer(sweepConfig)
        
        sensorFusion?.onOrientationUpdate = { newOrientation, _ ->
            vm.updateOrientation(newOrientation)
            
            // Analyze motion
            motionAnalyzer?.let { analyzer ->
                val analysis = analyzer.analyzeMotion(newOrientation)
                vm.updateMotionVelocity(analysis.velocity)
                
                // Update sweep state based on motion
                when (sweepState.value) {
                    SweepCaptureState.READY_TO_START -> {
                        if (analysis.velocity > 5f) {
                            vm.updateSweepState(SweepCaptureState.SWEEPING_IN_PROGRESS)
                            // TODO: Start continuous capture once wired
                        }
                    }
                    SweepCaptureState.SWEEPING_IN_PROGRESS -> {
                        // Update progress
                        val progress = sweepConfig.calculateProgress(newOrientation.headingDeg)
                        vm.updateSweepProgress(progress)
                        
                        // Pause if motion is too erratic
                        if (analysis.state == MotionState.JERKY || 
                            analysis.state == MotionState.STATIONARY) {
                            vm.pauseSweep()
                        }
                        
                        // Complete if reached target angle
                        if (progress >= 1f) {
                            vm.completeSweep()
                        }
                    }
                    SweepCaptureState.PAUSED -> {
                        if (analysis.state == MotionState.GOOD) {
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
    }

    DisposableEffect(Unit) {
        onDispose {
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
