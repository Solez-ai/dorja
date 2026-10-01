package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.slapps.cupertino.theme.CupertinoTheme
import com.slapps.cupertino.theme.lightColorScheme as cupertinoLightColorScheme

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

/**
 * The single application theme. DORJA is light-only: even when the Android
 * system is in dark mode the app renders with the light palette.
 */
@Composable
fun DorjaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    CompositionLocalProvider(
        LocalDorjaColors provides DorjaLightColors
    ) {
        CupertinoTheme(
            colorScheme = cupertinoLightColorScheme()
        ) {
            MaterialTheme(
                colorScheme = DorjaLightColorScheme,
                typography = DorjaTypography,
                content = content
            )
        }
    }
}
