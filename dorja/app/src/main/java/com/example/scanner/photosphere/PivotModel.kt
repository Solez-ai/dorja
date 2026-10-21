package com.example.scanner.photosphere

data class PivotModel(
    val leverArmMeters: Float,
    val sceneDistanceMeters: Float,
) {
    init {
        require(leverArmMeters >= 0f) { "lever arm must not be negative, was $leverArmMeters" }
        require(sceneDistanceMeters > 0f) {
            "scene distance must be positive, was $sceneDistanceMeters"
        }
    }

    val ratio: Double =
        (leverArmMeters.toDouble() / sceneDistanceMeters.toDouble()).coerceIn(0.0, MAX_RATIO)

    companion object {
        const val MAX_RATIO: Double = 0.25
        val None: PivotModel = PivotModel(leverArmMeters = 0f, sceneDistanceMeters = 1f)
        val HandheldBodySwivel: PivotModel =
            PivotModel(leverArmMeters = 0.35f, sceneDistanceMeters = 10f)
    }
}
