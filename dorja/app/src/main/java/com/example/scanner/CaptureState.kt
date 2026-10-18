package com.example.scanner

/**
 * Represents the state of a capture session in the scanner.
 * This state machine guides the user through the multi-frame capture process.
 */
enum class CaptureState {
    /**
     * User has not yet confirmed the anchor (initial reference pose).
     * The app is waiting for the user to position the camera and tap "Start Scan".
     */
    WAITING_FOR_INITIAL_ANCHOR,

    /**
     * The app is capturing the first frame (anchor frame).
     * User should hold steady while the first frame is being processed.
     */
    CAPTURING_INITIAL_FRAME,

    /**
     * The app is guiding the user to move to the next capture target.
     * AR guidance ring is active, showing where the next frame should be taken.
     */
    MOVING_TO_TARGET,

    /**
     * The camera is at a target position and the app is waiting for stabilization.
     * User should hold steady for 2 seconds (or gyroscope/accel confirms stability).
     * Once stable, the app will auto-trigger the capture.
     */
    STABILIZING,

    /**
     * All required frames have been captured (e.g., 12 frames for full scan).
     * The app is now ready to stitch or display results.
     */
    COMPLETED,

    /**
     * An error occurred during capture (camera binding failed, sensor unavailable, etc.).
     * User should see a recovery UI with instructions.
     */
    ERROR
}
