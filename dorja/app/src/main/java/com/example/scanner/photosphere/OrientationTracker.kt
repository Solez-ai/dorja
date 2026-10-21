package com.example.scanner.photosphere

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val TAG = "OrientationTracker"
private const val MATRIX_SIZE = 9
private const val ROTATION_VECTOR_SIZE = 4
private const val SENSOR_THREAD_NAME = "orientation-sensor"

data class OrientationData(
    val yawDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val accuracy: OrientationAccuracy = OrientationAccuracy.Unknown,
    val timestampNanos: Long = 0L,
    val cameraBasis: FloatArray? = null,
) {
    val hasFix: Boolean get() = timestampNanos > 0L
}

enum class OrientationAccuracy {
    Unknown,
    Unreliable,
    Low,
    Medium,
    High,
    ;

    val isUsable: Boolean get() = this == Medium || this == High
    val allowsCapture: Boolean get() = this != Unreliable

    internal companion object {
        fun fromSensorAccuracy(accuracy: Int): OrientationAccuracy = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> High
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Medium
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Low
            SensorManager.SENSOR_STATUS_UNRELIABLE -> Unreliable
            else -> Unknown
        }
    }
}

enum class OrientationReference {
    Screen,
    Camera,
}

class OrientationTracker(
    context: Context,
    private val samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME,
    private val reference: OrientationReference = OrientationReference.Camera,
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationVectorSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val sensorType: Int = rotationVectorSensor?.type ?: Sensor.TYPE_ROTATION_VECTOR
    private val isMagnetometerFree: Boolean = sensorType == Sensor.TYPE_GAME_ROTATION_VECTOR

    val isSensorAvailable: Boolean get() = rotationVectorSensor != null

    private val _orientation = MutableStateFlow(OrientationData())
    val orientation: StateFlow<OrientationData> = _orientation.asStateFlow()

    @Volatile
    var displayRotation: Int = Surface.ROTATION_0

    private val rotationVectorValues = FloatArray(ROTATION_VECTOR_SIZE)
    private val rotationMatrix = FloatArray(MATRIX_SIZE)
    private val displayMatrix = FloatArray(MATRIX_SIZE)
    private val anglesRadians = FloatArray(3)
    private val basisScratch = FloatArray(MATRIX_SIZE)

    @Volatile
    private var accuracy = OrientationAccuracy.Unknown

    private val lock = Any()
    private var isListening = false
    private var sensorThread: HandlerThread? = null

    fun startListening(): Boolean {
        val manager = sensorManager ?: return false
        val sensor = rotationVectorSensor ?: return false

        synchronized(lock) {
            if (isListening) return true

            val thread = HandlerThread(SENSOR_THREAD_NAME).apply { start() }
            val registered = manager.registerListener(
                this,
                sensor,
                samplingPeriodUs,
                Handler(thread.looper),
            )

            if (registered) {
                sensorThread = thread
                isListening = true
            } else {
                Log.w(TAG, "Rotation vector registration refused")
                thread.quitSafely()
            }
            return registered
        }
    }

    fun stopListening() {
        synchronized(lock) {
            if (!isListening) return

            sensorManager?.unregisterListener(this)
            isListening = false
            sensorThread?.quitSafely()
            sensorThread = null
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != sensorType) return

        val raw = event.values
        if (raw.size < 3) return

        val values = if (raw.size > ROTATION_VECTOR_SIZE) {
            raw.copyInto(
                destination = rotationVectorValues,
                endIndex = ROTATION_VECTOR_SIZE,
            )
        } else {
            raw
        }

        try {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Rejected a malformed rotation vector", e)
            return
        }

        val basis = cameraBasisMatrix(rotationMatrix, displayRotation, basisScratch)

        val angles = when (reference) {
            OrientationReference.Screen ->
                screenAnglesDegrees(rotationMatrix) ?: return
            OrientationReference.Camera ->
                anglesFromCameraBasis(basis, anglesRadians)
        }

        _orientation.value = OrientationData(
            yawDegrees = angles[0],
            pitchDegrees = angles[1],
            rollDegrees = angles[2],
            accuracy = accuracyOf(event),
            timestampNanos = event.timestamp,
            cameraBasis = basis.copyOf(),
        )
    }

    private fun accuracyOf(event: SensorEvent): OrientationAccuracy {
        if (isMagnetometerFree) return OrientationAccuracy.Unknown
        val reported = accuracy
        if (reported != OrientationAccuracy.Unknown) return reported
        val fromEvent = OrientationAccuracy.fromSensorAccuracy(event.accuracy)
        return if (fromEvent == OrientationAccuracy.Unreliable) reported else fromEvent
    }

    private fun screenAnglesDegrees(source: FloatArray): FloatArray? {
        val axes = DisplayAxes.forDisplayRotation(displayRotation)
        if (!SensorManager.remapCoordinateSystem(source, axes.axisX, axes.axisY, displayMatrix)) {
            Log.w(TAG, "Display remap rejected for rotation $displayRotation")
            return null
        }
        SensorManager.getOrientation(displayMatrix, anglesRadians)
        anglesRadians[0] = normalizeDegrees(anglesRadians[0].toDegrees())
        anglesRadians[1] = anglesRadians[1].toDegrees()
        anglesRadians[2] = normalizeDegrees(anglesRadians[2].toDegrees())
        return anglesRadians
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type != sensorType || isMagnetometerFree) return
        val mapped = OrientationAccuracy.fromSensorAccuracy(accuracy)
        this.accuracy = mapped
        _orientation.update { it.copy(accuracy = mapped) }
    }
}

