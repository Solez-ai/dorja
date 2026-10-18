package com.example.scanner

/**
 * Represents metadata for a single captured frame in a panoramic scan.
 * This data is used for stitching, alignment, and frame preview.
 *
 * @param path File path to the captured JPEG image (e.g., "/cache/frame_r0_c0_xxx.jpg")
 * @param heading Compass heading at the time of capture, in degrees [0, 360)
 * @param pitchDeg Pitch (up/down tilt) at capture time, in degrees [-90, 90]. Default 0 for flat horizontal scans.
 * @param row Ring index for vertical scanning (stacking frames at different heights).
 *            Default 0 for single-ring (flat) scans. Higher values for multi-row panoramas.
 * @param col Frame index within the ring (0-11 for 12-frame scan, 0-9 for 10-frame, etc.)
 * @param isCap Boolean flag indicating if this frame is a ceiling or floor capture (not part of the main ring).
 * @param capType Ceiling or floor marker. Values: "ceiling", "floor", or null for regular frames.
 */
data class FrameData(
    val path: String,
    val heading: Float,
    val pitchDeg: Float = 0f,
    val row: Int = 0,
    val col: Int = 0,
    val isCap: Boolean = false,
    val capType: String? = null
) {
    /**
     * Validates that the frame data is internally consistent and valid.
     *
     * @return true if all values are within expected ranges and constraints, false otherwise.
     */
    fun isValid(): Boolean {
        // Path should not be empty
        if (path.isBlank()) return false

        // Heading must be in [0, 360)
        if (heading !in 0f..360f) return false

        // Pitch must be in [-90, 90]
        if (pitchDeg !in -90f..90f) return false

        // Row and column indices should be non-negative
        if (row < 0 || col < 0) return false

        // If isCap is true, capType must be "ceiling" or "floor"
        if (isCap && capType !in listOf("ceiling", "floor")) return false

        // If isCap is false, capType should be null
        if (!isCap && capType != null) return false

        return true
    }
}
