package com.example.scanner.photosphere

data class AlignmentState(
    val distanceDegrees: Float = Float.NaN,
    val isWithinThreshold: Boolean = false,
    val dwellProgress: Float = 0f,
    val triggered: Boolean = false,
)

class AlignmentGate(
    val thresholdDegrees: Float = DEFAULT_THRESHOLD_DEGREES,
    val requiredDwellMillis: Long = DEFAULT_DWELL_MILLIS,
) {
    init {
        require(thresholdDegrees > 0f) { "thresholdDegrees must be positive: $thresholdDegrees" }
        require(requiredDwellMillis >= 0L) { "requiredDwellMillis must not be negative: $requiredDwellMillis" }
    }

    private var trackedTargetId: Int? = null
    private var dwellStartNanos: Long? = null
    private var hasFiredForCurrentDwell: Boolean = false

    fun reset() {
        trackedTargetId = null
        dwellStartNanos = null
        hasFiredForCurrentDwell = false
    }

    fun evaluate(
        orientation: OrientationData,
        target: SphereTarget?,
    ): AlignmentState {
        if (target == null || !orientation.hasFix || !orientation.accuracy.allowsCapture) {
            reset()
            return AlignmentState()
        }

        if (trackedTargetId != target.id) {
            trackedTargetId = target.id
            dwellStartNanos = null
            hasFiredForCurrentDwell = false
        }

        val distance = SphereProjection.project(orientation, target).angularDistanceDegrees
        if (distance > thresholdDegrees) {
            dwellStartNanos = null
            hasFiredForCurrentDwell = false
            return AlignmentState(
                distanceDegrees = distance,
                isWithinThreshold = false,
                dwellProgress = 0f,
                triggered = false,
            )
        }

        val sampleTime = orientation.timestampNanos
        val start = dwellStartNanos?.takeIf { sampleTime >= it } ?: sampleTime.also {
            dwellStartNanos = it
        }
        val elapsedMillis = (sampleTime - start) / NANOS_PER_MILLI
        val progress = if (requiredDwellMillis == 0L) {
            1f
        } else {
            (elapsedMillis.toFloat() / requiredDwellMillis).coerceIn(0f, 1f)
        }

        val shouldTrigger = progress >= 1f && !hasFiredForCurrentDwell
        if (shouldTrigger) {
            hasFiredForCurrentDwell = true
        }

        return AlignmentState(
            distanceDegrees = distance,
            isWithinThreshold = true,
            dwellProgress = progress,
            triggered = shouldTrigger,
        )
    }

    companion object {
        const val DEFAULT_THRESHOLD_DEGREES = 2f
        const val DEFAULT_DWELL_MILLIS = 300L
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
