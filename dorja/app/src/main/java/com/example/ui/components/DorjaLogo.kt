package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Dorja wordmark. [outlined] draws a thin white stroke behind the original
 * artwork for use on dark surfaces (e.g. the green navigation drawer).
 */
@Composable
fun DorjaLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "Dorja Logo",
    outlined: Boolean = false,
    strokeWidth: Dp = 1.4.dp
) {
    val painter = painterResource(id = R.drawable.ic_dorja_logo)
    if (!outlined) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier
        )
        return
    }

    val offsets = listOf(
        -strokeWidth to 0.dp,
        strokeWidth to 0.dp,
        0.dp to -strokeWidth,
        0.dp to strokeWidth,
        -strokeWidth to -strokeWidth,
        -strokeWidth to strokeWidth,
        strokeWidth to -strokeWidth,
        strokeWidth to strokeWidth
    )
    Box(modifier = modifier) {
        offsets.forEach { (x, y) ->
            Image(
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.offset(x, y)
            )
        }
        Image(
            painter = painter,
            contentDescription = contentDescription
        )
    }
}
