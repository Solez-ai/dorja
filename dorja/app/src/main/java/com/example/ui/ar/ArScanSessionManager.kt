package com.example.ui.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableException

data class DetectedPlaneInfo(
    val id: String,
    val type: PlaneType,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
)

enum class PlaneType {
    FLOOR,
    WALL,
    CEILING,
    UNKNOWN
}

data class PointCloudPoint(
    val x: Float,
    val y: Float,
    val confidence: Float
)

class ArScanSessionManager(private val context: Context) {
    var session: Session? = null
        private set
    var isArCoreSupported = false
        private set

    fun initialize(): Boolean {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            if (availability.isSupported) {
                isArCoreSupported = true
                val newSession = Session(context)
                val config = Config(newSession).apply {
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                }
                newSession.configure(config)
                session = newSession
                return true
            }
        } catch (e: UnavailableException) {
            isArCoreSupported = false
        } catch (e: Exception) {
            isArCoreSupported = false
        }
        return false
    }

    fun resume() {
        try {
            session?.resume()
        } catch (e: Exception) {
            // Handled
        }
    }

    fun pause() {
        try {
            session?.pause()
        } catch (e: Exception) {
            // Handled
        }
    }

    fun destroy() {
        try {
            session?.close()
            session = null
        } catch (e: Exception) {
            // Handled
        }
    }

    companion object {
        fun calculateCoverage(
            floorPlanesCount: Int,
            wallPlanesCount: Int,
            frameCount: Int,
            elapsedSeconds: Float
        ): Float {
            val floorScore = if (floorPlanesCount > 0) 25f else 0f
            val wallScore = (wallPlanesCount.coerceAtMost(4) / 4f) * 40f
            val frameScore = (frameCount.coerceAtMost(250) / 250f) * 20f
            val timeScore = (elapsedSeconds.coerceAtMost(15f) / 15f) * 15f
            return (floorScore + wallScore + frameScore + timeScore).coerceAtMost(98.5f)
        }

        fun getGuidance(coverage: Float): String {
            return when {
                coverage < 15f -> "Point at the floor to detect ground plane"
                coverage < 25f -> "Hold steady — detecting surface geometries"
                coverage < 40f -> "Slowly pan LEFT to scan the left wall"
                coverage < 55f -> "Now pan RIGHT to scan the right wall"
                coverage < 70f -> "Tilt UP to capture ceiling and upper bounds"
                coverage < 80f -> "Tilt DOWN to capture floor details and corners"
                coverage < 90f -> "Move FORWARD slowly across the room"
                coverage < 95f -> "Almost complete — filling remaining geometry gaps"
                else -> "Reality Passport coverage complete!"
            }
        }
    }
}
