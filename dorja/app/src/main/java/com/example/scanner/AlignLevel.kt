package com.example.scanner

/**
 * Represents the alignment quality level of the current camera position relative to a target.
 * Used to provide visual feedback to the user via the AR guidance ring color.
 */
enum class AlignLevel {
    /**
     * Poor alignment. The camera is far from the target position.
     * Typically displayed as red on the guidance ring.
     */
    RED,

    /**
     * Acceptable alignment. The camera is approaching the target but not yet optimal.
     * Typically displayed as yellow on the guidance ring.
     */
    YELLOW,

    /**
     * Excellent alignment. The camera is properly positioned at the target.
     * Typically displayed as green on the guidance ring.
     * May trigger auto-capture or haptic feedback.
     */
    GREEN
}
