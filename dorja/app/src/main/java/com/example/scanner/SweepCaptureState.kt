package com.example.scanner

/**
 * State machine for Samsung/Apple-style horizontal sweep panorama capture.
 * 
 * Replaces the old CaptureState which was designed for discrete 12-frame circular scanning.
 * This new state machine supports continuous capture during a single smooth pan.
 */
enum class SweepCaptureState {
    /**
     * Initial state. User has positioned the camera and is ready to start the sweep.
     * 
     * UI: Display "Tap to start" button with sweep direction preview
     * Next: Transitions to SWEEPING_IN_PROGRESS when user taps "Start"
     */
    READY_TO_START,
    
    /**
     * Active capture in progress. User is panning the camera horizontally.
     * Strips are being captured automatically as motion is detected.
     * 
     * UI: Display directional arrow, progress bar, and real-time preview strip
     * Next: 
     * - PAUSED if motion stops for >1 second
     * - SWEEP_COMPLETE when sweep range is covered
     * - ERROR if capture fails
     */
    SWEEPING_IN_PROGRESS,
    
    /**
     * Capture paused because user stopped moving.
     * Strips are not being captured during pause.
     * 
     * UI: Display "Keep moving to continue" prompt
     * Next:
     * - SWEEPING_IN_PROGRESS when motion resumes
     * - ERROR if paused too long (>10 seconds)
     */
    PAUSED,
    
    /**
     * Sweep range completed. All required strips have been captured.
     * 
     * UI: Display "Sweep complete" message, begin stitching animation
     * Next: Automatically transitions to STITCHING
     */
    SWEEP_COMPLETE,
    
    /**
     * Final stitching in progress (if not done in real-time).
     * Combining all captured strips into final panorama.
     * 
     * UI: Display progress spinner "Stitching panorama..."
     * Next:
     * - COMPLETED when stitching finishes successfully
     * - ERROR if stitching fails
     */
    STITCHING,
    
    /**
     * Panorama completed successfully and ready for review/save.
     * 
     * UI: Display final panorama with "Retake" and "Save" buttons
     * Next: Terminal state (or READY_TO_START if user retakes)
     */
    COMPLETED,
    
    /**
     * An error occurred during capture, stitching, or processing.
     * 
     * UI: Display error message with "Retry" button
     * Next: READY_TO_START if user retries
     */
    ERROR
}

/**
 * Metadata for the current sweep capture session.
 * Tracks progress, captured strips, and quality metrics.
 */
data class SweepSession(
    /**
     * Current capture state.
     */
    val state: SweepCaptureState,
    
    /**
     * Sweep configuration (range, direction, etc.).
     */
    val config: SweepConfig,
    
    /**
     * Number of strips captured so far.
     */
    val stripsCaptured: Int = 0,
    
    /**
     * Estimated total strips for this sweep.
     */
    val estimatedTotalStrips: Int = config.estimateStripCount(),
    
    /**
     * Sweep progress as percentage [0.0, 1.0].
     */
    val progress: Float = 0f,
    
    /**
     * Current motion analysis result.
     */
    val motionAnalysis: MotionAnalysis? = null,
    
    /**
     * Time when sweep started (milliseconds since epoch).
     */
    val startTime: Long = System.currentTimeMillis(),
    
    /**
     * Time when last strip was captured.
     */
    val lastCaptureTime: Long = startTime,
    
    /**
     * Average stitching quality score [0, 1] for captured strips.
     * 1.0 = perfect alignment, 0.0 = poor alignment.
     */
    val stitchingQuality: Float = 1.0f,
    
    /**
     * Error message if state is ERROR.
     */
    val errorMessage: String? = null
) {
    /**
     * Checks if the sweep is complete (reached end of range).
     */
    fun isComplete(): Boolean {
        return progress >= 0.98f || state == SWEEP_COMPLETE || state == COMPLETED
    }
    
    /**
     * Checks if capture should be paused due to inactivity.
     */
    fun shouldPause(): Boolean {
        val timeSinceLastCapture = System.currentTimeMillis() - lastCaptureTime
        return state == SWEEPING_IN_PROGRESS && timeSinceLastCapture > 1000L
    }
    
    /**
     * Checks if session has timed out (paused too long).
     */
    fun isTimedOut(): Boolean {
        val timeSinceLastCapture = System.currentTimeMillis() - lastCaptureTime
        return state == PAUSED && timeSinceLastCapture > 10000L
    }
    
    /**
     * Calculates estimated time remaining based on current progress and elapsed time.
     */
    fun estimateTimeRemaining(): Long {
        if (progress <= 0f) return 0L
        
        val elapsed = System.currentTimeMillis() - startTime
        val estimatedTotal = elapsed / progress
        val remaining = estimatedTotal - elapsed
        
        return maxOf(0L, remaining.toLong())
    }
    
    /**
     * Gets completion percentage as integer [0, 100].
     */
    fun getProgressPercent(): Int {
        return (progress * 100f).toInt().coerceIn(0, 100)
    }
}
