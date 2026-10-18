package com.example.scanner

import android.util.Log
import kotlin.math.abs

/**
 * Analyzes device motion to provide feedback for smooth panorama capture.
 * 
 * Monitors rotation velocity, smoothness, and stability to determine if the user
 * is panning at the optimal speed and keeping the device level.
 * 
 * Works in conjunction with SensorFusionManager to receive orientation updates.
 */
class MotionAnalyzer(
    private val sweepConfig: SweepConfig
) {
    // Circular buffer for heading history (stores ~0.5 seconds at 100Hz)
    private val headingHistory = CircularBuffer(size = 50)
    private val velocityHistory = CircularBuffer(size = 20)
    
    // Last analyzed state
    private var lastMotionState: MotionState = MotionState.STATIONARY
    private var lastAnalysisTime = 0L
    
    // Direction detection
    private var detectedDirection: SweepDirection? = null
    private var directionConfidenceCount = 0
    
    /**
     * Processes a new orientation update and analyzes motion state.
     * Should be called on every orientation update from SensorFusionManager.
     * 
     * @param orientation Current device orientation
     * @return Current motion state with velocity and quality metrics
     */
    fun analyzeMotion(orientation: Orientation): MotionAnalysis {
        val currentTime = System.currentTimeMillis()
        
        // Add heading to history
        headingHistory.add(orientation.headingDeg)
        
        // Calculate rotation velocity (degrees per second)
        val velocity = calculateRotationVelocity()
        velocityHistory.add(velocity)
        
        // Detect sweep direction if AUTO_DETECT
        if (sweepConfig.direction == SweepDirection.AUTO_DETECT && detectedDirection == null) {
            detectDirection(velocity)
        }
        
        // Analyze motion quality
        val motionState = determineMotionState(velocity, orientation.pitchDeg)
        val smoothness = calculateSmoothness()
        
        lastMotionState = motionState
        lastAnalysisTime = currentTime
        
        return MotionAnalysis(
            state = motionState,
            velocity = velocity,
            smoothness = smoothness,
            detectedDirection = detectedDirection ?: sweepConfig.direction,
            pitchAngle = orientation.pitchDeg,
            isTilted = abs(orientation.pitchDeg) > sweepConfig.maxTiltDegrees
        )
    }
    
    /**
     * Calculates the current rotation velocity in degrees per second.
     * Uses recent heading history to estimate velocity.
     */
    private fun calculateRotationVelocity(): Float {
        if (headingHistory.size() < 5) return 0f
        
        val recentHeadings = headingHistory.getRecent(10) // Last ~0.1 seconds
        if (recentHeadings.size < 2) return 0f
        
        // Calculate differences between consecutive headings
        val differences = mutableListOf<Float>()
        for (i in 1 until recentHeadings.size) {
            var diff = recentHeadings[i] - recentHeadings[i - 1]
            
            // Handle 360°/0° wrap-around
            if (diff > 180f) diff -= 360f
            if (diff < -180f) diff += 360f
            
            differences.add(diff)
        }
        
        // Average difference per sample * samples per second
        val avgDiffPerSample = differences.average().toFloat()
        val samplesPerSecond = 100f // Assuming ~100Hz sensor updates
        
        return avgDiffPerSample * samplesPerSecond
    }
    
    /**
     * Calculates motion smoothness as a quality metric [0, 1].
     * 1.0 = perfectly smooth, 0.0 = very jerky.
     */
    private fun calculateSmoothness(): Float {
        if (velocityHistory.size() < 5) return 1.0f
        
        val recentVelocities = velocityHistory.getRecent(10)
        if (recentVelocities.size < 2) return 1.0f
        
        // Calculate velocity variance (jerkiness indicator)
        val mean = recentVelocities.average().toFloat()
        val variance = recentVelocities.map { (it - mean) * (it - mean) }.average().toFloat()
        
        // Convert variance to smoothness score
        // Low variance (< 10) = smooth = score 1.0
        // High variance (> 100) = jerky = score 0.0
        val smoothness = when {
            variance < 10f -> 1.0f
            variance > 100f -> 0.0f
            else -> 1.0f - ((variance - 10f) / 90f)
        }
        
        return smoothness.coerceIn(0f, 1f)
    }
    
    /**
     * Detects sweep direction from velocity pattern.
     * Requires consistent movement in one direction for 5 consecutive samples.
     */
    private fun detectDirection(velocity: Float) {
        if (abs(velocity) < 5f) {
            // Not moving enough to detect direction
            directionConfidenceCount = 0
            return
        }
        
        val currentDirection = if (velocity > 0) {
            SweepDirection.LEFT_TO_RIGHT
        } else {
            SweepDirection.RIGHT_TO_LEFT
        }
        
        if (detectedDirection == null) {
            detectedDirection = currentDirection
            directionConfidenceCount = 1
        } else if (detectedDirection == currentDirection) {
            directionConfidenceCount++
            
            if (directionConfidenceCount >= 5) {
                // Direction confirmed
                Log.d(TAG, "Sweep direction detected: $detectedDirection")
            }
        } else {
            // Direction changed, reset
            detectedDirection = null
            directionConfidenceCount = 0
        }
    }
    
    /**
     * Determines the current motion state based on velocity and tilt.
     */
    private fun determineMotionState(velocity: Float, pitchDeg: Float): MotionState {
        val absVelocity = abs(velocity)
        
        // Check tilt first
        if (abs(pitchDeg) > sweepConfig.maxTiltDegrees) {
            return MotionState.TILTED
        }
        
        // Check velocity
        return when {
            absVelocity < 3f -> MotionState.STATIONARY
            absVelocity < 8f -> MotionState.TOO_SLOW
            absVelocity > 25f -> MotionState.TOO_FAST
            calculateSmoothness() < 0.6f -> MotionState.JERKY
            else -> MotionState.GOOD
        }
    }
    
    /**
     * Checks if motion is suitable for capturing a strip.
     * Only capture during GOOD motion state.
     */
    fun shouldCaptureStrip(): Boolean {
        return lastMotionState == MotionState.GOOD
    }
    
    /**
     * Resets the analyzer state (call when starting a new sweep).
     */
    fun reset() {
        headingHistory.clear()
        velocityHistory.clear()
        detectedDirection = null
        directionConfidenceCount = 0
        lastMotionState = MotionState.STATIONARY
    }
    
    /**
     * Gets the currently detected or configured sweep direction.
     */
    fun getCurrentDirection(): SweepDirection {
        return detectedDirection ?: sweepConfig.direction
    }
    
    companion object {
        private const val TAG = "MotionAnalyzer"
    }
}

