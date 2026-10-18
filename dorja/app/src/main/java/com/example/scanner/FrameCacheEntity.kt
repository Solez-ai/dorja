package com.example.scanner

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Room entity for persisting captured frame metadata in the local database.
 * Used for frame cache management, session recovery, and panorama stitching.
 *
 * @param frameId Unique identifier for this frame (primary key, typically UUID or timestamp-based)
 * @param sessionId Reference to the parent ScanSessionEntity
 * @param filePath File system path to the JPEG image (e.g., /cache/frame_r0_c0_xxx.jpg)
 * @param heading Compass heading at capture time, in degrees [0, 360)
 * @param pitchDeg Pitch angle at capture time, in degrees [-90, 90]
 * @param rollDeg Roll angle at capture time, in degrees [-180, 180]
 * @param rowIndex Ring index for multi-row scans (typically 0 for flat scans)
 * @param colIndex Frame index within the ring (0-11 for 12-frame scans)
 * @param isCap true if this frame captures ceiling or floor
 * @param capType Type of cap: "ceiling", "floor", or null for regular frames
 * @param captureTimestampMs Milliseconds since epoch when the frame was captured
 * @param fileSize File size in bytes
 */
@Entity(
    tableName = "frame_cache",
    indices = [
        Index("sessionId"),
        Index("captureTimestampMs")
    ]
)
data class FrameCacheEntity(
    @PrimaryKey
    val frameId: String,
    val sessionId: String,
    val filePath: String,
    val heading: Float,
    val pitchDeg: Float,
    val rollDeg: Float,
    val rowIndex: Int,
    val colIndex: Int,
    val isCap: Boolean,
    val capType: String?,
    val captureTimestampMs: Long,
    val fileSize: Long
) {
    /**
     * Converts this entity to a [FrameData] domain object.
     */
    fun toFrameData(): FrameData {
        return FrameData(
            path = filePath,
            heading = heading,
            pitchDeg = pitchDeg,
            row = rowIndex,
            col = colIndex,
            isCap = isCap,
            capType = capType
        )
    }

    companion object {
        /**
         * Creates a [FrameCacheEntity] from a [FrameData] domain object.
         */
        fun fromFrameData(
            frameId: String,
            sessionId: String,
            frameData: FrameData,
            rollDeg: Float,
            fileSize: Long
        ): FrameCacheEntity {
            return FrameCacheEntity(
                frameId = frameId,
                sessionId = sessionId,
                filePath = frameData.path,
                heading = frameData.heading,
                pitchDeg = frameData.pitchDeg,
                rollDeg = rollDeg,
                rowIndex = frameData.row,
                colIndex = frameData.col,
                isCap = frameData.isCap,
                capType = frameData.capType,
                captureTimestampMs = System.currentTimeMillis(),
                fileSize = fileSize
            )
        }
    }
}
