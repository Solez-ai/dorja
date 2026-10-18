package com.example.ui.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.scanner.AlignLevel
import com.example.scanner.Orientation

@Composable
fun ARGuidanceRing(
    alignLevel: AlignLevel = AlignLevel.RED,
    orientation: Orientation? = null,
    modifier: Modifier = Modifier
) {
    val ringColor by animateColorAsState(
        targetValue = when (alignLevel) {
            AlignLevel.RED -> Color(0xFFD32F2F)
            AlignLevel.YELLOW -> Color(0xFFFBC02D)
            AlignLevel.GREEN -> Color(0xFF388E3C)
        },
        label = "ringColor"
    )

    val contentDesc = when (alignLevel) {
        AlignLevel.RED -> "Alignment ring: Not aligned"
        AlignLevel.YELLOW -> "Alignment ring: Partially aligned"
        AlignLevel.GREEN -> "Alignment ring: Aligned, ready to capture"
    }

    Box(
        modifier = modifier
            .size(280.dp)
            .semantics {
                contentDescription = contentDesc
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val center = center
            val radius = size.minDimension / 2f

            drawCircle(
                color = ringColor,
                radius = radius,
                center = center,
                style = Stroke(width = 8f)
            )

            if (alignLevel == AlignLevel.GREEN) {
                drawCircle(
                    color = ringColor.copy(alpha = 0.3f),
                    radius = radius * 0.8f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