/**
 * Current state of device motion for panorama capture.
 */
enum class MotionState {
    /**
     * Device is not moving (velocity near zero).
     */
    STATIONARY,
    
    /**
     * Device is moving too slowly (<8°/sec).
     * Panorama capture will take too long.
     */
    TOO_SLOW,
    
    /**
     * Device is moving too fast (>25°/sec).
     * Strips may have motion blur or stitching artifacts.
     */
    TOO_FAST,
    
    /**
     * Device motion is jerky/unstable (high velocity variance).
     * Results in poor stitching quality.
     */
    JERKY,
    
    /**
     * Device is tilted more than allowed threshold from horizontal.
     * Panorama will have vertical misalignment.
     */
    TILTED,
    
    /**
     * Motion is optimal for panorama capture.
     * Smooth, properly-paced, and level.
     */
    GOOD
}

/**
 * Result of motion analysis with all relevant metrics.
 */
data class MotionAnalysis(
    /**
     * Current motion state classification.
     */
    val state: MotionState,
    
    /**
     * Rotation velocity in degrees per second.
     * Positive = left-to-right, negative = right-to-left.
     */
    val velocity: Float,
    
    /**
     * Motion smoothness quality metric [0, 1].
     * 1.0 = perfectly smooth, 0.0 = very jerky.
     */
    val smoothness: Float,
    
    /**
     * Detected or configured sweep direction.
     */
    val detectedDirection: SweepDirection,
    
    /**
     * Current device pitch angle (vertical tilt).
     */
    val pitchAngle: Float,
    
    /**
     * True if device is tilted beyond acceptable threshold.
     */
    val isTilted: Boolean
) {
    /**
     * Gets user-friendly feedback message for current motion state.
     */
    fun getFeedbackMessage(): String {
        return when (state) {
            MotionState.STATIONARY -> "Start moving to begin sweep"
            MotionState.TOO_SLOW -> "Move a bit faster"
            MotionState.TOO_FAST -> "Slow down"
            MotionState.JERKY -> "Move more smoothly"
            MotionState.TILTED -> "Keep camera level"
            MotionState.GOOD -> "Perfect - keep going"
        }
    }
    
    /**
     * Gets optimal velocity guidance based on current velocity.
     */
    fun getVelocityGuidance(optimalVelocity: Float): String {
        val diff = abs(velocity) - optimalVelocity
        return when {
            diff < -5f -> "↑ Speed up"
            diff > 5f -> "↓ Slow down"
            else -> "✓ Good pace"
        }
    }
}

/**
 * Simple circular buffer for storing recent values.
 */
private class CircularBuffer(val size: Int) {
    private val buffer = FloatArray(size)
    private var index = 0
    private var count = 0
    
    fun add(value: Float) {
        buffer[index] = value
        index = (index + 1) % size
        if (count < size) count++
    }
    
    fun size(): Int = count
    
    fun getRecent(n: Int): List<Float> {
        val actualN = minOf(n, count)
        val result = mutableListOf<Float>()
        
        for (i in 0 until actualN) {
            val idx = (index - 1 - i + size) % size
            result.add(0, buffer[idx])
        }
        
        return result
    }
    
    fun clear() {
        count = 0
        index = 0
    }
}
