package com.example.scanner

/**
 * Configuration for horizontal sweep panorama capture.
 * Defines the sweep range, direction, and capture parameters.
 */
data class SweepConfig(
    /**
     * Starting compass heading in degrees [0, 360).
     * Typically set to user's current heading when "Start" is tapped.
     */
    val startHeading: Float,
    
    /**
     * Total sweep angle in degrees.
     * Common values:
     * - 90° for narrow room scan
     * - 120° for standard room (default)
     * - 180° for wide panorama
     */
    val sweepAngle: Float = 120f,
    
    /**
     * Direction of the sweep.
     */
    val direction: SweepDirection = SweepDirection.AUTO_DETECT,
    
    /**
     * Minimum rotation angle between strip captures (in degrees).
     * Smaller values = more strips = smoother panorama but larger file.
     * Range: 2-5 degrees recommended.
     */
    val stripIntervalDegrees: Float = 3f,
    
    /**
     * Optimal rotation velocity in degrees per second.
     * Used to provide speed feedback to user.
     */
    val optimalVelocity: Float = 15f,
    
    /**
     * Maximum allowed tilt from horizontal (in degrees).
     * If device pitch exceeds this, warn user to level camera.
     */
    val maxTiltDegrees: Float = 10f
) {
    /**
     * Calculates the ending heading for this sweep.
     * Handles wrapping around 360°/0° boundary.
     */
    fun getEndHeading(): Float {
        val end = when (direction) {
            SweepDirection.LEFT_TO_RIGHT -> (startHeading + sweepAngle) % 360f
            SweepDirection.RIGHT_TO_LEFT -> (startHeading - sweepAngle + 360f) % 360f
            SweepDirection.AUTO_DETECT -> (startHeading + sweepAngle) % 360f
        }
        return end
    }
    
    /**
     * Calculates sweep progress as a percentage [0.0, 1.0].
     * 
     * @param currentHeading Current compass heading in degrees [0, 360)
     * @return Progress from 0.0 (start) to 1.0 (end), clamped to [0, 1]
     */
    fun calculateProgress(currentHeading: Float): Float {
        val normalizedCurrent = normalizeAngle(currentHeading)
        val normalizedStart = normalizeAngle(startHeading)
        
        // Calculate angle traveled from start
        var traveled = when (direction) {
            SweepDirection.LEFT_TO_RIGHT -> {
                if (normalizedCurrent >= normalizedStart) {
                    normalizedCurrent - normalizedStart
                } else {
                    (360f - normalizedStart) + normalizedCurrent
                }
            }
            SweepDirection.RIGHT_TO_LEFT -> {
                if (normalizedCurrent <= normalizedStart) {
                    normalizedStart - normalizedCurrent
                } else {
                    normalizedStart + (360f - normalizedCurrent)
                }
            }
            SweepDirection.AUTO_DETECT -> {
                // Same as LEFT_TO_RIGHT until direction is determined
                if (normalizedCurrent >= normalizedStart) {
                    normalizedCurrent - normalizedStart
                } else {
                    (360f - normalizedStart) + normalizedCurrent
                }
            }
        }
        
        val progress = traveled / sweepAngle
        return progress.coerceIn(0f, 1f)
    }
    
    /**
     * Checks if the sweep is complete (reached end heading).
     */
    fun isComplete(currentHeading: Float): Boolean {
        return calculateProgress(currentHeading) >= 0.98f // 98% to account for sensor noise
    }
    
    /**
     * Normalizes an angle to [0, 360) range.
     */
    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0f) normalized += 360f
        return normalized
    }
    
    /**
     * Estimates the number of strips that will be captured for this sweep.
     */
    fun estimateStripCount(): Int {
        return (sweepAngle / stripIntervalDegrees).toInt()
    }
    
    companion object {
        /**
         * Creates a default sweep config starting from the current heading.
         */
        fun createDefault(currentHeading: Float): SweepConfig {
            return SweepConfig(
                startHeading = currentHeading,
                sweepAngle = 120f,
                direction = SweepDirection.AUTO_DETECT
            )
        }
        
        /**
         * Creates a narrow sweep (90°) for small rooms or tight spaces.
         */
        fun createNarrow(currentHeading: Float): SweepConfig {
            return SweepConfig(
                startHeading = currentHeading,
                sweepAngle = 90f,
                direction = SweepDirection.AUTO_DETECT
            )
        }
        
        /**
         * Creates a wide sweep (180°) for large rooms or panoramic views.
         */
        fun createWide(currentHeading: Float): SweepConfig {
            return SweepConfig(
                startHeading = currentHeading,
                sweepAngle = 180f,
                direction = SweepDirection.AUTO_DETECT,
                stripIntervalDegrees = 2.5f // More strips for smoother wide panorama
            )
        }
    }
}

/**
 * Direction of the horizontal sweep.
 */
enum class SweepDirection {
    /**
     * Sweep from left to right (clockwise when viewed from above).
     */
    LEFT_TO_RIGHT,
    
    /**
     * Sweep from right to left (counterclockwise when viewed from above).
     */
    RIGHT_TO_LEFT,
    
    /**
     * Automatically detect direction from first movement.
     * After detecting, updates to LEFT_TO_RIGHT or RIGHT_TO_LEFT.
     */
    AUTO_DETECT
}
