package com.example.ui.scanner

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure 3D spherical geometry & projection calculations for 360° × 180° room scanning.
 */
object ScanGeometry {

    const val DEFAULT_HFOV_DEG = 63.0
    const val DEFAULT_VFOV_DEG = 48.0

    enum class ScanMode {
        FULL_SPHERE,
        QUICK_SCAN,
        AR_CORNER_SCAN
    }

    data class Ray(val x: Float, val y: Float, val z: Float)

    data class ScanTarget(
        val targetIndex: Int,
        val ringIndex: Int,
        val pitchDeg: Float,
        val headingDeg: Float,
        val isCap: Boolean = false,
        val capType: String? = null // "zenith" or "nadir"
    )

    /**
     * Converts longitude/latitude (radians) to a 3D unit ray vector.
     * Longitude [-PI, PI], Latitude [-PI/2, PI/2].
     */
    fun lonLatToRay(lonRad: Float, latRad: Float): Ray {
        val cosLat = cos(latRad)
        return Ray(
            x = cosLat * sin(lonRad),
            y = sin(latRad),
            z = cosLat * cos(lonRad)
        )
    }

    /**
     * Converts a 3D ray vector to longitude/latitude (radians).
     * Returns Pair(lonRad, latRad).
     */
    fun rayToLonLat(ray: Ray): Pair<Float, Float> {
        val len = Math.sqrt((ray.x * ray.x + ray.y * ray.y + ray.z * ray.z).toDouble()).toFloat()
        if (len < 1e-6f) return Pair(0f, 0f)
        val nx = ray.x / len
        val ny = ray.y / len
        val nz = ray.z / len

        val lat = asin(ny.coerceIn(-1f, 1f))
        val lon = atan2(nx, nz)
        return Pair(lon, lat)
    }

