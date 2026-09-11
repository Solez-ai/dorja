package com.example.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DorjaThemePaletteTest {

    @Test
    fun lightCanvasStaysBrightByDefault() {
        assertTrue(DorjaLightColors.CanvasBg.red > 0.9f)
        assertTrue(DorjaLightColors.Ink950.red < 0.2f)
    }

    @Test
    fun darkPaletteKeepsHighContrastTextOnCanvas() {
        assertTrue(DorjaDarkColors.CanvasBg.red < 0.15f)
        assertTrue(DorjaDarkColors.Ink950.red > 0.85f)
        assertTrue(DorjaDarkColors.White.red < 0.2f)
        assertTrue(DorjaDarkColors.InverseFg.red > 0.9f)
    }

    @Test
    fun darkModeStateDefaultsToOff() {
        assertEquals(false, ThemeSettings.darkMode.value)
    }
}
