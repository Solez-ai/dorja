package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DorjaLightColorScheme = lightColorScheme(
    primary = DorjaColors.Jol600,
    onPrimary = DorjaColors.White,
    primaryContainer = DorjaColors.Teal100,
    onPrimaryContainer = DorjaColors.Teal900,
    secondary = DorjaColors.Ink950,
    onSecondary = DorjaColors.White,
    secondaryContainer = DorjaColors.Sand100,
    onSecondaryContainer = DorjaColors.Ink950,
    tertiary = DorjaColors.Gray700,
    onTertiary = DorjaColors.White,
    background = DorjaColors.Paper50,
    onBackground = DorjaColors.Ink950,
    surface = DorjaColors.White,
    onSurface = DorjaColors.Ink950,
    surfaceVariant = DorjaColors.Sand100,
    onSurfaceVariant = DorjaColors.Gray700,
    outline = DorjaColors.Sand300,
    outlineVariant = DorjaColors.Gray300,
    error = DorjaColors.Error,
    onError = DorjaColors.White,
    errorContainer = DorjaColors.ErrorContainer,
    onErrorContainer = DorjaColors.Error
)

private val DorjaDarkColorScheme = darkColorScheme(
    primary = DorjaColors.Jol600,
    onPrimary = DorjaColors.White,
    primaryContainer = DorjaColors.Teal900,
    onPrimaryContainer = DorjaColors.Teal100,
    secondary = DorjaColors.Sand300,
    onSecondary = DorjaColors.Ink950,
    secondaryContainer = DorjaColors.Gray700,
    onSecondaryContainer = DorjaColors.Paper50,
    tertiary = DorjaColors.Teal100,
    onTertiary = DorjaColors.Ink950,
    background = DorjaColors.Ink950,
    onBackground = DorjaColors.Paper50,
    surface = DorjaColors.Gray700,
    onSurface = DorjaColors.Paper50,
    surfaceVariant = DorjaColors.Ink950,
    onSurfaceVariant = DorjaColors.Sand300,
    outline = DorjaColors.Gray500,
    outlineVariant = DorjaColors.Gray700,
    error = DorjaColors.Error,
    onError = DorjaColors.White
)

@Composable
fun DorjaTheme(
    darkTheme: Boolean = false, // Default to clean architectural light theme for maximum readability
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DorjaDarkColorScheme else DorjaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = DorjaTypography,
        content = content
    )
}
