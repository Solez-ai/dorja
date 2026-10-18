package com.example.scanner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Manages sensor fusion for device orientation tracking.
 *
 * Provides two modes:
 * 1. Primary (GYRO_PRIMARY): Gyroscope + accelerometer fusion using complementary filter
 * 2. Fallback (ACCEL_MAG_FALLBACK): Accelerometer + magnetometer fusion when gyro unavailable
 *
 * Handles sensor availability detection, sensor failures, low-pass smoothing, and
 * magnetometer interference detection.
 *
 * Output: Orientation(headingDeg, pitchDeg, rollDeg) with validated Euler angles
 */
class SensorFusionManager(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensor references
    private val gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Current sensor mode
    private var sensorMode: SensorMode = SensorMode.UNAVAILABLE

    // Sensor data buffers
    private val gyroData = FloatArray(3)
    private val accelData = FloatArray(3)
    private val magData = FloatArray(3)

    // Quaternion state (gyro integration)
    private var qw = 1f
    private var qx = 0f
    private var qy = 0f
    private var qz = 0f

    // Low-pass filtering
    private val lowPassAlpha = 0.25f
    private var prevOrientation = Orientation()

    // Magnetometer interference detection
    private val magVarianceHistory = CircularBuffer(SIZE = 50)
    private val magInterferenceThreshold = 500f // Threshold for variance

    // Timing
    private var lastTimestamp = 0L

    // Callback for orientation updates
    var onOrientationUpdate: ((Orientation, SensorMode) -> Unit)? = null

    // Listener state
    private var isListening = false

    init {
        detectAvailableSensors()
    }

    /**
     * Detects available sensors and sets the appropriate sensor mode.
     * Called during initialization and when sensors fail.
     */
    fun detectAvailableSensors() {
        val hasGyro = gyroscopeSensor != null
        val hasAccel = accelerometerSensor != null
        val hasMag = magnetometerSensor != null

        sensorMode = when {
            hasGyro && hasAccel -> {
                Log.d(TAG, "Sensor configuration: GYRO_PRIMARY (gyro + accel)")
                SensorMode.GYRO_PRIMARY
            }
            hasAccel && hasMag -> {
                Log.d(TAG, "Sensor configuration: ACCEL_MAG_FALLBACK (accel + mag)")
                SensorMode.ACCEL_MAG_FALLBACK
            }
            else -> {
                Log.w(TAG, "Insufficient sensors available for orientation tracking")
                SensorMode.UNAVAILABLE
            }
        }
    }

    /**
     * Starts listening to sensor events.
     * Should be called when the scanner screen becomes active.
     */
    fun start() {
        if (isListening) return

        if (sensorMode == SensorMode.UNAVAILABLE) {
            Log.e(TAG, "Cannot start sensor fusion: no suitable sensors available")
            return
        }

        // Register all available sensors
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        isListening = true
        Log.d(TAG, "Sensor fusion started")
    }

    /**
     * Stops listening to sensor events.
     * Should be called when the scanner screen becomes inactive.
     */
    fun stop() {
        if (!isListening) return

        sensorManager.unregisterListener(this)
        isListening = false
        Log.d(TAG, "Sensor fusion stopped")
    }

    /**
     * Main sensor event handler.
     * Processes sensor events and updates orientation based on current mode.
     */
    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        try {
            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> {
                    System.arraycopy(event.values, 0, gyroData, 0, 3)
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, accelData, 0, 3)
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magData, 0, 3)
                    checkMagnetometerInterference()
                }
            }

            // Only process every update cycle to avoid redundant calculations
            if (lastTimestamp == 0L) {
                lastTimestamp = currentTime
                return
            }

            val durationSeconds = (currentTime - lastTimestamp) / 1000f
            lastTimestamp = currentTime

            // Compute orientation based on current mode
            val orientation = when (sensorMode) {
                SensorMode.GYRO_PRIMARY -> computeGyroAccelFusion(durationSeconds)
                SensorMode.ACCEL_MAG_FALLBACK -> computeAccelMagFusion()
                SensorMode.UNAVAILABLE -> return
            }

            // Apply low-pass smoothing
            val smoothedOrientation = applyLowPassFilter(orientation)

            // Validate and clamp Euler angles
            val validatedOrientation = validateAndClampOrientation(smoothedOrientation)

            // Store for next iteration
            prevOrientation = validatedOrientation

            // Notify listener
            onOrientationUpdate?.invoke(validatedOrientation, sensorMode)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing sensor event: ${e.message}", e)
            // On error, attempt to continue with previous orientation
        }
    }

    /**
     * Gyroscope + accelerometer fusion using complementary filter.
     * Gyro provides quick rotation updates, accel corrects long-term drift.
     *
     * Complementary filter: output = alpha * gyro + (1 - alpha) * accel
     * alpha = 0.25 for stability (accel-weighted, reduces drift sensitivity)
     */
    private fun computeGyroAccelFusion(durationSeconds: Float): Orientation {
        // Integrate gyroscope to update quaternion
        integrateGyroscope(durationSeconds)

        // Compute heading from magnetometer heading (if available)
        val magHeading = computeMagnetometerHeading()

        // Convert quaternion to Euler angles
        val (heading, pitch, roll) = quaternionToEuler(qw, qx, qy, qz)

        // Get accel-based pitch estimate
        val accelPitch = computeAccelerometerPitch()

        // Complementary filter: blend gyro pitch with accel pitch
        val complementaryAlpha = 0.25f
        val fusedPitch = complementaryAlpha * accelPitch + (1 - complementaryAlpha) * pitch

        // Blend heading with magnetometer if available
        val fusedHeading = if (magHeading != null) {
            complementaryAlpha * magHeading + (1 - complementaryAlpha) * heading
        } else {
            heading
        }

        return Orientation(
            headingDeg = fusedHeading,
            pitchDeg = fusedPitch,
            rollDeg = roll
        )
    }

    /**
     * Accelerometer + magnetometer fusion (fallback mode).
     * Computes quaternion from accel (pitch) and magnetometer (heading).
     * Roll is derived from tilt.
     */
    private fun computeAccelMagFusion(): Orientation {
        // Get heading from magnetometer
        val heading = computeMagnetometerHeading() ?: 0f

        // Get pitch from accelerometer
        val pitch = computeAccelerometerPitch()

        // Compute roll from accelerometer tilt
        val roll = computeAccelerometerRoll()

        return Orientation(
            headingDeg = heading,
            pitchDeg = pitch,
            rollDeg = roll
        )
    }

    /**
     * Integrates gyroscope angular velocity to update quaternion state.
     * Uses simple Euler integration.
     */
    private fun integrateGyroscope(durationSeconds: Float) {
        // Angular velocity in rad/s
        val wx = gyroData[0]
        val wy = gyroData[1]
        val wz = gyroData[2]

        // Quaternion derivative: dq/dt = 0.5 * q * omega_quat
        // where omega_quat = (0, wx, wy, wz)
        val dqw = 0.5f * (-qx * wx - qy * wy - qz * wz) * durationSeconds
        val dqx = 0.5f * (qw * wx + qy * wz - qz * wy) * durationSeconds
        val dqy = 0.5f * (qw * wy - qx * wz + qz * wx) * durationSeconds
        val dqz = 0.5f * (qw * wz + qx * wy - qy * wx) * durationSeconds

        // Update quaternion
        qw += dqw
        qx += dqx
        qy += dqy
        qz += dqz

        // Normalize quaternion to maintain unit magnitude
        normalizeQuaternion()
    }

    /**
     * Normalizes the quaternion to unit magnitude.
     */
    private fun normalizeQuaternion() {
        val norm = sqrt(qw * qw + qx * qx + qy * qy + qz * qz)
        if (norm > 0.0001f) {
            qw /= norm
            qx /= norm
            qy /= norm
            qz /= norm
        }
    }

    /**
     * Computes pitch angle from accelerometer data.
     * Pitch = atan2(-ax, sqrt(ay^2 + az^2))
     * Assumes accelerometer is in device coordinate frame (x=right, y=up, z=toward user)
     */
    private fun computeAccelerometerPitch(): Float {
        val ax = accelData[0]
        val ay = accelData[1]
        val az = accelData[2]

        val norm = sqrt(ax * ax + ay * ay + az * az)
        if (norm < 0.1f) return 0f // Magnitude too small, likely stationary

        // Normalized acceleration
        val nax = ax / norm
        val nay = ay / norm
        val naz = az / norm

        // Pitch: angle from horizontal
        val pitchRad = atan2(-nax, sqrt(nay * nay + naz * naz))
        return Math.toDegrees(pitchRad.toDouble()).toFloat()
    }

    /**
     * Computes roll angle from accelerometer data.
     * Roll = atan2(ay, az)
     */
    private fun computeAccelerometerRoll(): Float {
        val ay = accelData[1]
        val az = accelData[2]

        val rollRad = atan2(ay, az)
        return Math.toDegrees(rollRad.toDouble()).toFloat()
    }

    /**
     * Computes heading from magnetometer data using tilt-compensated algorithm.
     * Returns null if magnetometer data is unavailable or invalid.
     */
    private fun computeMagnetometerHeading(): Float? {
        val mx = magData[0]
        val my = magData[1]
        val mz = magData[2]

        val magNorm = sqrt(mx * mx + my * my + mz * mz)
        if (magNorm < 10f) return null // Magnetometer reading too weak

        // Simplified heading calculation (ignoring tilt compensation for now)
        // Heading = atan2(-my, mx) in device frame
        val headingRad = atan2(my, mx)
        var headingDeg = Math.toDegrees(headingRad.toDouble()).toFloat()

        // Normalize to [0, 360)
        if (headingDeg < 0) headingDeg += 360f
        if (headingDeg >= 360f) headingDeg -= 360f

        return headingDeg
    }

    /**
     * Converts quaternion to Euler angles (heading, pitch, roll).
     * Returns Triple(headingDeg, pitchDeg, rollDeg)
     */
    private fun quaternionToEuler(qw: Float, qx: Float, qy: Float, qz: Float): Triple<Float, Float, Float> {
        // Roll (x-axis rotation)
        val sinr_cosp = 2f * (qw * qx + qy * qz)
        val cosr_cosp = 1f - 2f * (qx * qx + qy * qy)
        val rollRad = atan2(sinr_cosp, cosr_cosp)

        // Pitch (y-axis rotation)
        val sinp = 2f * (qw * qy - qz * qx)
        val pitchRad = if (abs(sinp) >= 1f) {
            // Gimbal lock case
            Math.PI.toFloat() / 2f * if (sinp >= 0) 1f else -1f
        } else {
            kotlin.math.asin(sinp)
        }

        // Heading (z-axis rotation)
        val siny_cosp = 2f * (qw * qz + qx * qy)
        val cosy_cosp = 1f - 2f * (qy * qy + qz * qz)
        val headingRad = atan2(siny_cosp, cosy_cosp)

        val roll = Math.toDegrees(rollRad.toDouble()).toFloat()
        val pitch = Math.toDegrees(pitchRad.toDouble()).toFloat()
        val heading = Math.toDegrees(headingRad.toDouble()).toFloat()

        return Triple(heading, pitch, roll)
    }

    /**
     * Applies exponential moving average low-pass filter to reduce jitter.
     * New = alpha * current + (1 - alpha) * previous
     * alpha = 0.25 (accel-weighted, smooth but responsive)
     */
    private fun applyLowPassFilter(orientation: Orientation): Orientation {
        val filteredHeading = lowPassAlpha * orientation.headingDeg + (1 - lowPassAlpha) * prevOrientation.headingDeg
        val filteredPitch = lowPassAlpha * orientation.pitchDeg + (1 - lowPassAlpha) * prevOrientation.pitchDeg
        val filteredRoll = lowPassAlpha * orientation.rollDeg + (1 - lowPassAlpha) * prevOrientation.rollDeg

        return Orientation(
            headingDeg = filteredHeading,
            pitchDeg = filteredPitch,
            rollDeg = filteredRoll
        )
    }

    /**
     * Validates and clamps Euler angles to valid ranges.
     * heading ∈ [0, 360)
     * pitch ∈ [-90, 90]
     * roll ∈ [-180, 180]
     */
    private fun validateAndClampOrientation(orientation: Orientation): Orientation {
        var heading = orientation.headingDeg
        var pitch = orientation.pitchDeg
        var roll = orientation.rollDeg

        // Clamp heading to [0, 360)
        while (heading < 0f) heading += 360f
        while (heading >= 360f) heading -= 360f

        // Clamp pitch to [-90, 90]
        pitch = pitch.coerceIn(-90f, 90f)

        // Clamp roll to [-180, 180]
        while (roll > 180f) roll -= 360f
        while (roll < -180f) roll += 360f

        return Orientation(
            headingDeg = heading,
            pitchDeg = pitch,
            rollDeg = roll
        )
    }

    /**
     * Detects magnetometer interference based on variance in magnetic field magnitude.
     * Logs warning if variance exceeds threshold.
     */
    private fun checkMagnetometerInterference() {
        val mx = magData[0]
        val my = magData[1]
        val mz = magData[2]

        val magnitude = sqrt(mx * mx + my * my + mz * mz)
        magVarianceHistory.add(magnitude)

        if (magVarianceHistory.count() >= 10) {
            val variance = magVarianceHistory.variance()
            if (variance > magInterferenceThreshold) {
                Log.w(TAG, "Magnetometer interference detected (variance=$variance). Move away from metal objects.")
                onMagnetometerInterferenceDetected?.invoke()
            }
        }
    }

    // Callback for magnetometer interference detection
    var onMagnetometerInterferenceDetected: (() -> Unit)? = null

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Optional: Log accuracy changes for debugging
        Log.d(TAG, "Sensor ${sensor.name} accuracy changed to $accuracy")
    }

    /**
     * Simple circular buffer for calculating rolling statistics.
     */
    private class CircularBuffer(val SIZE: Int) {
        private val buffer = FloatArray(SIZE)
        private var index = 0
        private var count = 0

        fun add(value: Float) {
            buffer[index] = value
            index = (index + 1) % SIZE
            if (count < SIZE) count++
        }

        fun count() = count

        fun variance(): Float {
            if (count < 2) return 0f

            val mean = buffer.take(count).average().toFloat()
            var sumSq = 0f
            for (i in 0 until count) {
                val diff = buffer[i] - mean
                sumSq += diff * diff
            }
            return sumSq / count
        }
    }

    companion object {
        private const val TAG = "SensorFusionManager"
    }
}
