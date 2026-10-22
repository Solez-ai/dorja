package com.example.scanner.photosphere

import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

data class SphereTarget(
    val id: Int,
    val ringIndex: Int,
    val yawDegrees: Float,
    val elevationDegrees: Float,
) {
    val pitchDegrees: Float get() = -elevationDegrees

    val directionX: Float
    val directionY: Float
    val directionZ: Float

    init {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        val elevation = Math.toRadians(elevationDegrees.toDouble())
        val cosElevation = cos(elevation)
        directionX = (sin(yaw) * cosElevation).toFloat()
        directionY = (cos(yaw) * cosElevation).toFloat()
        directionZ = sin(elevation).toFloat()
    }
}

enum class SphereCaptureScope {
    FullSphere,
    Ring,
}

object SphereTargetGrid {

    const val DEFAULT_OVERLAP_FRACTION = 0.35f
    private const val STEP_SAFETY_FACTOR = 0.9f
    private const val MIN_FOV_DEGREES = 15f
    private const val MAX_FOV_DEGREES = 120f
    private const val MAX_OVERLAP_FRACTION = 0.75f
    private const val MIN_RING_TARGETS = 4
    private const val POLE_DEGREES = 90f

    fun generate(
        fieldOfView: FieldOfView = FieldOfView.TYPICAL_PORTRAIT,
        overlapFraction: Float = DEFAULT_OVERLAP_FRACTION,
        scope: SphereCaptureScope = SphereCaptureScope.FullSphere,
    ): List<SphereTarget> {
        val horizontalFov = fieldOfView.horizontalDegrees.coerceIn(MIN_FOV_DEGREES, MAX_FOV_DEGREES)
        val verticalFov = fieldOfView.verticalDegrees.coerceIn(MIN_FOV_DEGREES, MAX_FOV_DEGREES)
        val overlap = overlapFraction.coerceIn(0f, MAX_OVERLAP_FRACTION)

        val horizontalStep = horizontalFov * (1f - overlap) * STEP_SAFETY_FACTOR
        val verticalStep = verticalFov * (1f - overlap) * STEP_SAFETY_FACTOR

        if (scope == SphereCaptureScope.Ring) {
            return generateRing(
                startId = 0,
                ringIndex = 0,
                elevationDegrees = 0f,
                horizontalStepDegrees = horizontalStep,
            )
        }

        val bandsAboveHorizon = max(1, ceil((POLE_DEGREES / verticalStep).toDouble()).toInt())
        val ringElevations = buildList {
            for (band in -bandsAboveHorizon + 1 until bandsAboveHorizon) {
                add(band * (POLE_DEGREES / bandsAboveHorizon))
            }
        }.sortedBy { kotlin.math.abs(it) }

        val targets = ArrayList<SphereTarget>()
        var nextId = 0
        var ringIndex = 0

        for (elevation in ringElevations) {
            val ringTargets = generateRing(
                startId = nextId,
                ringIndex = ringIndex,
                elevationDegrees = elevation,
                horizontalStepDegrees = horizontalStep,
            )
            targets.addAll(ringTargets)
            nextId += ringTargets.size
            ringIndex++
        }

        targets.add(SphereTarget(id = nextId++, ringIndex = ringIndex++, yawDegrees = 0f, elevationDegrees = POLE_DEGREES))
        targets.add(SphereTarget(id = nextId, ringIndex = ringIndex, yawDegrees = 0f, elevationDegrees = -POLE_DEGREES))

        return targets
    }

    private fun generateRing(
        startId: Int,
        ringIndex: Int,
        elevationDegrees: Float,
        horizontalStepDegrees: Float,
    ): List<SphereTarget> {
        val circumferenceScale = cos(Math.toRadians(elevationDegrees.toDouble())).toFloat()
        val count = max(
            MIN_RING_TARGETS,
            ceil(((360f * circumferenceScale) / horizontalStepDegrees).toDouble()).roundToInt(),
        )
        val step = 360f / count
        return List(count) { index ->
            SphereTarget(
                id = startId + index,
                ringIndex = ringIndex,
                yawDegrees = normalizeDegrees(index * step),
                elevationDegrees = elevationDegrees,
            )
        }
    }
}
