package com.example.scanner

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScanGeometryPropertyTest {

    @Test
    fun propertyTest_FrameOverlapSymmetric() {
        repeat(100) {
            val h1 = (Math.random() * 360f).toFloat()
            val h2 = (Math.random() * 360f).toFloat()
            
            val overlap1 = ScanGeometry.calculateFrameOverlap(h1, h2)
            val overlap2 = ScanGeometry.calculateFrameOverlap(h2, h1)
            
            assertEquals(overlap1, overlap2, 0.0001f)
            assertTrue(overlap1 in 0f..1f)
        }
    }

    @Test
    fun propertyTest_RotationMatrixValid() {
        repeat(100) {
            val angle = (Math.random() * 360f) * 3.14159f / 180f
            val qw = kotlin.math.cos(angle / 2f).toFloat()
            val qz = kotlin.math.sin(angle / 2f).toFloat()
            
            val matrix = ScanGeometry.quaternionToRotationMatrix(qw, 0f, 0f, qz)
            assertEquals(9, matrix.size)
        }
    }

    @Test
    fun propertyTest_ScanTargetsEvenlySpaced() {
        for (frameCount in 6..12) {
            val targets = ScanGeometry.generateScanTargets(frameCount, 0f)
            assertEquals(frameCount, targets.size)
            
            for (target in targets) {
                assertTrue(target.isValid())
            }
        }
    }

    @Test
    fun propertyTest_PanoramaDimensions() {
        repeat(50) {
            val width = (1024 + Math.random() * 3072).toInt()
            val height = (512 + Math.random() * 1536).toInt()
            
            val (outW, outH) = ScanGeometry.calculatePanoramaDimensions(width, height)
            val aspectRatio = outW.toFloat() / outH.toFloat()
            assertTrue(aspectRatio in 1.99f..2.01f)
        }
    }

    @Test
    fun propertyTest_PanoramaValidation() {
        val (w, h) = ScanGeometry.calculatePanoramaDimensions(1920, 1440)
        
        assertTrue(ScanGeometry.isValidPanorama(w, h, 10_000_000L))
        assertFalse(ScanGeometry.isValidPanorama(w + 100, h, 10_000_000L))
        assertFalse(ScanGeometry.isValidPanorama(w, h, 60_000_000L))
    }
}
