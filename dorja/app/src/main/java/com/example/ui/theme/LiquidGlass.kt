package com.example.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Authentic Apple Liquid Glass Engine for Android.
 *
 * Glass here is produced the way real glass panels look: a translucent
 * vertical-gradient tint, a specular refraction border stroke (bright top,
 * dark bottom edge) and a soft drop shadow. We deliberately do NOT apply a
 * RenderEffect blur to the element itself -- blurring the node would smudge
 * its own text/icons into an unreadable rectangle (the "blurry box" bug).
 */
object LiquidGlassDefaults {
    val LightGlassTint = Color(0xCCF4F7FB)        // Translucent frost white
    val DarkGlassTint = Color(0xCC1A1C1E)         // Translucent dark slate
    val StainedGlassTint = Color(0xD90061A4)      // Stained Jol600 brand accent for primary CTAs
    val SpecularHighlightTop = Color(0x66FFFFFF)   // Top specular lens stroke
    val SpecularHighlightBottom = Color(0x14000000)// Bottom subtle shadow stroke

    // Blur radius constants (kept for API compatibility; glass no longer self-blurs)
    val BlurSmall = 8.dp
    val BlurMedium = 16.dp
    val BlurLarge = 24.dp

    val SpecularBorderBrush = Brush.verticalGradient(
        colors = listOf(SpecularHighlightTop, Color(0x1FFFFFFF), SpecularHighlightBottom)
    )
}

/**
 * Applies a Liquid Glass surface: translucent gradient tint, specular
 * refraction border and a soft rounded-corner shadow.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = LiquidGlassDefaults.LightGlassTint,
    blurRadius: Dp = 24.dp,
    glassColor: Color? = null,
    specularColor: Color? = null,
    showSpecularBorder: Boolean = true,
    borderWidth: Dp = 1.dp
): Modifier {
    val finalTint = glassColor ?: tint
    val specTop = specularColor ?: LiquidGlassDefaults.SpecularHighlightTop

    val specularBrush = Brush.verticalGradient(
        colors = listOf(specTop, Color(0x1FFFFFFF), LiquidGlassDefaults.SpecularHighlightBottom)
    )
    val tintBrush = Brush.verticalGradient(
        colors = listOf(
            lerp(finalTint, Color.White, 0.07f),
            finalTint,
            lerp(finalTint, Color.Black, 0.08f)
        )
    )

    return this
        .graphicsLayer {
            this.shape = shape
            clip = true
            shadowElevation = 6f
        }
        .background(tintBrush, shape)
        .then(
            if (showSpecularBorder) {
                Modifier.border(
                    border = BorderStroke(borderWidth, specularBrush),
                    shape = shape
                )
            } else Modifier
        )
}

/**
 * Tactile spring press scale animation modifier for touch feedback.
 * Scales down to 0.97f on press with bouncy spring response and haptic bump.
 */
@Composable
fun Modifier.pressScale(
    enabled: Boolean = true,
    hapticBump: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    if (!enabled || onClick == null) return this

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(false)
                    if (hapticBump) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    scope.launch {
                        scale.animateTo(
                            0.97f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        )
                    }
                    val up = waitForUpOrCancellation()
                    scope.launch {
                        scale.animateTo(
                            1f,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMedium,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            )
                        )
                    }
                    if (up != null) {
                        onClick()
                    }
                }
            }
        }
}

/**
 * Functional Layer Liquid Glass Container.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = if (LocalDarkTheme.current) LiquidGlassDefaults.DarkGlassTint else LiquidGlassDefaults.LightGlassTint,
    blurRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(shape = shape, tint = tint, blurRadius = blurRadius),
        content = content
    )
}
