package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DorjaLightColorScheme = lightColorScheme(
    primary = DorjaLightColors.Jol600,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = DorjaLightColors.Teal100,
    onPrimaryContainer = DorjaLightColors.Teal900,
    secondary = DorjaLightColors.Ink950,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = DorjaLightColors.Sand100,
    onSecondaryContainer = DorjaLightColors.Ink950,
    tertiary = DorjaLightColors.Gray700,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = DorjaLightColors.Paper50,
    onBackground = DorjaLightColors.Ink950,
    surface = DorjaLightColors.White,
    onSurface = DorjaLightColors.Ink950,
    surfaceVariant = DorjaLightColors.Sand100,
    onSurfaceVariant = DorjaLightColors.Gray700,
    outline = DorjaLightColors.Sand300,
    outlineVariant = DorjaLightColors.Gray300,
    error = DorjaLightColors.Error,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = DorjaLightColors.ErrorContainer,
    onErrorContainer = DorjaLightColors.Error
)

private val DorjaDarkColorScheme = darkColorScheme(
    primary = DorjaDarkColors.Jol600,
    onPrimary = DorjaDarkColors.Ink950,
    primaryContainer = DorjaDarkColors.Jol100,
    onPrimaryContainer = DorjaDarkColors.Jol700,
    secondary = DorjaDarkColors.Sand300,
    onSecondary = DorjaDarkColors.Ink950,
    secondaryContainer = DorjaDarkColors.Sand100,
    onSecondaryContainer = DorjaDarkColors.Ink950,
    tertiary = DorjaDarkColors.Teal100,
    onTertiary = DorjaDarkColors.Ink950,
    background = DorjaDarkColors.CanvasBg,
    onBackground = DorjaDarkColors.Ink950,
    surface = DorjaDarkColors.White,
    onSurface = DorjaDarkColors.Ink950,
    surfaceVariant = DorjaDarkColors.Sand100,
    onSurfaceVariant = DorjaDarkColors.Gray700,
    outline = DorjaDarkColors.BentoCardBorder,
    outlineVariant = DorjaDarkColors.Gray300,
    error = DorjaDarkColors.Error,
    onError = DorjaDarkColors.CanvasBg,
    errorContainer = DorjaDarkColors.ErrorContainer,
    onErrorContainer = DorjaDarkColors.Error
)

@Composable
fun DorjaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DorjaDarkColors else DorjaLightColors
    val colorScheme = if (darkTheme) DorjaDarkColorScheme else DorjaLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDorjaColors provides tokens,
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DorjaTypography,
            content = content
        )
    }
}
