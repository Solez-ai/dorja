package com.example.scanner

import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for ScanGeometry calculations.
 * Tests rotation matrix math, frame overlap detection, target generation, and panorama validation.
 */
class ScanGeometryTest {

    // ==================== Quaternion to Rotation Matrix Tests ====================

    @Test
    fun quaternionToRotationMatrix_identity() {
        // Identity quaternion (1, 0, 0, 0) should produce identity matrix
        val matrix = ScanGeometry.quaternionToRotationMatrix(qw = 1f, qx = 0f, qy = 0f, qz = 0f)

        // Expected identity matrix: [1 0 0; 0 1 0; 0 0 1]
        assertEquals(1f, matrix[0], 0.0001f)  // m00
        assertEquals(0f, matrix[1], 0.0001f)  // m01
        assertEquals(0f, matrix[2], 0.0001f)  // m02
        assertEquals(0f, matrix[3], 0.0001f)  // m10
        assertEquals(1f, matrix[4], 0.0001f)  // m11
        assertEquals(0f, matrix[5], 0.0001f)  // m12
        assertEquals(0f, matrix[6], 0.0001f)  // m20
        assertEquals(0f, matrix[7], 0.0001f)  // m21
        assertEquals(1f, matrix[8], 0.0001f)  // m22
    }

    @Test
    fun quaternionToRotationMatrix_90DegreeRotationZ() {
        // Quaternion for 90° rotation around Z axis: (cos(45°), 0, 0, sin(45°))
        val angle = 90f * PI.toFloat() / 180f
        val halfAngle = angle / 2f
        val qw = cos(halfAngle)
        val qz = sin(halfAngle)

        val matrix = ScanGeometry.quaternionToRotationMatrix(qw = qw, qx = 0f, qy = 0f, qz = qz)

        // 90° rotation around Z: [0 -1 0; 1 0 0; 0 0 1]
        assertEquals(0f, matrix[0], 0.001f)   // m00 ≈ 0
        assertEquals(-1f, matrix[1], 0.001f)  // m01 ≈ -1
        assertEquals(1f, matrix[3], 0.001f)   // m10 ≈ 1
        assertEquals(0f, matrix[4], 0.001f)   // m11 ≈ 0
        assertEquals(1f, matrix[8], 0.001f)   // m22 ≈ 1
    }

    @Test
    fun quaternionToRotationMatrix_resultShape() {
        val matrix = ScanGeometry.quaternionToRotationMatrix(qw = 1f, qx = 0f, qy = 0f, qz = 0f)
        assertEquals(9, matrix.size)  // 3×3 matrix in row-major order
    }

    // ==================== Frame Overlap Tests ====================