internal enum class DisplayAxes(val axisX: Int, val axisY: Int) {
    Rotation0(SensorManager.AXIS_X, SensorManager.AXIS_Y),
    Rotation90(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X),
    Rotation180(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y),
    Rotation270(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X),
    ;

    internal companion object {
        fun forDisplayRotation(displayRotation: Int): DisplayAxes = when (displayRotation) {
            Surface.ROTATION_90 -> Rotation90
            Surface.ROTATION_180 -> Rotation180
            Surface.ROTATION_270 -> Rotation270
            else -> Rotation0
        }
    }
}

private fun Float.toDegrees(): Float = Math.toDegrees(toDouble()).toFloat()

internal fun normalizeDegrees(degrees: Float): Float {
    var value = degrees % 360f
    if (value >= 180f) value -= 360f
    if (value < -180f) value += 360f
    return value
}

internal fun cameraBasisMatrix(
    deviceToWorld: FloatArray,
    displayRotation: Int,
    out: FloatArray,
): FloatArray {
    val fx = -deviceToWorld[2]
    val fy = -deviceToWorld[5]
    val fz = -deviceToWorld[8]

    val rx: Float
    val ry: Float
    val rz: Float
    val ux: Float
    val uy: Float
    val uz: Float
    when (displayRotation) {
        Surface.ROTATION_90 -> {
            rx = deviceToWorld[1]; ry = deviceToWorld[4]; rz = deviceToWorld[7]
            ux = -deviceToWorld[0]; uy = -deviceToWorld[3]; uz = -deviceToWorld[6]
        }
        Surface.ROTATION_180 -> {
            rx = -deviceToWorld[0]; ry = -deviceToWorld[3]; rz = -deviceToWorld[6]
            ux = -deviceToWorld[1]; uy = -deviceToWorld[4]; uz = -deviceToWorld[7]
        }
        Surface.ROTATION_270 -> {
            rx = -deviceToWorld[1]; ry = -deviceToWorld[4]; rz = -deviceToWorld[7]
            ux = deviceToWorld[0]; uy = deviceToWorld[3]; uz = deviceToWorld[6]
        }
        else -> {
            rx = deviceToWorld[0]; ry = deviceToWorld[3]; rz = deviceToWorld[6]
            ux = deviceToWorld[1]; uy = deviceToWorld[4]; uz = deviceToWorld[7]
        }
    }

    out[0] = rx; out[1] = -ux; out[2] = fx
    out[3] = ry; out[4] = -uy; out[5] = fy
    out[6] = rz; out[7] = -uz; out[8] = fz
    return out
}

internal fun anglesFromCameraBasis(basis: FloatArray, out: FloatArray): FloatArray {
    val fx = basis[2].toDouble()
    val fy = basis[5].toDouble()
    val fz = basis[8].toDouble()

    val yaw = Math.toDegrees(atan2(fx, fy)).toFloat()
    val elevation = asin(fz.coerceIn(-1.0, 1.0))
    val pitch = -Math.toDegrees(elevation).toFloat()

    val yawRad = Math.toRadians(yaw.toDouble())
    val sinYaw = sin(yawRad)
    val cosYaw = cos(yawRad)
    val sinElevation = sin(elevation)
    val cosElevation = cos(elevation)
    val up0X = -sinYaw * sinElevation
    val up0Y = -cosYaw * sinElevation
    val up0Z = cosElevation
    val rx = basis[0].toDouble()
    val ry = basis[3].toDouble()
    val rz = basis[6].toDouble()
    val ux = -basis[1].toDouble()
    val uy = -basis[4].toDouble()
    val uz = -basis[7].toDouble()
    val sinRoll = -(rx * up0X + ry * up0Y + rz * up0Z)
    val cosRoll = ux * up0X + uy * up0Y + uz * up0Z

    out[0] = normalizeDegrees(yaw)
    out[1] = pitch
    out[2] = normalizeDegrees(Math.toDegrees(atan2(sinRoll, cosRoll)).toFloat())
    return out
}

internal fun cameraAnglesDegrees(
    deviceToWorld: FloatArray,
    displayRotation: Int,
    out: FloatArray,
): FloatArray {
    val basis = FloatArray(MATRIX_SIZE)
    cameraBasisMatrix(deviceToWorld, displayRotation, basis)
    return anglesFromCameraBasis(basis, out)
}
