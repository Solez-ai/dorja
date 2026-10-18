package com.example.scanner

/**
 * Represents the zoom capabilities detected on the device's camera hardware.
 * Used to constrain the zoom slider and set appropriate default zoom levels.
 *
 * @param minZoomRatio Minimum zoom ratio (typically 0.5x for ultra-wide, 1.0x for standard)
 * @param maxZoomRatio Maximum zoom ratio (typically 2.0x to 8.0x depending on lens quality)
 * @param defaultZoom Default zoom ratio to start with (typically 0.8x or 1.0x)
 * @param hasUltraWide true if the device has an ultra-wide lens (equiv. focal length < 24mm)
 * @param hasStandard true if the device has a standard lens (equiv. focal length 28-35mm)
 * @param hasTelephoto true if the device has a telephoto lens (equiv. focal length > 50mm)
 */
data class ZoomCapabilities(
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
    val defaultZoom: Float,
    val hasUltraWide: Boolean,
    val hasStandard: Boolean,
    val hasTelephoto: Boolean
) {
    /**
     * Validates that zoom capabilities are internally consistent.
     *
     * @return true if all zoom values are valid and consistent, false otherwise.
     */
    fun isValid(): Boolean {
        // Min zoom should be positive and <= max zoom
        if (minZoomRatio <= 0f || minZoomRatio > maxZoomRatio) return false

        // Max zoom should be positive
        if (maxZoomRatio <= 0f) return false

        // Default zoom should be within the valid range
        if (defaultZoom < minZoomRatio || defaultZoom > maxZoomRatio) return false

        // At least one lens type should be available
        if (!hasUltraWide && !hasStandard && !hasTelephoto) return false

        return true
    }

    companion object {
        /**
         * Creates a safe default ZoomCapabilities when hardware detection fails.
         * Assumes a standard lens with modest zoom range.
         */
        fun createDefault(): ZoomCapabilities {
            return ZoomCapabilities(
                minZoomRatio = 1.0f,
                maxZoomRatio = 2.0f,
                defaultZoom = 1.0f,
                hasUltraWide = false,
                hasStandard = true,
                hasTelephoto = false
            )
        }
    }
}
