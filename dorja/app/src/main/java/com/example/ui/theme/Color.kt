package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic Dorja palette. Light is the default; [DorjaDarkColors] is swapped
 * in through [LocalDorjaColors] when the user enables Dark Mode in Settings.
 */
data class DorjaColorTokens(
    val CanvasBg: Color,
    val Ink950: Color,
    val Jol600: Color,
    val Jol100: Color,
    val Jol700: Color,
    val Paper50: Color,
    val Sand300: Color,
    val Sand100: Color,
    val Teal100: Color,
    val Teal900: Color,
    val Gray700: Color,
    val Gray600: Color,
    val Gray500: Color,
    val Gray300: Color,
    val White: Color,
    val InverseBg: Color,
    val InverseFg: Color,
    val Error: Color,
    val ErrorContainer: Color,
    val Success: Color,
    val Warning: Color,
    val WarningContainer: Color,
    val BentoBlueBg: Color,
    val BentoBlueText: Color,
    val BentoBlueIcon: Color,
    val BentoGreenBg: Color,
    val BentoGreenText: Color,
    val BentoGreenIcon: Color,
    val BentoAmberBg: Color,
    val BentoAmberText: Color,
    val BentoAmberIcon: Color,
    val BentoPurpleBg: Color,
    val BentoPurpleText: Color,
    val BentoPurpleIcon: Color,
    val BentoCardBg: Color,
    val BentoCardBorder: Color,
    val DrawerBackdrop: Color,
    val DrawerSidebar: Color,
    val DrawerSidebarSoft: Color,
    val DrawerCream: Color,
    val DrawerMuted: Color,
    val DrawerAccent: Color
)

val DorjaLightColors = DorjaColorTokens(
    CanvasBg = Color(0xFFFDFBFF),
    Ink950 = Color(0xFF1A1C1E),
    Jol600 = Color(0xFF0061A4),
    Jol100 = Color(0xFFD6E3FF),
    Jol700 = Color(0xFF00487B),
    Paper50 = Color(0xFFF8F9FE),
    Sand300 = Color(0xFFE1E2EC),
    Sand100 = Color(0xFFF0F4FA),
    Teal100 = Color(0xFFD6E3FF),
    Teal900 = Color(0xFF001B3E),
    Gray700 = Color(0xFF44474E),
    Gray600 = Color(0xFF5E6066),
    Gray500 = Color(0xFF74777F),
    Gray300 = Color(0xFFE1E2EC),
    White = Color(0xFFFFFFFF),
    InverseBg = Color(0xFF1A1C1E),
    InverseFg = Color(0xFFFFFFFF),
    Error = Color(0xFFBA1A1A),
    ErrorContainer = Color(0xFFFFDAD6),
    Success = Color(0xFF386B3B),
    Warning = Color(0xFF825500),
    WarningContainer = Color(0xFFFFDDB3),
    BentoBlueBg = Color(0xFFD6E3FF),
    BentoBlueText = Color(0xFF001B3E),
    BentoBlueIcon = Color(0xFF0061A4),
    BentoGreenBg = Color(0xFFE2F1E3),
    BentoGreenText = Color(0xFF111F12),
    BentoGreenIcon = Color(0xFF386B3B),
    BentoAmberBg = Color(0xFFFFF1D6),
    BentoAmberText = Color(0xFF291800),
    BentoAmberIcon = Color(0xFF825500),
    BentoPurpleBg = Color(0xFFF3E7FF),
    BentoPurpleText = Color(0xFF231049),
    BentoPurpleIcon = Color(0xFF6B4EA2),
    BentoCardBg = Color(0xFFFFFFFF),
    BentoCardBorder = Color(0xFFE1E2EC),
    DrawerBackdrop = Color(0xFF0D1F17),
    DrawerSidebar = Color(0xFF122B1F),
    DrawerSidebarSoft = Color(0x33FFFFFF),
    DrawerCream = Color(0xFFF2EFE4),
    DrawerMuted = Color(0xFF8FA89A),
    DrawerAccent = Color(0xFF9BD4A8)
)

