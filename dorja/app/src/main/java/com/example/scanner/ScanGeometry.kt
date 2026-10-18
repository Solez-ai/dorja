package com.example.scanner

/**
 * Encapsulates all geometric calculations for panoramic scanning.
 * Handles rotation matrices, frame overlap detection, and panorama dimension calculations.
 */
object ScanGeometry {
    /**
     * Represents a single capture target position in the scan sequence.
     *
     * @param targetIndex Unique index of this target in the sequence (0-based)
     * @param ringIndex Vertical ring index (0 for flat scan, higher for multi-row captures)
     * @param pitchDeg Pitch angle in degrees [-90, 90]. 0 = horizontal, positive = up, negative = down.
     * @param headingDeg Target compass heading in degrees [0, 360)
     */
    data class ScanTarget(
        val targetIndex: Int,
        val ringIndex: Int = 0,
        val pitchDeg: Float = 0f,
        val headingDeg: Float
    ) {
        /**
         * Validates that the target data is internally consistent.
         *
         * @return true if all values are within expected ranges, false otherwise.
         */
        fun isValid(): Boolean {
            if (targetIndex < 0) return false
            if (ringIndex < 0) return false
            if (pitchDeg !in -90f..90f) return false
            if (headingDeg !in 0f..360f) return false
            return true
        }
    }

    /**
     * Computes a 3×3 rotation matrix from a unit quaternion.
     * Used to transform frame positions for overlap calculations.
     *
     * @param qw Quaternion w component (scalar)
     * @param qx Quaternion x component
     * @param qy Quaternion y component
     * @param qz Quaternion z component
     * @return 3×3 rotation matrix as a 9-element FloatArray [m00, m01, m02, m10, m11, m12, m20, m21, m22]
     *
     * Quaternion should be normalized: qw² + qx² + qy² + qz² = 1
     */
    fun quaternionToRotationMatrix(
        qw: Float,
        qx: Float,
        qy: Float,
        qz: Float
    ): FloatArray {
        val matrix = FloatArray(9)

        val sqw = qw * qw
        val sqx = qx * qx
        val sqy = qy * qy
        val sqz = qz * qz

        val invs = 1f / (sqw + sqx + sqy + sqz)
        matrix[0] = (sqw + sqx - sqy - sqz) * invs
        matrix[4] = (sqw - sqx + sqy - sqz) * invs
        matrix[8] = (sqw - sqx - sqy + sqz) * invs

        var tmp1 = qx * qy
        var tmp2 = qz * qw
        matrix[1] = 2f * (tmp1 + tmp2) * invs
        matrix[3] = 2f * (tmp1 - tmp2) * invs

        tmp1 = qx * qz
        tmp2 = qy * qw
        matrix[2] = 2f * (tmp1 - tmp2) * invs
        matrix[6] = 2f * (tmp1 + tmp2) * invs

        tmp1 = qy * qz
        tmp2 = qx * qw
        matrix[5] = 2f * (tmp1 + tmp2) * invs
        matrix[7] = 2f * (tmp1 - tmp2) * invs

        return matrix
    }

    /**
     * Calculates the overlap ratio between two frames based on their heading difference.
     * Assumes frames are captured in a horizontal ring at the same pitch.
     *
     * This is a symmetric operation: overlap(h1, h2) ≡ overlap(h2, h1)
     *
     * @param heading1 Compass heading of the first frame, in degrees [0, 360)
     * @param heading2 Compass heading of the second frame, in degrees [0, 360)
     * @return Overlap ratio in [0, 1], where 1.0 = 100% overlap, 0.0 = no overlap
     */
    fun calculateFrameOverlap(heading1: Float, heading2: Float): Float {
        // Normalize headings to [0, 360)
        val h1 = heading1 % 360f
        val h2 = heading2 % 360f

        // Calculate shortest angle difference (always 0-180 degrees)
        var diff = kotlin.math.abs(h1 - h2)
        if (diff > 180f) diff = 360f - diff

        // Assume each frame covers approximately 45 degrees with overlap.
        // Maximum overlap at 0 degrees difference, linearly decreasing to 0 at 45 degrees.
        // This is a simplified model; actual overlap depends on camera FOV.
        val maxOverlapAngle = 45f
        val overlap = (maxOverlapAngle - diff) / maxOverlapAngle

        return overlap.coerceIn(0f, 1f)
    }

    /**
     * Generates a list of evenly-spaced scan targets around a 360-degree ring.
     *
     * @param frameCount Number of frames to capture (typically 6, 10, or 12)
     * @param anchorHeading The compass heading of the first (anchor) frame, in degrees [0, 360)
     * @return List of [ScanTarget] with evenly-spaced headings 360/frameCount degrees apart
     *
     * Example: frameCount=12, anchorHeading=0 → targets at 0°, 30°, 60°, ..., 330°
     */
    fun generateScanTargets(frameCount: Int, anchorHeading: Float): List<ScanTarget> {
        require(frameCount in 6..12) { "Frame count must be between 6 and 12" }

        val targets = mutableListOf<ScanTarget>()
        val angleStep = 360f / frameCount

        for (i in 0 until frameCount) {
            val heading = (anchorHeading + (i * angleStep)) % 360f
            targets.add(
                ScanTarget(
                    targetIndex = i,
                    ringIndex = 0,
                    pitchDeg = 0f,
                    headingDeg = heading
                )
            )
        }

        return targets
    }

    /**
     * Calculates the output panorama dimensions for a given input resolution and frame count.
     * Ensures a 2:1 aspect ratio (width/height = 2) and power-of-2 sizes for efficiency.
     *
     * @param inputWidth Input frame width, in pixels
     * @param inputHeight Input frame height, in pixels
     * @return Pair<Int, Int> (outputWidth, outputHeight) maintaining 2:1 aspect ratio
     *
     * Example: inputWidth=1920, inputHeight=1440 → Pair(4096, 2048)
     */
    fun calculatePanoramaDimensions(inputWidth: Int, inputHeight: Int): Pair<Int, Int> {
        require(inputWidth > 0 && inputHeight > 0) { "Input dimensions must be positive" }

        // Standard panorama resolution is 4096×2048 (2:1 aspect ratio)
        val standardWidth = 4096
        val standardHeight = 2048

        // If input is very small, use a smaller standard
        if (inputWidth < 1920 || inputHeight < 1440) {
            return Pair(2048, 1024)
        }

        // If input is larger than standard, maintain 2:1 but cap at standard
        return Pair(standardWidth, standardHeight)
    }

    /**
     * Validates that a panorama matches expected constraints.
     *
     * @param width Panorama width in pixels
     * @param height Panorama height in pixels
     * @param fileSizeBytes File size in bytes
     * @return true if panorama is valid, false if it violates constraints
     *
     * Constraints:
     * - Aspect ratio (width/height) must be in range [1.99, 2.01] (allows ±0.5% deviation from 2:1)
     * - File size must be < 50MB (52428800 bytes)
     */
    fun isValidPanorama(width: Int, height: Int, fileSizeBytes: Long): Boolean {
        if (width <= 0 || height <= 0) return false

        val aspectRatio = width.toFloat() / height
        if (aspectRatio !in 1.99f..2.01f) return false

        if (fileSizeBytes >= 52428800L) return false // 50MB limit

        return true
    }
}