    /**
     * Calculates angular distance in degrees between two directions specified by heading and pitch.
     */
    fun angularDistanceDeg(h1Deg: Float, p1Deg: Float, h2Deg: Float, p2Deg: Float): Float {
        val r1 = lonLatToRay(Math.toRadians(h1Deg.toDouble()).toFloat(), Math.toRadians(p1Deg.toDouble()).toFloat())
        val r2 = lonLatToRay(Math.toRadians(h2Deg.toDouble()).toFloat(), Math.toRadians(p2Deg.toDouble()).toFloat())
        val dot = (r1.x * r2.x + r1.y * r2.y + r1.z * r2.z).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(dot.toDouble())).toFloat()
    }

    /**
     * Projects a 3D world ray into a camera frame defined by heading, pitch, hFOV, and vFOV.
     * Returns pixel coordinates (x, y) inside [0..frameW, 0..frameH] if visible, or null if outside FOV or behind camera.
     */
    fun projectRayIntoFrame(
        ray: Ray,
        frameHeadingDeg: Float,
        framePitchDeg: Float,
        hfovDeg: Float = DEFAULT_HFOV_DEG.toFloat(),
        vfovDeg: Float = DEFAULT_VFOV_DEG.toFloat(),
        frameW: Int = 800,
        frameH: Int = 600
    ): Pair<Float, Float>? {
        val hRad = Math.toRadians(frameHeadingDeg.toDouble()).toFloat()
        val pRad = Math.toRadians(framePitchDeg.toDouble()).toFloat()

        // Rotate ray by -heading around Y axis
        val cosH = cos(-hRad)
        val sinH = sin(-hRad)
        val rx1 = ray.x * cosH + ray.z * sinH
        val ry1 = ray.y
        val rz1 = -ray.x * sinH + ray.z * cosH

        // Rotate by -pitch around X axis
        val cosP = cos(-pRad)
        val sinP = sin(-pRad)
        val rx2 = rx1
        val ry2 = ry1 * cosP - rz1 * sinP
        val rz2 = ry1 * sinP + rz1 * cosP

        // If behind camera plane
        if (rz2 <= 1e-4f) return null

        val tanHalfH = tan(Math.toRadians((hfovDeg / 2.0))).toFloat()
        val tanHalfV = tan(Math.toRadians((vfovDeg / 2.0))).toFloat()

        val normX = (rx2 / rz2) / tanHalfH
        val normY = (ry2 / rz2) / tanHalfV

        if (abs(normX) > 1.2f || abs(normY) > 1.2f) return null

        val px = (normX + 1f) * 0.5f * frameW
        val py = (1f - normY) * 0.5f * frameH

        return Pair(px, py)
    }

    /**
     * Generates target grid points for scan modes:
     * Full Sphere:
     *   - Cap Zenith (+90° pitch, 1 shot)
     *   - Ring 1 (+70° pitch, 12 stops)
     *   - Ring 2 (+35° pitch, 12 stops)
     *   - Ring 3 (0° pitch, 12 stops)
     *   - Ring 4 (-35° pitch, 12 stops)
     *   - Ring 5 (-70° pitch, 12 stops)
     *   - Cap Nadir (-90° pitch, 1 shot)
     * Total: 62 shots.
     *
     * Quick Scan:
     *   - Ring 1 (+35° pitch, 8 stops)
     *   - Ring 2 (0° pitch, 8 stops)
     *   - Ring 3 (-35° pitch, 8 stops)
     * Total: 24 shots.
     */
    fun generateScanTargets(mode: ScanMode): List<ScanTarget> {
        val targets = mutableListOf<ScanTarget>()
        var globalIdx = 0

        when (mode) {
            ScanMode.FULL_SPHERE -> {
                // Cap Zenith (+90°)
                targets.add(ScanTarget(globalIdx++, ringIndex = 0, pitchDeg = 90f, headingDeg = 0f, isCap = true, capType = "zenith"))

                // Ring 1: +70°
                for (col in 0 until 12) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 1, pitchDeg = 70f, headingDeg = col * 30f))
                }
                // Ring 2: +35°
                for (col in 0 until 12) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 2, pitchDeg = 35f, headingDeg = col * 30f))
                }
                // Ring 3: 0°
                for (col in 0 until 12) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 3, pitchDeg = 0f, headingDeg = col * 30f))
                }
                // Ring 4: -35°
                for (col in 0 until 12) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 4, pitchDeg = -35f, headingDeg = col * 30f))
                }
                // Ring 5: -70°
                for (col in 0 until 12) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 5, pitchDeg = -70f, headingDeg = col * 30f))
                }

                // Cap Nadir (-90°)
                targets.add(ScanTarget(globalIdx++, ringIndex = 6, pitchDeg = -90f, headingDeg = 0f, isCap = true, capType = "nadir"))
            }

            ScanMode.QUICK_SCAN -> {
                // Classic 360° Horizontal Panorama (1 ring on horizon, 12 stops)
                for (col in 0 until 12) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 0, pitchDeg = 0f, headingDeg = col * 30f))
                }
            }

            ScanMode.AR_CORNER_SCAN -> {
                // AR 3D Room Corner Point-by-Point Mapping Mode
                for (col in 0 until 4) {
                    targets.add(ScanTarget(globalIdx++, ringIndex = 0, pitchDeg = -30f, headingDeg = col * 90f))
                }
            }
        }
        return targets
    }

    /**
     * Computes coverage percentage (0.0 .. 100.0) across a spherical lon/lat grid.
     * Takes list of captured frame orientations: Pair(headingDeg, pitchDeg).
     */
    fun computeCoveragePercent(
        capturedFrames: List<Pair<Float, Float>>,
        hfovDeg: Float = DEFAULT_HFOV_DEG.toFloat(),
        vfovDeg: Float = DEFAULT_VFOV_DEG.toFloat(),
        gridCols: Int = 36,
        gridRows: Int = 18
    ): Float {
        if (capturedFrames.isEmpty()) return 0f

        var coveredCells = 0
        val totalCells = gridCols * gridRows

        for (row in 0 until gridRows) {
            val latRad = Math.PI.toFloat() * (0.5f - (row + 0.5f) / gridRows)
            val latDeg = Math.toDegrees(latRad.toDouble()).toFloat()

            for (col in 0 until gridCols) {
                val lonRad = (2f * Math.PI.toFloat() * ((col + 0.5f) / gridCols)) - Math.PI.toFloat()
                val lonDeg = Math.toDegrees(lonRad.toDouble()).toFloat()

                // Check if any frame covers this (lonDeg, latDeg)
                var cellCovered = false
                for ((fHeading, fPitch) in capturedFrames) {
                    val dist = angularDistanceDeg(lonDeg, latDeg, fHeading, fPitch)
                    if (dist <= vfovDeg * 0.55f) {
                        cellCovered = true
                        break
                    }
                }
                if (cellCovered) coveredCells++
            }
        }

        return (coveredCells.toFloat() / totalCells) * 100f
    }
}
