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
    fun darkModeIsRemovedAndDefaultsToLight() {
        // Light-only application: no dark palette exists, and the composition
        // local defaults to the light token set.
        assertEquals(DorjaLightColors, LocalDorjaColors.defaultValue)
    }
}
