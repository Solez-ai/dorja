package com.example.ui.scanner

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.CameraManager
import com.example.scanner.ZoomCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ZoomSlider Composable using Material 3 Slider.
 *
 * Binds slider to [CameraManager.getZoomCapabilities()] to set min/max values.
 * Provides real-time zoom updates, visual feedback, and accessibility support.
 *
 * Features:
 * - Real-time zoom updates: On slider change, calls [cameraManager.setZoomRatio()]
 * - Visual feedback: Displays current zoom ratio (e.g., "1.0x") above slider
 * - Snap-to-nearest: If user tries value outside range, displays toast notification
 * - Fixed zoom handling: Devices with min=max → disables slider, shows "Fixed zoom" label
 * - Accessibility: contentDescription with current zoom value, step size, min/max range
 * - Smooth transitions and responsive UI
 *
 * Related Requirements: 1.2, 1.3, 4.2, 4.5
 * Target Latency: <100ms between slider change and preview update
 *
 * @param cameraManager The CameraManager instance managing camera zoom
 * @param modifier Composable modifier
 * @param onZoomChanged Optional callback when zoom value changes
 */
@Composable
fun ZoomSlider(
    cameraManager: CameraManager?,
    modifier: Modifier = Modifier,
    onZoomChanged: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    var zoomCapabilities by remember { mutableFloatStateOf(1.0f) }
    var currentZoom by remember { mutableFloatStateOf(1.0f) }
    var minZoom by remember { mutableFloatStateOf(1.0f) }
    var maxZoom by remember { mutableFloatStateOf(2.0f) }
    var isLoading by remember { mutableFloatStateOf(true) }
    var isFixedZoom by remember { mutableFloatStateOf(false) }
    var errorMessage by remember { mutableFloatStateOf<String?>(null) }

    // Initialize zoom capabilities on first render
    LaunchedEffect(cameraManager) {
        if (cameraManager == null) {
            isLoading = false
            return@LaunchedEffect
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val capabilities = cameraManager.detectZoomCapabilities()
                if (capabilities.isValid()) {
                    minZoom = capabilities.minZoomRatio
                    maxZoom = capabilities.maxZoomRatio
                    currentZoom = capabilities.defaultZoom
                    isFixedZoom = capabilities.minZoomRatio >= capabilities.maxZoomRatio
                    zoomCapabilities = capabilities.minZoomRatio
                } else {
                    errorMessage = "Invalid zoom capabilities detected"
                }
            } catch (e: Exception) {
                errorMessage = "Failed to initialize zoom: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Show error toast if any
    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Render zoom slider UI
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header with label and current zoom display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Zoom",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp
            )

            // Current zoom ratio display
            Text(
                text =
                    if (isFixedZoom) "Fixed zoom" else String.format("%.1fx", currentZoom),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            // Loading state
            Text(
                text = "Loading zoom settings…",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.height(24.dp)
            )
        } else if (isFixedZoom) {
            // Fixed zoom state (min ≈ max)
            Text(
                text = "Fixed zoom (${String.format("%.1fx", currentZoom)})",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .height(24.dp)
                    .semantics {
                        contentDescription =
                            "Zoom is fixed at ${String.format("%.1fx", currentZoom)} for this device"
                    }
            )
        } else {
            // Active slider
            Slider(
                value = currentZoom,
                onValueChange = { newValue ->
                    // Snap to nearest supported value (round to 0.1x)
                    val snappedValue = (newValue * 10).toInt().toFloat() / 10
                    val clampedValue = snappedValue.coerceIn(minZoom, maxZoom)

                    if (clampedValue != currentZoom) {
                        currentZoom = clampedValue
                        onZoomChanged(clampedValue)

                        // Apply zoom to camera in real-time
                        cameraManager?.setZoomRatio(clampedValue)

                        // Snap notification if outside range
                        if (clampedValue != newValue) {
                            Toast.makeText(
                                context,
                                "Using maximum zoom for this device",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                valueRange = minZoom..maxZoom,
                steps =
                    ((maxZoom - minZoom) * 10).toInt(), // 0.1x step increments
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Zoom slider, current value ${String.format("%.1fx", currentZoom)}, " +
                                    "range ${String.format("%.1fx", minZoom)} to ${String.format("%.1fx", maxZoom)}, " +
                                    "step size 0.1x. Adjust slider left to zoom out, right to zoom in"
                        },
                colors = SliderDefaults.colors(),
                thumb = {
                    // Use Material 3 default slider thumb
                },
                track = {
                    // Use Material 3 default slider track
                }
            )

            // Min/max zoom labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1fx", minZoom),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format("%.1fx", maxZoom),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
