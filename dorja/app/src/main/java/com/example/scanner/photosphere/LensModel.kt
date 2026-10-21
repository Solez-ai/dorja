package com.example.scanner.photosphere

import kotlin.math.abs

class RadialDistortion(
    val coefficients: DoubleArray,
) {
    val isSignificant: Boolean
        get() = coefficients.size >= 3 && coefficients.any { abs(it) > 0.0 }

    fun pixelCoefficientsFor(focalLengthPx: Double): DoubleArray? {
        if (!isSignificant || !focalLengthPx.isFinite() || focalLengthPx <= 0.0) return null
        val f2 = focalLengthPx * focalLengthPx
        val f4 = f2 * f2
        val f6 = f4 * f2
        return doubleArrayOf(
            coefficients[0] / f2,
            coefficients[1] / f4,
            coefficients[2] / f6,
        )
    }
}

internal fun radialFactor(coefficients: DoubleArray, r2: Double): Double =
    1.0 + r2 * (coefficients[0] + r2 * (coefficients[1] + r2 * coefficients[2]))

internal fun distortPixel(
    column: Double,
    row: Double,
    centreX: Double,
    centreY: Double,
    coefficients: DoubleArray,
): DoubleArray {
    val x = column - centreX
    val y = row - centreY
    val factor = radialFactor(coefficients, x * x + y * y)
    return doubleArrayOf(centreX + x * factor, centreY + y * factor)
}

private const val UNDISTORT_ITERATIONS = 12
private const val UNDISTORT_TOLERANCE_PX = 1e-4

internal fun undistortPixel(
    column: Double,
    row: Double,
    centreX: Double,
    centreY: Double,
    coefficients: DoubleArray,
): DoubleArray {
    val targetX = column - centreX
    val targetY = row - centreY
    var x = targetX
    var y = targetY
    repeat(UNDISTORT_ITERATIONS) {
        val factor = radialFactor(coefficients, x * x + y * y)
        if (factor <= 0.0) return doubleArrayOf(centreX + x, centreY + y)
        val nextX = targetX / factor
        val nextY = targetY / factor
        val settled = abs(nextX - x) < UNDISTORT_TOLERANCE_PX &&
            abs(nextY - y) < UNDISTORT_TOLERANCE_PX
        x = nextX
        y = nextY
        if (settled) return doubleArrayOf(centreX + x, centreY + y)
    }
    return doubleArrayOf(centreX + x, centreY + y)
}
