package com.example.ui.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.SweepCaptureState
import com.example.scanner.SweepDirection

/**
 * Samsung/Apple-style sweep guidance UI for horizontal panorama capture.
 * Displays directional arrow, progress bar, and sweep instructions.
 */
@Composable
fun SweepGuidance(
    state: SweepCaptureState,
    direction: SweepDirection,
    progress: Float,
    velocity: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "sweepProgress"
    )

    val instructionText = when (state) {
        SweepCaptureState.READY_TO_START -> when (direction) {
            SweepDirection.LEFT_TO_RIGHT -> "Move phone slowly to the right →"
            SweepDirection.RIGHT_TO_LEFT -> "Move phone slowly to the left ←"
        }
        SweepCaptureState.SWEEPING_IN_PROGRESS -> {
            when {
                velocity < 5f -> "Too slow! Move steadily"
                velocity > 30f -> "Too fast! Slow down"
                else -> "Keep moving smoothly"
            }
        }
        SweepCaptureState.PAUSED -> "Paused - continue panning"
        SweepCaptureState.SWEEP_COMPLETE -> "Sweep complete! Processing..."
        SweepCaptureState.STITCHING -> "Stitching panorama..."
        SweepCaptureState.COMPLETED -> "Panorama complete!"
        SweepCaptureState.ERROR -> "Error - tap to retry"
    }

    val progressColor by animateColorAsState(
        targetValue = when {
            state == SweepCaptureState.ERROR -> Color(0xFFD32F2F)
            velocity > 30f -> Color(0xFFFBC02D) // Too fast
            velocity < 5f && state == SweepCaptureState.SWEEPING_IN_PROGRESS -> Color(0xFFFBC02D) // Too slow
            state == SweepCaptureState.SWEEPING_IN_PROGRESS -> Color(0xFF388E3C) // Good
            else -> Color(0xFF2196F3) // Ready/Processing
        },
        label = "progressColor"
    )

    val contentDesc = buildString {
        append("Panorama sweep guidance. ")
        append("State: ${state.name}. ")
        append("Progress: ${(progress * 100).toInt()} percent. ")
        append(instructionText)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = contentDesc
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Directional Arrow
        if (state == SweepCaptureState.READY_TO_START || 
            state == SweepCaptureState.SWEEPING_IN_PROGRESS) {
            Icon(
                imageVector = when (direction) {
                    SweepDirection.LEFT_TO_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                    SweepDirection.RIGHT_TO_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                },
                contentDescription = null,
                tint = progressColor,
                modifier = Modifier.size(64.dp)
            )
        }

        // Instruction Text
        Text(
            text = instructionText,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = Color.White
        )

        // Progress Bar
        SweepProgressBar(
            progress = animatedProgress,
            color = progressColor,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
        )

        // Progress Percentage
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            color = Color.White
        )

        // Preview Strip (visual feedback of captured strips)
        if (state == SweepCaptureState.SWEEPING_IN_PROGRESS) {
            PreviewStripIndicator(
                progress = animatedProgress,
                direction = direction,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(40.dp)
            )
        }
    }
}

/**
 * Horizontal progress bar showing sweep completion.
 */
@Composable
private fun SweepProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        
        // Background track
        drawRoundRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius
        )

        // Progress fill
        val progressWidth = size.width * progress.coerceIn(0f, 1f)
        if (progressWidth > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(progressWidth, size.height),
                cornerRadius = cornerRadius
            )
        }

        // Border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius,
            style = Stroke(width = 2f)
        )
    }
}

/**
 * Visual representation of captured panorama strips.
 * Shows accumulated strips as a mini preview.
 */
@Composable
private fun PreviewStripIndicator(
    progress: Float,
    direction: SweepDirection,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stripCount = 20 // Visual segments
            val stripWidth = size.width / stripCount
            val filledCount = (stripCount * progress).toInt()

            for (i in 0 until filledCount) {
                val alpha = 0.3f + (i.toFloat() / stripCount) * 0.7f
                val x = when (direction) {
                    SweepDirection.LEFT_TO_RIGHT -> i * stripWidth
                    SweepDirection.RIGHT_TO_LEFT -> size.width - (i + 1) * stripWidth
                }
                
                drawRect(
                    color = Color(0xFF4CAF50).copy(alpha = alpha),
                    topLeft = Offset(x, 0f),
                    size = Size(stripWidth - 2f, size.height)
                )
            }
        }
    }
}