val DorjaDarkColors = DorjaColorTokens(
    CanvasBg = Color(0xFF0F1419),
    Ink950 = Color(0xFFF2F4F7),
    Jol600 = Color(0xFF5BA8E8),
    Jol100 = Color(0xFF16324C),
    Jol700 = Color(0xFF9CCCF0),
    Paper50 = Color(0xFF151B22),
    Sand300 = Color(0xFF2C3542),
    Sand100 = Color(0xFF1A222C),
    Teal100 = Color(0xFF17324A),
    Teal900 = Color(0xFFD4E6FF),
    Gray700 = Color(0xFFC8CDD6),
    Gray600 = Color(0xFFA8B0BC),
    Gray500 = Color(0xFF8B93A0),
    Gray300 = Color(0xFF2C3542),
    White = Color(0xFF1C2430),
    InverseBg = Color(0xFF080A0E),
    InverseFg = Color(0xFFF5F7FA),
    Error = Color(0xFFFFB4AB),
    ErrorContainer = Color(0xFF5C1A1A),
    Success = Color(0xFF7DCA82),
    Warning = Color(0xFFE8A84A),
    WarningContainer = Color(0xFF33240E),
    BentoBlueBg = Color(0xFF17324A),
    BentoBlueText = Color(0xFFD4E6FF),
    BentoBlueIcon = Color(0xFF7EB8F0),
    BentoGreenBg = Color(0xFF17301C),
    BentoGreenText = Color(0xFFC5E8C8),
    BentoGreenIcon = Color(0xFF7DCA82),
    BentoAmberBg = Color(0xFF33240E),
    BentoAmberText = Color(0xFFFFD9A0),
    BentoAmberIcon = Color(0xFFE8A84A),
    BentoPurpleBg = Color(0xFF2A1F3D),
    BentoPurpleText = Color(0xFFE4D0FF),
    BentoPurpleIcon = Color(0xFFC4A0F0),
    BentoCardBg = Color(0xFF1C2430),
    BentoCardBorder = Color(0xFF2C3542),
    DrawerBackdrop = Color(0xFF070E0B),
    DrawerSidebar = Color(0xFF0E1F18),
    DrawerSidebarSoft = Color(0x33FFFFFF),
    DrawerCream = Color(0xFFF2EFE4),
    DrawerMuted = Color(0xFF8FA89A),
    DrawerAccent = Color(0xFF9BD4A8)
)

val LocalDorjaColors = staticCompositionLocalOf { DorjaLightColors }
val LocalDarkTheme = staticCompositionLocalOf { false }

/**
 * Theme-aware color access used by every screen. Reads [LocalDorjaColors],
 * so existing `DorjaColors.CanvasBg` call sites pick up Dark Mode automatically.
 */
object DorjaColors {
    val CanvasBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.CanvasBg
    val Ink950: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Ink950
    val Jol600: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Jol600
    val Jol100: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Jol100
    val Jol700: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Jol700
    val Paper50: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Paper50
    val Sand300: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Sand300
    val Sand100: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Sand100
    val Teal100: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Teal100
    val Teal900: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Teal900
    val Gray700: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Gray700
    val Gray600: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Gray600
    val Gray500: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Gray500
    val Gray300: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Gray300
    val White: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.White
    val InverseBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.InverseBg
    val InverseFg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.InverseFg
    val Error: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Error
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.ErrorContainer
    val Success: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Success
    val Warning: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.Warning
    val WarningContainer: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.WarningContainer
    val BentoBlueBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoBlueBg
    val BentoBlueText: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoBlueText
    val BentoBlueIcon: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoBlueIcon
    val BentoGreenBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoGreenBg
    val BentoGreenText: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoGreenText
    val BentoGreenIcon: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoGreenIcon
    val BentoAmberBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoAmberBg
    val BentoAmberText: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoAmberText
    val BentoAmberIcon: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoAmberIcon
    val BentoPurpleBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoPurpleBg
    val BentoPurpleText: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoPurpleText
    val BentoPurpleIcon: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoPurpleIcon
    val BentoCardBg: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoCardBg
    val BentoCardBorder: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.BentoCardBorder
    val DrawerBackdrop: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.DrawerBackdrop
    val DrawerSidebar: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.DrawerSidebar
    val DrawerSidebarSoft: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.DrawerSidebarSoft
    val DrawerCream: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.DrawerCream
    val DrawerMuted: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.DrawerMuted
    val DrawerAccent: Color @Composable @ReadOnlyComposable get() = LocalDorjaColors.current.DrawerAccent
}