    @Test
    fun calculateFrameOverlap_identicalHeadings() {
        val overlap = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 0f)
        assertEquals(1f, overlap, 0.001f)  // Max overlap when identical
    }

    @Test
    fun calculateFrameOverlap_symmetry() {
        val overlap1 = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 30f)
        val overlap2 = ScanGeometry.calculateFrameOverlap(heading1 = 30f, heading2 = 0f)
        assertEquals(overlap1, overlap2, 0.0001f)  // Symmetric
    }

    @Test
    fun calculateFrameOverlap_smallDifference() {
        val overlap = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 10f)
        assertTrue(overlap > 0.7f)  // Should have good overlap
        assertTrue(overlap < 1f)    // But not perfect
    }

    @Test
    fun calculateFrameOverlap_mediumDifference() {
        val overlap = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 30f)
        assertTrue(overlap > 0.3f)  // Should have some overlap
        assertTrue(overlap < 0.7f)
    }

    @Test
    fun calculateFrameOverlap_largeDifference() {
        val overlap = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 60f)
        assertTrue(overlap >= 0f)   // Can be zero or small
        assertTrue(overlap < 0.5f)
    }

    @Test
    fun calculateFrameOverlap_noOverlap() {
        val overlap = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 100f)
        assertEquals(0f, overlap, 0.0001f)  // No overlap
    }

    @Test
    fun calculateFrameOverlap_wrapping_359to1Degree() {
        // Test heading wrapping: 359° and 1° are close together
        val overlap = ScanGeometry.calculateFrameOverlap(heading1 = 359f, heading2 = 1f)
        val overlapDirect = ScanGeometry.calculateFrameOverlap(heading1 = 0f, heading2 = 2f)
        assertEquals(overlapDirect, overlap, 0.001f)  // Should be similar (wrapping handled)
    }

    @Test
    fun calculateFrameOverlap_range() {
        // All overlaps should be in [0, 1]
        for (h1 in 0..360 step 30) {
            for (h2 in 0..360 step 30) {
                val overlap = ScanGeometry.calculateFrameOverlap(h1.toFloat(), h2.toFloat())
                assertTrue(overlap >= 0f && overlap <= 1f, "Overlap out of range: $overlap")
            }
        }
    }

    // ==================== Generate Scan Targets Tests ====================

    @Test
    fun generateScanTargets_6Frames() {
        val targets = ScanGeometry.generateScanTargets(frameCount = 6, anchorHeading = 0f)
        assertEquals(6, targets.size)
        // 6 frames → 60° apart
        assertEquals(0f, targets[0].headingDeg, 0.001f)
        assertEquals(60f, targets[1].headingDeg, 0.001f)
        assertEquals(120f, targets[2].headingDeg, 0.001f)
        assertEquals(180f, targets[3].headingDeg, 0.001f)
        assertEquals(240f, targets[4].headingDeg, 0.001f)
        assertEquals(300f, targets[5].headingDeg, 0.001f)
    }

    @Test
    fun generateScanTargets_10Frames() {
        val targets = ScanGeometry.generateScanTargets(frameCount = 10, anchorHeading = 0f)
        assertEquals(10, targets.size)
        // 10 frames → 36° apart
        assertEquals(0f, targets[0].headingDeg, 0.001f)
        assertEquals(36f, targets[1].headingDeg, 0.001f)
        assertEquals(72f, targets[2].headingDeg, 0.001f)
    }

    @Test
    fun generateScanTargets_12Frames() {
        val targets = ScanGeometry.generateScanTargets(frameCount = 12, anchorHeading = 0f)
        assertEquals(12, targets.size)
        // 12 frames → 30° apart
        assertEquals(0f, targets[0].headingDeg, 0.001f)
        assertEquals(30f, targets[1].headingDeg, 0.001f)
        assertEquals(60f, targets[2].headingDeg, 0.001f)
        assertEquals(90f, targets[3].headingDeg, 0.001f)
    }

    @Test
    fun generateScanTargets_evenlySpaced() {
        val targets = ScanGeometry.generateScanTargets(frameCount = 12, anchorHeading = 0f)
        val angleStep = 360f / 12

        for (i in 0 until 12) {
            val expected = (i * angleStep) % 360f
            assertEquals(expected, targets[i].headingDeg, 0.001f)
        }
    }

    @Test
    fun generateScanTargets_consecutiveOverlapValid() {
        val targets = ScanGeometry.generateScanTargets(frameCount = 12, anchorHeading = 0f)

        // Each consecutive pair should have valid overlap
        for (i in 0 until targets.size - 1) {
            val overlap = ScanGeometry.calculateFrameOverlap(
                targets[i].headingDeg,
                targets[i + 1].headingDeg
            )
            assertTrue(overlap > 0.2f, "Consecutive frames should have overlap")
        }
    }

    @Test
    fun generateScanTargets_targetIndexSequence() {
        val targets = ScanGeometry.generateScanTargets(frameCount = 12, anchorHeading = 45f)
        for (i in 0 until 12) {
            assertEquals(i, targets[i].targetIndex)
        }
    }

    @Test
    fun generateScanTargets_anchorOffset() {
        val targetsNoOffset = ScanGeometry.generateScanTargets(frameCount = 12, anchorHeading = 0f)
        val targetsWithOffset = ScanGeometry.generateScanTargets(frameCount = 12, anchorHeading = 90f)

        // All targets should be offset by the anchor heading
        for (i in 0 until 12) {
            val expected = (targetsNoOffset[i].headingDeg + 90f) % 360f
            assertEquals(expected, targetsWithOffset[i].headingDeg, 0.001f)
        }
    }

    @Test
    fun generateScanTargets_invalidFrameCount_tooSmall() {
        try {
            ScanGeometry.generateScanTargets(frameCount = 5, anchorHeading = 0f)
            assertTrue(false, "Should throw for frameCount < 6")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("6") ?: false)
        }
    }

    @Test
    fun generateScanTargets_invalidFrameCount_tooLarge() {
        try {
            ScanGeometry.generateScanTargets(frameCount = 13, anchorHeading = 0f)
            assertTrue(false, "Should throw for frameCount > 12")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("12") ?: false)
        }
    }

    // ==================== Panorama Dimension Tests ====================

    @Test
    fun calculatePanoramaDimensions_standardInput() {
        val (width, height) = ScanGeometry.calculatePanoramaDimensions(1920, 1440)
        assertEquals(4096, width)
        assertEquals(2048, height)
    }

    @Test
    fun calculatePanoramaDimensions_smallInput() {
        val (width, height) = ScanGeometry.calculatePanoramaDimensions(1280, 960)
        assertEquals(2048, width)
        assertEquals(1024, height)
    }

    @Test
    fun calculatePanoramaDimensions_largeInput() {
        val (width, height) = ScanGeometry.calculatePanoramaDimensions(4096, 3072)
        assertEquals(4096, width)
        assertEquals(2048, height)
    }

    @Test
    fun calculatePanoramaDimensions_2to1AspectRatio() {
        val (width, height) = ScanGeometry.calculatePanoramaDimensions(2000, 1500)
        assertEquals(2f, width.toFloat() / height.toFloat(), 0.001f)  // 2:1 ratio
    }

    @Test
    fun calculatePanoramaDimensions_invalidInput_zeroWidth() {
        try {
            ScanGeometry.calculatePanoramaDimensions(0, 1440)
            assertTrue(false, "Should throw for zero width")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("positive") ?: false)
        }
    }

    @Test
    fun calculatePanoramaDimensions_invalidInput_negativeHeight() {
        try {
            ScanGeometry.calculatePanoramaDimensions(1920, -1440)
            assertTrue(false, "Should throw for negative height")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("positive") ?: false)
        }
    }

    // ==================== Panorama Validation Tests ====================

    @Test
    fun isValidPanorama_standard4096x2048() {
        val valid = ScanGeometry.isValidPanorama(width = 4096, height = 2048, fileSizeBytes = 10_000_000)
        assertTrue(valid)
    }

    @Test
    fun isValidPanorama_smaller2048x1024() {
        val valid = ScanGeometry.isValidPanorama(width = 2048, height = 1024, fileSizeBytes = 5_000_000)
        assertTrue(valid)
    }

    @Test
    fun isValidPanorama_aspectRatioTooSmall() {
        val valid = ScanGeometry.isValidPanorama(width = 4000, height = 2048, fileSizeBytes = 10_000_000)
        assertFalse(valid)  // Ratio is ~1.95, outside [1.99, 2.01]
    }

    @Test
    fun isValidPanorama_aspectRatioTooLarge() {
        val valid = ScanGeometry.isValidPanorama(width = 4200, height = 2048, fileSizeBytes = 10_000_000)
        assertFalse(valid)  // Ratio is ~2.05, outside [1.99, 2.01]
    }

    @Test
    fun isValidPanorama_fileSizeExceeds50MB() {
        val valid = ScanGeometry.isValidPanorama(width = 4096, height = 2048, fileSizeBytes = 60_000_000)
        assertFalse(valid)  // > 50MB
    }

    @Test
    fun isValidPanorama_fileSizeAt50MB() {
        val valid = ScanGeometry.isValidPanorama(width = 4096, height = 2048, fileSizeBytes = 52_428_800)
        assertFalse(valid)  // Exactly 50MB should fail (>= check)
    }

    @Test
    fun isValidPanorama_fileSizeJustBelow50MB() {
        val valid = ScanGeometry.isValidPanorama(width = 4096, height = 2048, fileSizeBytes = 52_428_799)
        assertTrue(valid)
    }

    @Test
    fun isValidPanorama_zeroWidth() {
        val valid = ScanGeometry.isValidPanorama(width = 0, height = 2048, fileSizeBytes = 10_000_000)
        assertFalse(valid)
    }

    @Test
    fun isValidPanorama_negativeHeight() {
        val valid = ScanGeometry.isValidPanorama(width = 4096, height = -2048, fileSizeBytes = 10_000_000)
        assertFalse(valid)
    }

    @Test
    fun isValidPanorama_perfectAspectRatio() {
        // Test exact 2:1 ratios
        assertTrue(ScanGeometry.isValidPanorama(width = 2000, height = 1000, fileSizeBytes = 5_000_000))
        assertTrue(ScanGeometry.isValidPanorama(width = 4096, height = 2048, fileSizeBytes = 10_000_000))
    }

    @Test
    fun isValidPanorama_boundaryCases() {
        // Just within tolerance: 1.99 ratio ≈ 4078 x 2048
        assertTrue(ScanGeometry.isValidPanorama(width = 4078, height = 2048, fileSizeBytes = 10_000_000))

        // Just outside tolerance: 1.98 ratio ≈ 4055 x 2048
        assertFalse(ScanGeometry.isValidPanorama(width = 4055, height = 2048, fileSizeBytes = 10_000_000))

        // Just within tolerance: 2.01 ratio ≈ 4115 x 2048
        assertTrue(ScanGeometry.isValidPanorama(width = 4115, height = 2048, fileSizeBytes = 10_000_000))

        // Just outside tolerance: 2.02 ratio ≈ 4137 x 2048
        assertFalse(ScanGeometry.isValidPanorama(width = 4137, height = 2048, fileSizeBytes = 10_000_000))
    }
}
