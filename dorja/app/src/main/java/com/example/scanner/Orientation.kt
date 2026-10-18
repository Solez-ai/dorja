package com.example.scanner

/**
 * Represents the 3D rotational orientation of the device in Euler angles (heading, pitch, roll).
 * Derived from sensor fusion (gyroscope, accelerometer, magnetometer).
 *
 * @param headingDeg Compass heading in degrees [0, 360). 0° = North, 90° = East, 180° = South, 270° = West.
 * @param pitchDeg Pitch angle in degrees [-90, 90]. Positive = looking up, Negative = looking down.
 * @param rollDeg Roll angle in degrees [-180, 180]. Positive = tilted right, Negative = tilted left.
 */
data class Orientation(
    val headingDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f
) {
    /**
     * Validates that all Euler angles are within their valid ranges.
     *
     * @return true if heading ∈ [0, 360), pitch ∈ [-90, 90], roll ∈ [-180, 180], false otherwise.
     */
    fun isValid(): Boolean =
        headingDeg in 0f..360f &&
        pitchDeg in -90f..90f &&
        rollDeg in -180f..180f
}
