package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.pressScale

/**
 * Simple mic button used in the AI bottom sheet.
 *
 * @param isListening whether the mic is currently active.
 * @param onToggle callback invoked when the button is pressed.
 */
@Composable
fun MicButton(
    isListening: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val image: ImageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier.pressScale(onClick = onToggle)
    ) {
        Icon(
            imageVector = image,
            contentDescription = if (isListening) "Stop listening" else "Start listening",
            tint = if (isListening) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.Black
        )
    }
}
