package com.example.scanner

/**
 * Represents the current sensor configuration available on the device.
 * Used to track which sensors are being used for orientation tracking.
 */
enum class SensorMode {
    /**
     * Gyroscope is available and is the primary sensor for orientation tracking.
     * May be supplemented with accelerometer for stability.
     */
    GYRO_PRIMARY,

    /**
     * Gyroscope is not available or has failed.
     * The app falls back to accelerometer + magnetometer fusion for orientation.
     */
    ACCEL_MAG_FALLBACK,

    /**
     * No suitable sensors are available.
     * The scanner cannot function on this device.
     */
    UNAVAILABLE
}
