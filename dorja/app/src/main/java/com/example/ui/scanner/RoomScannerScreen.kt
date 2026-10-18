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
    viewModel: ScannerViewModel,
    cameraManager: CameraManager?,
    sensorFusion: SensorFusionManager?,
    onComplete: () -> Unit = {}
) {
    val captureState = viewModel.captureState.collectAsState()
    val currentZoom = viewModel.currentZoom.collectAsState()
    val alignLevel = viewModel.alignLevel.collectAsState()
    val frameCount = viewModel.frameCount.collectAsState()

    LaunchedEffect(sensorFusion) {
        sensorFusion?.start()
        sensorFusion?.onOrientationUpdate = { orientation, _ ->
            viewModel.updateOrientation(orientation)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CameraPreview(cameraManager, modifier = Modifier.weight(1f))

            ARGuidanceRing(alignLevel.value, modifier = Modifier.align(Alignment.CenterHorizontally))

            FramePreviewRail(emptyList())

            ZoomSlider(cameraManager) { zoom ->
                viewModel.updateZoom(zoom)
            }

            CaptureButton(
                currentFrame = frameCount.value,
                state = captureState.value,
                onCapture = {
                    viewModel.incrementFrameCount()
                    if (frameCount.value >= 12) {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
