package com.example.ui.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.scanner.CameraManager
import com.example.scanner.SensorFusionManager

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
    val captureState = vm.captureState.collectAsState()
    val currentZoom = vm.currentZoom.collectAsState()
    val alignLevel = vm.alignLevel.collectAsState()
    val frameCount = vm.frameCount.collectAsState()

    LaunchedEffect(sensorFusion) {
        sensorFusion?.start()
        sensorFusion?.onOrientationUpdate = { orientation, _ ->
            vm.updateOrientation(orientation)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CameraPreview(
                modifier = Modifier.weight(1f),
                cameraManager = cameraManager
            )

            ARGuidanceRing(alignLevel.value, modifier = Modifier.align(Alignment.CenterHorizontally))

            FramePreviewRail(emptyList())

            ZoomSlider(cameraManager) { zoom ->
                vm.updateZoom(zoom)
            }

            CaptureButton(
                currentFrame = frameCount.value,
                state = captureState.value,
                onCapture = {
                    vm.incrementFrameCount()
                    if (frameCount.value >= 12) {
                        onScanComplete(listingId, "scan_result")
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
