package com.example.scanner.photosphere

internal object TargetSelection {

    const val CLAIM_RADIUS_DEGREES = 6f
    const val SUCCESSOR_BIAS_DEGREES = 12f

    fun chooseActiveTarget(
        remaining: List<SphereTarget>,
        currentActiveId: Int?,
        lastCaptured: SphereTarget?,
        orientation: OrientationData,
    ): SphereTarget? {
        if (remaining.isEmpty()) return null
        if (!orientation.hasFix) {
            return remaining.firstOrNull { it.id == currentActiveId } ?: remaining.first()
        }

        val nearest = remaining.minBy {
            SphereProjection.project(orientation, it).angularDistanceDegrees
        }
        val nearestDistance = SphereProjection.project(orientation, nearest).angularDistanceDegrees
        if (nearestDistance <= CLAIM_RADIUS_DEGREES) {
            return nearest
        }

        val current = remaining.firstOrNull { it.id == currentActiveId }
        if (current != null) {
            return current
        }

        if (lastCaptured != null) {
            return remaining.minBy { candidate ->
                val distance = SphereProjection.project(orientation, candidate).angularDistanceDegrees
                if (candidate.ringIndex == lastCaptured.ringIndex) {
                    distance - SUCCESSOR_BIAS_DEGREES
                } else {
                    distance
                }
            }
        }

        return nearest
    }
}
