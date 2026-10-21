package com.example.scanner.photosphere

internal fun meanOrientation(samples: List<OrientationData>): OrientationData {
    val valid = samples.filter { it.hasFix }
    val reference = valid.lastOrNull() ?: samples.lastOrNull() ?: OrientationData()
    if (valid.isEmpty()) return reference

    val bases = valid.mapNotNull { it.cameraBasis }
    if (bases.size == valid.size) {
        val basis = meanCameraBasis(bases)
        val angles = anglesFromCameraBasis(basis, FloatArray(3))
        return OrientationData(
            yawDegrees = angles[0],
            pitchDegrees = angles[1],
            rollDegrees = angles[2],
            accuracy = reference.accuracy,
            timestampNanos = reference.timestampNanos,
            cameraBasis = basis,
        )
    }

    return OrientationData(
        yawDegrees = normalizeDegrees(meanAngleDegrees(valid.map { it.yawDegrees })),
        pitchDegrees = meanAngleDegrees(valid.map { it.pitchDegrees }),
        rollDegrees = normalizeDegrees(meanAngleDegrees(valid.map { it.rollDegrees })),
        accuracy = reference.accuracy,
        timestampNanos = reference.timestampNanos,
    )
}

private fun meanCameraBasis(bases: List<FloatArray>): FloatArray {
    val matrices = bases.map { basis -> DoubleArray(9) { basis[it].toDouble() } }
    val mean = RotationMath.weightedMeanRotation(matrices, List(matrices.size) { 1.0 })
    return FloatArray(9) { mean[it].toFloat() }
}

internal fun meanAngleDegrees(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    val reference = values.first()
    var sum = 0.0
    values.forEach { value ->
        var deviation = value - reference
        while (deviation > 180f) deviation -= 360f
        while (deviation < -180f) deviation += 360f
        sum += deviation
    }
    return reference + (sum / values.size).toFloat()
}
