package com.example.scanner

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for persisting scan session metadata in the local database.
 * Represents a single 360-degree room capture session.
 *
 * @param sessionId Unique identifier for this session (primary key, typically UUID)
 * @param roomName User-provided name for the room (e.g., "Master Bedroom", "Living Room")
 * @param propertyId Reference to the property being scanned
 * @param anchorHeading Compass heading of the first (anchor) frame, in degrees [0, 360)
 * @param frameCount Number of frames captured in this session
 * @param sensorMode Sensor mode used: GYRO_PRIMARY, ACCEL_MAG_FALLBACK, or UNAVAILABLE
 * @param panoramaPath File path to the final stitched panorama (null if stitching incomplete)
 * @param panoramaWidth Width of the panorama in pixels
 * @param panoramaHeight Height of the panorama in pixels
 * @param status Session status: PENDING, STITCHING, COMPLETED, ERROR
 * @param startTimestampMs Milliseconds since epoch when the session started
 * @param completionTimestampMs Milliseconds since epoch when the session completed (null if incomplete)
 */
@Entity(
    tableName = "scan_sessions",
    indices = [
        Index("propertyId"),
        Index("startTimestampMs")
    ]
)
data class ScanSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val roomName: String,
    val propertyId: String,
    val anchorHeading: Float,
    val frameCount: Int,
    val sensorMode: String, // GYRO_PRIMARY, ACCEL_MAG_FALLBACK, UNAVAILABLE
    val panoramaPath: String?,
    val panoramaWidth: Int,
    val panoramaHeight: Int,
    val status: String, // PENDING, STITCHING, COMPLETED, ERROR
    val startTimestampMs: Long,
    val completionTimestampMs: Long?
) {
    /**
     * Validates that the session data is internally consistent.
     *
     * @return true if all values are within expected ranges, false otherwise.
     */
    fun isValid(): Boolean {
        if (sessionId.isBlank()) return false
        if (roomName.isBlank()) return false
        if (propertyId.isBlank()) return false
        if (anchorHeading !in 0f..360f) return false
        if (frameCount < 0) return false
        if (sensorMode !in listOf("GYRO_PRIMARY", "ACCEL_MAG_FALLBACK", "UNAVAILABLE")) return false
        if (status !in listOf("PENDING", "STITCHING", "COMPLETED", "ERROR")) return false
        if (startTimestampMs <= 0) return false

        // If completed, panorama path should be set
        if (status == "COMPLETED" && panoramaPath.isNullOrBlank()) return false
        if (status == "COMPLETED" && panoramaWidth <= 0) return false
        if (status == "COMPLETED" && panoramaHeight <= 0) return false

        // If completed, completion timestamp should be set
        if (status == "COMPLETED" && completionTimestampMs == null) return false
        if (completionTimestampMs != null && completionTimestampMs < startTimestampMs) return false

        return true
    }

    companion object {
        /**
         * Creates a new session for a room being scanned.
         */
        fun create(
            sessionId: String,
            roomName: String,
            propertyId: String,
            anchorHeading: Float,
            sensorMode: String
        ): ScanSessionEntity {
            return ScanSessionEntity(
                sessionId = sessionId,
                roomName = roomName,
                propertyId = propertyId,
                anchorHeading = anchorHeading,
                frameCount = 0,
                sensorMode = sensorMode,
                panoramaPath = null,
                panoramaWidth = 0,
                panoramaHeight = 0,
                status = "PENDING",
                startTimestampMs = System.currentTimeMillis(),
                completionTimestampMs = null
            )
        }
    }
}
