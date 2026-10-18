package com.example.scanner

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ScannerIntegrationTest {

    @Test
    fun testScannerWorkflow_FrameCapture() {
        val targets = ScanGeometry.generateScanTargets(12, 0f)
        assertEquals(12, targets.size)
        
        for (target in targets) {
            assertTrue(target.isValid())
        }
    }

    @Test
    fun testScannerWorkflow_PanoramaGeneration() {
        val (width, height) = ScanGeometry.calculatePanoramaDimensions(1920, 1440)
        assertTrue(ScanGeometry.isValidPanorama(width, height, 20_000_000L))
    }

    @Test
    fun testSensorFusionOrientationFlow() {
        val orientations = listOf(
            Orientation(0f, 0f, 0f),
            Orientation(30f, 10f, 5f),
            Orientation(60f, 20f, 10f)
        )
        
        for (orient in orientations) {
            assertTrue(orient.isValid())
        }
    }

    @Test
    fun testFrameData_Validation() {
        val frame = FrameData(
            path = "/cache/frame.jpg",
            heading = 90f,
            pitchDeg = 0f,
            row = 0,
            col = 0,
            isCap = false,
            capType = null
        )
        
        assertTrue(frame.isValid())
    }
}
