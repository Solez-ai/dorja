package com.example.scanner.photosphere

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tan

data class FieldOfView(
    val horizontalDegrees: Float,
    val verticalDegrees: Float,
) {
    init {
        require(horizontalDegrees in 1f..179f) { "horizontalDegrees out of (1, 179): $horizontalDegrees" }
        require(verticalDegrees in 1f..179f) { "verticalDegrees out of (1, 179): $verticalDegrees" }
    }

    fun transposed(): FieldOfView = FieldOfView(
        horizontalDegrees = verticalDegrees,
        verticalDegrees = horizontalDegrees,
    )

    companion object {
        val TYPICAL_PORTRAIT = FieldOfView(horizontalDegrees = 52f, verticalDegrees = 66f)
    }
}

data class TargetViewCoordinate(
    val x: Float,
    val y: Float,
    val z: Float,
    val angularDistanceDegrees: Float,
) {
    val isInFront: Boolean get() = z > 0f
}

object SphereProjection {

    fun project(
        orientation: OrientationData,
        target: SphereTarget,
    ): TargetViewCoordinate {
        val yaw = Math.toRadians(orientation.yawDegrees.toDouble())
        val elevation = Math.toRadians((-orientation.pitchDegrees).toDouble())
        val roll = Math.toRadians(orientation.rollDegrees.toDouble())

        val sinYaw = sin(yaw)
        val cosYaw = cos(yaw)
        val cosElevation = cos(elevation)

        val forwardX = sinYaw * cosElevation
        val forwardY = cosYaw * cosElevation
        val forwardZ = sin(elevation)

        val right0X = cosYaw
        val right0Y = -sinYaw
        val up0X = right0Y * forwardZ
        val up0Y = -right0X * forwardZ
        val up0Z = right0X * forwardY - right0Y * forwardX

        val cosRoll = cos(roll)
        val sinRoll = sin(roll)
        val rightX = right0X * cosRoll - up0X * sinRoll
        val rightY = right0Y * cosRoll - up0Y * sinRoll
        val rightZ = -up0Z * sinRoll
        val upX = up0X * cosRoll + right0X * sinRoll
        val upY = up0Y * cosRoll + right0Y * sinRoll
        val upZ = up0Z * cosRoll

        val tx = target.directionX.toDouble()
        val ty = target.directionY.toDouble()
        val tz = target.directionZ.toDouble()

        val x = tx * rightX + ty * rightY + tz * rightZ
        val y = tx * upX + ty * upY + tz * upZ
        val z = (tx * forwardX + ty * forwardY + tz * forwardZ).coerceIn(-1.0, 1.0)

        return TargetViewCoordinate(
            x = x.toFloat(),
            y = y.toFloat(),
            z = z.toFloat(),
            angularDistanceDegrees = Math.toDegrees(acos(z)).toFloat(),
        )
    }

    fun focalLengthPx(
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        fieldOfView: FieldOfView,
    ): Float {
        if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return 0f
        val tanHalfH = tan(Math.toRadians(fieldOfView.horizontalDegrees / 2.0)).toFloat()
        val tanHalfV = tan(Math.toRadians(fieldOfView.verticalDegrees / 2.0)).toFloat()
        val fx = (viewportWidthPx / 2f) / tanHalfH
        val fy = (viewportHeightPx / 2f) / tanHalfV
        return max(fx, fy)
    }
}
