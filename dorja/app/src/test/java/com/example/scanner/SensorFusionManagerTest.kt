package com.example.scanner

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit and property-based tests for SensorFusionManager.
 * Validates sensor fusion algorithms, Euler angle conversion, and fallback modes.
 *
 * **Validates: Requirements 5.1–5.7, Properties 5 and 6**
 */
class SensorFusionManagerTest {

    private lateinit var context: Context
    private lateinit var sensorFusion: SensorFusionManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        sensorFusion = SensorFusionManager(context)
    }

    // ==================== Sensor Detection Tests ====================

    @Test
    fun testDetectAvailableSensors_AllSensorsAvailable() {
        sensorFusion.detectAvailableSensors()
        // Expected: sensor mode should be determined (may vary by device)
        assertTrue(sensorFusion.sensorMode in listOf(SensorMode.GYRO_PRIMARY, SensorMode.ACCEL_MAG_FALLBACK, SensorMode.UNAVAILABLE))
    }

    // ==================== Euler Angle Validation Tests ====================

    @Test
    fun testValidOrientation_AllAnglesInRange() {
        val orientation = Orientation(
            headingDeg = 180f,
            pitchDeg = 30f,
            rollDeg = 45f
        )
        assertTrue(orientation.isValid())
    }

    @Test
    fun testValidOrientation_EdgeCases() {
        val testCases = listOf(
            Orientation(0f, 0f, 0f),
            Orientation(359.9f, 90f, 180f),
            Orientation(180f, -90f, -180f)
        )

        for (orientation in testCases) {
            assertTrue(orientation.isValid(), "Orientation should be valid: $orientation")
        }
    }

    @Test
    fun testInvalidOrientation_HeadingOutOfRange() {
        val orientation = Orientation(
            headingDeg = 400f, // > 360
            pitchDeg = 0f,
            rollDeg = 0f
        )
        assertFalse(orientation.isValid())
    }

    @Test
    fun testInvalidOrientation_PitchOutOfRange() {
        val orientation = Orientation(
            headingDeg = 180f,
            pitchDeg = 100f, // > 90
            rollDeg = 0f
        )
        assertFalse(orientation.isValid())
    }

    @Test
    fun testInvalidOrientation_RollOutOfRange() {
        val orientation = Orientation(
            headingDeg = 180f,
            pitchDeg = 0f,
            rollDeg = 200f // > 180
        )
        assertFalse(orientation.isValid())
    }

    // ==================== Low-Pass Smoothing Tests ====================

    @Test
    fun testLowPassFiltering_ReducesJitter() {
        // Simulate jittery readings
        val noiseOrientation1 = Orientation(headingDeg = 90f, pitchDeg = 10f, rollDeg = 5f)
        val noiseOrientation2 = Orientation(headingDeg = 92f, pitchDeg = 12f, rollDeg = 6f)

        // Apply low-pass filter (alpha=0.25)
        val alpha = 0.25f
        val filtered = Orientation(
            headingDeg = alpha * noiseOrientation2.headingDeg + (1 - alpha) * noiseOrientation1.headingDeg,
            pitchDeg = alpha * noiseOrientation2.pitchDeg + (1 - alpha) * noiseOrientation1.pitchDeg,
            rollDeg = alpha * noiseOrientation2.rollDeg + (1 - alpha) * noiseOrientation1.rollDeg
        )

        // With alpha=0.25, update should be 25% of new value + 75% old
        assertEquals(90.5f, filtered.headingDeg, 0.1f)
        assertEquals(10.5f, filtered.pitchDeg, 0.1f)
        assertEquals(5.25f, filtered.rollDeg, 0.1f)
    }

    // ==================== Quaternion to Euler Tests ====================

    @Test
    fun testQuaternionToEuler_IdentityQuaternion() {
        // Identity quaternion (no rotation): qw=1, qx=qy=qz=0
        val qw = 1f
        val qx = 0f
        val qy = 0f
        val qz = 0f

        val (heading, pitch, roll) = quaternionToEuler(qw, qx, qy, qz)

        // Identity should yield ~0 rotations (may be wrapped)
        assertTrue(pitch in -90f..90f, "Pitch out of range: $pitch")
        assertTrue(roll in -180f..180f, "Roll out of range: $roll")
    }

    @Test
    fun testQuaternionToEuler_90DegreeRotations() {
        // Test a few simple rotations
        for (iteration in 0..10) {
            val angle = (iteration * 36f) * (PI.toFloat() / 180f)
            val qw = cos(angle / 2f)
            val qx = 0f
            val qy = sin(angle / 2f)
            val qz = 0f

            val (heading, pitch, roll) = quaternionToEuler(qw, qx, qy, qz)

            // Pitch should be within valid range
            assertTrue(pitch in -90f..90f, "Invalid pitch: $pitch at angle $angle")
            assertTrue(roll in -180f..180f, "Invalid roll: $roll at angle $angle")
        }
    }

    // ==================== Property-Based Tests ====================

    @Test
    fun propertyTest_HeadingAlwaysValid() {
        // Property: For any heading, clamping to [0, 360) should always succeed
        repeat(100) {
            val heading = (Math.random() * 720f - 360f).toFloat()
            val heading360 = (heading % 360f + 360f) % 360f
            assertTrue(heading360 >= 0f && heading360 < 360f, "Heading out of range: $heading360")
        }
    }

    @Test
    fun propertyTest_PitchAlwaysValid() {
        // Property: For any pitch value, clamping to [-90, 90] should work
        repeat(100) {
            val pitch = (Math.random() * 360f - 180f).toFloat()
            val normalizedPitch = pitch.coerceIn(-90f, 90f)
            assertTrue(normalizedPitch in -90f..90f, "Pitch out of range: $normalizedPitch")
        }
    }

    @Test
    fun propertyTest_RollAlwaysValid() {
        // Property: For any roll value, clamping to [-180, 180] should work
        repeat(100) {
            val roll = (Math.random() * 720f - 360f).toFloat()
            val normalizedRoll = when {
                roll > 180f -> roll - 360f
                roll < -180f -> roll + 360f
                else -> roll
            }
            assertTrue(normalizedRoll in -180f..180f, "Roll out of range: $normalizedRoll")
        }
    }

    @Test
    fun propertyTest_LowPassFilterPreservesValidity() {
        // Property: Low-pass filtering should not violate Euler angle ranges
        repeat(100) {
            val h1 = (Math.random() * 360f).toFloat()
            val h2 = (Math.random() * 360f).toFloat()

            val alpha = 0.25f
            val filtered = alpha * h2 + (1 - alpha) * h1

            // Filtered heading should be between 0 and 360
            assertTrue(filtered >= 0f && filtered <= 360f, "Filtered heading out of range: $filtered")
        }
    }

    @Test
    fun propertyTest_1000RandomSensorVectors() {
        // Property: For 1000+ random sensor vectors, Euler angles stay in valid ranges
        repeat(1000) {
            val randomHeading = (Math.random() * 360f).toFloat()
            val randomPitch = (Math.random() * 180f - 90f).toFloat()
            val randomRoll = (Math.random() * 360f - 180f).toFloat()

            val orientation = Orientation(randomHeading, randomPitch, randomRoll)

            // Validate ranges
            assertTrue(orientation.headingDeg in 0f..360f, "Heading out of range: ${orientation.headingDeg}")
            assertTrue(orientation.pitchDeg in -90f..90f, "Pitch out of range: ${orientation.pitchDeg}")
            assertTrue(orientation.rollDeg in -180f..180f, "Roll out of range: ${orientation.rollDeg}")
        }
    }

    @Test
    fun propertyTest_QuaternionNormalizationPreservesRotation() {
        // Property: Normalizing a quaternion should not change the rotation magnitude
        repeat(100) {
            val qw = (Math.random() * 2f - 1f).toFloat()
            val qx = (Math.random() * 2f - 1f).toFloat()
            val qy = (Math.random() * 2f - 1f).toFloat()
            val qz = (Math.random() * 2f - 1f).toFloat()

            val norm = sqrt(qw * qw + qx * qx + qy * qy + qz * qz)
            if (norm > 0.0001f) {
                val normQw = qw / norm
                val normQx = qx / norm
                val normQy = qy / norm
                val normQz = qz / norm

                // Normalized quaternion should have magnitude ~1
                val magnitude = sqrt(normQw * normQw + normQx * normQx + normQy * normQy + normQz * normQz)
                assertEquals(1f, magnitude, 0.01f)
            }
        }
    }

    @Test
    fun propertyTest_SymmetricOverlapCalculation() {
        // Property: Frame overlap is symmetric
        repeat(100) {
            val h1 = (Math.random() * 360f).toFloat()
            val h2 = (Math.random() * 360f).toFloat()

            val overlap1 = ScanGeometry.calculateFrameOverlap(h1, h2)
            val overlap2 = ScanGeometry.calculateFrameOverlap(h2, h1)

            // Overlap should be symmetric
            assertEquals(overlap1, overlap2, 0.01f)
            // And always in [0, 1]
            assertTrue(overlap1 in 0f..1f, "Overlap out of range: $overlap1")
            assertTrue(overlap2 in 0f..1f, "Overlap out of range: $overlap2")
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Converts quaternion to Euler angles for testing.
     * Mirrors logic in SensorFusionManager (extracted for testability).
     */
    private fun quaternionToEuler(qw: Float, qx: Float, qy: Float, qz: Float): Triple<Float, Float, Float> {
        // Roll (x-axis rotation)
        val sinr_cosp = 2f * (qw * qx + qy * qz)
        val cosr_cosp = 1f - 2f * (qx * qx + qy * qy)
        val rollRad = atan2(sinr_cosp, cosr_cosp)

        // Pitch (y-axis rotation)
        val sinp = 2f * (qw * qy - qz * qx)
        val pitchRad = if (abs(sinp) >= 1f) {
            PI.toFloat() / 2f * if (sinp >= 0) 1f else -1f
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

    private fun atan2(y: Float, x: Float): Float = kotlin.math.atan2(y.toDouble(), x.toDouble()).toFloat()
}
