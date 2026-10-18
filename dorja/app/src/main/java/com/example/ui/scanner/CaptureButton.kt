package com.example.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.CaptureState

@Composable
fun CaptureButton(
    currentFrame: Int = 0,
    totalFrames: Int = 12,
    state: CaptureState = CaptureState.WAITING_FOR_INITIAL_ANCHOR,
    onCapture: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isEnabled = state == CaptureState.WAITING_FOR_INITIAL_ANCHOR ||
            state == CaptureState.STABILIZING ||
            state == CaptureState.MOVING_TO_TARGET

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .background(
                color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(enabled = isEnabled) { onCapture() }
            .semantics {
                contentDescription =
                    "Capture button: Frame $currentFrame of $totalFrames. $${state.name}"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$currentFrame/$totalFrames",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
