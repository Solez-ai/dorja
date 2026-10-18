package com.example.scanner

import org.junit.Test
import org.junit.Before
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for scanner data models and enums.
 * Tests enum behavior, data class equality, validation logic, and serialization.
 */
class ScannerDataModelsTest {

    // ==================== CaptureState Tests ====================

    @Test
    fun captureState_allValuesExist() {
        val states = CaptureState.values()
        assertEquals(6, states.size)
        assertTrue(states.contains(CaptureState.WAITING_FOR_INITIAL_ANCHOR))
        assertTrue(states.contains(CaptureState.CAPTURING_INITIAL_FRAME))
        assertTrue(states.contains(CaptureState.MOVING_TO_TARGET))
        assertTrue(states.contains(CaptureState.STABILIZING))
        assertTrue(states.contains(CaptureState.COMPLETED))
        assertTrue(states.contains(CaptureState.ERROR))
    }

    @Test
    fun captureState_nameMapping() {
        assertEquals("WAITING_FOR_INITIAL_ANCHOR", CaptureState.WAITING_FOR_INITIAL_ANCHOR.name)
        assertEquals("CAPTURING_INITIAL_FRAME", CaptureState.CAPTURING_INITIAL_FRAME.name)
        assertEquals("MOVING_TO_TARGET", CaptureState.MOVING_TO_TARGET.name)
        assertEquals("STABILIZING", CaptureState.STABILIZING.name)
        assertEquals("COMPLETED", CaptureState.COMPLETED.name)
        assertEquals("ERROR", CaptureState.ERROR.name)
    }

    @Test
    fun captureState_valueOfParsing() {
        assertEquals(CaptureState.WAITING_FOR_INITIAL_ANCHOR, CaptureState.valueOf("WAITING_FOR_INITIAL_ANCHOR"))
        assertEquals(CaptureState.ERROR, CaptureState.valueOf("ERROR"))
    }

    // ==================== AlignLevel Tests ====================

    @Test
    fun alignLevel_allValuesExist() {
        val levels = AlignLevel.values()
        assertEquals(3, levels.size)
        assertTrue(levels.contains(AlignLevel.RED))
        assertTrue(levels.contains(AlignLevel.YELLOW))
        assertTrue(levels.contains(AlignLevel.GREEN))
    }

    @Test
    fun alignLevel_nameMapping() {
        assertEquals("RED", AlignLevel.RED.name)
        assertEquals("YELLOW", AlignLevel.YELLOW.name)
        assertEquals("GREEN", AlignLevel.GREEN.name)
    }

    @Test
    fun alignLevel_valueOfParsing() {
        assertEquals(AlignLevel.RED, AlignLevel.valueOf("RED"))
        assertEquals(AlignLevel.YELLOW, AlignLevel.valueOf("YELLOW"))
        assertEquals(AlignLevel.GREEN, AlignLevel.valueOf("GREEN"))
    }

    // ==================== SensorMode Tests ====================

    @Test
    fun sensorMode_allValuesExist() {
        val modes = SensorMode.values()
        assertEquals(3, modes.size)
        assertTrue(modes.contains(SensorMode.GYRO_PRIMARY))
        assertTrue(modes.contains(SensorMode.ACCEL_MAG_FALLBACK))
        assertTrue(modes.contains(SensorMode.UNAVAILABLE))
    }

    @Test
    fun sensorMode_nameMapping() {
        assertEquals("GYRO_PRIMARY", SensorMode.GYRO_PRIMARY.name)
        assertEquals("ACCEL_MAG_FALLBACK", SensorMode.ACCEL_MAG_FALLBACK.name)
        assertEquals("UNAVAILABLE", SensorMode.UNAVAILABLE.name)
    }

    @Test
    fun sensorMode_valueOfParsing() {
        assertEquals(SensorMode.GYRO_PRIMARY, SensorMode.valueOf("GYRO_PRIMARY"))
        assertEquals(SensorMode.UNAVAILABLE, SensorMode.valueOf("UNAVAILABLE"))
    }

    // ==================== Orientation Tests ====================

    @Test
    fun orientation_defaultValues() {
        val orientation = Orientation()
        assertEquals(0f, orientation.headingDeg)
        assertEquals(0f, orientation.pitchDeg)
        assertEquals(0f, orientation.rollDeg)
        assertTrue(orientation.isValid())
    }

    @Test
    fun orientation_customValues() {
        val orientation = Orientation(headingDeg = 90f, pitchDeg = 45f, rollDeg = -30f)
        assertEquals(90f, orientation.headingDeg)
        assertEquals(45f, orientation.pitchDeg)
        assertEquals(-30f, orientation.rollDeg)
        assertTrue(orientation.isValid())
    }

    @Test
    fun orientation_isValid_validRange() {
        assertTrue(Orientation(headingDeg = 0f, pitchDeg = -90f, rollDeg = -180f).isValid())
        assertTrue(Orientation(headingDeg = 180f, pitchDeg = 0f, rollDeg = 0f).isValid())
        assertTrue(Orientation(headingDeg = 359.9f, pitchDeg = 90f, rollDeg = 180f).isValid())
    }

    @Test
    fun orientation_isValid_outOfRange_heading() {
        assertFalse(Orientation(headingDeg = -1f, pitchDeg = 0f, rollDeg = 0f).isValid())
        assertFalse(Orientation(headingDeg = 360.1f, pitchDeg = 0f, rollDeg = 0f).isValid())
    }

    @Test
    fun orientation_isValid_outOfRange_pitch() {
        assertFalse(Orientation(headingDeg = 0f, pitchDeg = -90.1f, rollDeg = 0f).isValid())
        assertFalse(Orientation(headingDeg = 0f, pitchDeg = 90.1f, rollDeg = 0f).isValid())
    }

    @Test
    fun orientation_isValid_outOfRange_roll() {
        assertFalse(Orientation(headingDeg = 0f, pitchDeg = 0f, rollDeg = -180.1f).isValid())
        assertFalse(Orientation(headingDeg = 0f, pitchDeg = 0f, rollDeg = 180.1f).isValid())
    }

    @Test
    fun orientation_equality() {
        val o1 = Orientation(headingDeg = 90f, pitchDeg = 45f, rollDeg = -30f)
        val o2 = Orientation(headingDeg = 90f, pitchDeg = 45f, rollDeg = -30f)
        val o3 = Orientation(headingDeg = 90f, pitchDeg = 45f, rollDeg = -31f)

        assertEquals(o1, o2)
        assertTrue(o1 != o3)
    }

    // ==================== FrameData Tests ====================

    @Test
    fun frameData_validFrame() {
        val frame = FrameData(
            path = "/cache/frame_r0_c0_12345.jpg",
            heading = 90f,
            pitchDeg = 0f,
            row = 0,
            col = 0,
            isCap = false,
            capType = null
        )
        assertTrue(frame.isValid())
    }

    @Test
    fun frameData_isValid_emptyPath() {
        val frame = FrameData(
            path = "",
            heading = 90f,
            pitchDeg = 0f,
            row = 0,
            col = 0,
            isCap = false,
            capType = null
        )
        assertFalse(frame.isValid())
    }

    @Test
    fun frameData_isValid_outOfRangeHeading() {
        val frame = FrameData(
            path = "/cache/frame.jpg",
            heading = -1f,
            pitchDeg = 0f,
            row = 0,
            col = 0,
            isCap = false,
            capType = null
        )
        assertFalse(frame.isValid())
    }

    @Test
    fun frameData_isValid_outOfRangePitch() {
        val frame = FrameData(
            path = "/cache/frame.jpg",
            heading = 90f,
            pitchDeg = 100f,
            row = 0,
            col = 0,
            isCap = false,
            capType = null
        )
        assertFalse(frame.isValid())
    }

    @Test
    fun frameData_isValid_negativeIndices() {
        val frame = FrameData(
            path = "/cache/frame.jpg",
            heading = 90f,
            pitchDeg = 0f,
            row = -1,
            col = 0,
            isCap = false,
            capType = null
        )
        assertFalse(frame.isValid())
    }

    @Test
    fun frameData_isValid_ceilingCap() {
        val frame = FrameData(
            path = "/cache/frame_cap.jpg",
            heading = 0f,
            pitchDeg = 90f,
            row = 0,
            col = 12,
            isCap = true,
            capType = "ceiling"
        )
        assertTrue(frame.isValid())
    }

    @Test
    fun frameData_isValid_floorCap() {
        val frame = FrameData(
            path = "/cache/frame_cap.jpg",
            heading = 0f,
            pitchDeg = -90f,
            row = 0,
            col = 13,
            isCap = true,
            capType = "floor"
        )
        assertTrue(frame.isValid())
    }

    @Test
    fun frameData_isValid_invalidCapType() {
        val frame = FrameData(
            path = "/cache/frame_cap.jpg",
            heading = 0f,
            pitchDeg = 90f,
            row = 0,
            col = 12,
            isCap = true,
            capType = "wall"  // Invalid
        )
        assertFalse(frame.isValid())
    }

    @Test
    fun frameData_isValid_capTypeWithoutFlag() {
        val frame = FrameData(
            path = "/cache/frame.jpg",
            heading = 90f,
            pitchDeg = 0f,
            row = 0,
            col = 0,
            isCap = false,
            capType = "ceiling"  // Should be null when isCap=false
        )
        assertFalse(frame.isValid())
    }

    @Test
    fun frameData_equality() {
        val f1 = FrameData(path = "/cache/frame.jpg", heading = 90f, pitchDeg = 0f)
        val f2 = FrameData(path = "/cache/frame.jpg", heading = 90f, pitchDeg = 0f)
        val f3 = FrameData(path = "/cache/frame.jpg", heading = 91f, pitchDeg = 0f)

        assertEquals(f1, f2)
        assertTrue(f1 != f3)
    }

    // ==================== ZoomCapabilities Tests ====================

    @Test
    fun zoomCapabilities_validCapabilities() {
        val cap = ZoomCapabilities(
            minZoomRatio = 0.5f,
            maxZoomRatio = 4.0f,
            defaultZoom = 1.0f,
            hasUltraWide = true,
            hasStandard = true,
            hasTelephoto = false
        )
        assertTrue(cap.isValid())
    }

    @Test
    fun zoomCapabilities_isValid_minZoomZero() {
        val cap = ZoomCapabilities(
            minZoomRatio = 0f,
            maxZoomRatio = 4.0f,
            defaultZoom = 1.0f,
            hasUltraWide = true,
            hasStandard = true,
            hasTelephoto = false
        )
        assertFalse(cap.isValid())
    }

    @Test
    fun zoomCapabilities_isValid_minGreaterThanMax() {
        val cap = ZoomCapabilities(
            minZoomRatio = 5.0f,
            maxZoomRatio = 4.0f,
            defaultZoom = 4.5f,
            hasUltraWide = true,
            hasStandard = true,
            hasTelephoto = false
        )
        assertFalse(cap.isValid())
    }

    @Test
    fun zoomCapabilities_isValid_defaultOutOfRange() {
        val cap = ZoomCapabilities(
            minZoomRatio = 1.0f,
            maxZoomRatio = 4.0f,
            defaultZoom = 5.0f,
            hasUltraWide = true,
            hasStandard = true,
            hasTelephoto = false
        )
        assertFalse(cap.isValid())
    }

    @Test
    fun zoomCapabilities_isValid_noLensAvailable() {
        val cap = ZoomCapabilities(
            minZoomRatio = 1.0f,
            maxZoomRatio = 4.0f,
            defaultZoom = 1.0f,
            hasUltraWide = false,
            hasStandard = false,
            hasTelephoto = false
        )
        assertFalse(cap.isValid())
    }

    @Test
    fun zoomCapabilities_createDefault() {
        val cap = ZoomCapabilities.createDefault()
        assertEquals(1.0f, cap.minZoomRatio)
        assertEquals(2.0f, cap.maxZoomRatio)
        assertEquals(1.0f, cap.defaultZoom)
        assertTrue(cap.hasStandard)
        assertFalse(cap.hasUltraWide)
        assertFalse(cap.hasTelephoto)
        assertTrue(cap.isValid())
    }

    // ==================== ScanGeometry.ScanTarget Tests ====================

    @Test
    fun scanTarget_validTarget() {
        val target = ScanGeometry.ScanTarget(
            targetIndex = 0,
            ringIndex = 0,
            pitchDeg = 0f,
            headingDeg = 90f
        )
        assertTrue(target.isValid())
    }

    @Test
    fun scanTarget_isValid_negativeTargetIndex() {
        val target = ScanGeometry.ScanTarget(
            targetIndex = -1,
            ringIndex = 0,
            pitchDeg = 0f,
            headingDeg = 90f
        )
        assertFalse(target.isValid())
    }

    @Test
    fun scanTarget_isValid_invalidPitch() {
        val target = ScanGeometry.ScanTarget(
            targetIndex = 0,
            ringIndex = 0,
            pitchDeg = 100f,
            headingDeg = 90f
        )
        assertFalse(target.isValid())
    }

    @Test
    fun scanTarget_isValid_invalidHeading() {
        val target = ScanGeometry.ScanTarget(
            targetIndex = 0,
            ringIndex = 0,
            pitchDeg = 0f,
            headingDeg = 400f
        )
        assertFalse(target.isValid())
    }

    // ==================== FrameCacheEntity Tests ====================

    @Test
    fun frameCacheEntity_roundTripConversion() {
        val frameData = FrameData(
            path = "/cache/frame_r0_c0_12345.jpg",
            heading = 90f,
            pitchDeg = 0f,
            row = 0,
            col = 0,
            isCap = false,
            capType = null
        )

        val entity = FrameCacheEntity.fromFrameData(
            frameId = "frame_001",
            sessionId = "session_001",
            frameData = frameData,
            rollDeg = 0f,
            fileSize = 1024000
        )

        val restored = entity.toFrameData()
        assertEquals(frameData.path, restored.path)
        assertEquals(frameData.heading, restored.heading)
        assertEquals(frameData.pitchDeg, restored.pitchDeg)
        assertEquals(frameData.row, restored.row)
        assertEquals(frameData.col, restored.col)
        assertEquals(frameData.isCap, restored.isCap)
        assertEquals(frameData.capType, restored.capType)
    }

    // ==================== ScanSessionEntity Tests ====================

    @Test
    fun scanSessionEntity_isValid_completedSession() {
        val entity = ScanSessionEntity(
            sessionId = "session_001",
            roomName = "Living Room",
            propertyId = "property_001",
            anchorHeading = 0f,
            frameCount = 12,
            sensorMode = "GYRO_PRIMARY",
            panoramaPath = "/cache/panorama_001.jpg",
            panoramaWidth = 4096,
            panoramaHeight = 2048,
            status = "COMPLETED",
            startTimestampMs = 1000,
            completionTimestampMs = 2000
        )
        assertTrue(entity.isValid())
    }

    @Test
    fun scanSessionEntity_isValid_pendingSession() {
        val entity = ScanSessionEntity(
            sessionId = "session_001",
            roomName = "Living Room",
            propertyId = "property_001",
            anchorHeading = 0f,
            frameCount = 0,
            sensorMode = "GYRO_PRIMARY",
            panoramaPath = null,
            panoramaWidth = 0,
            panoramaHeight = 0,
            status = "PENDING",
            startTimestampMs = 1000,
            completionTimestampMs = null
        )
        assertTrue(entity.isValid())
    }

    @Test
    fun scanSessionEntity_isValid_blankSessionId() {
        val entity = ScanSessionEntity(
            sessionId = "",
            roomName = "Living Room",
            propertyId = "property_001",
            anchorHeading = 0f,
            frameCount = 12,
            sensorMode = "GYRO_PRIMARY",
            panoramaPath = null,
            panoramaWidth = 0,
            panoramaHeight = 0,
            status = "PENDING",
            startTimestampMs = 1000,
            completionTimestampMs = null
        )
        assertFalse(entity.isValid())
    }

    @Test
    fun scanSessionEntity_isValid_invalidSensorMode() {
        val entity = ScanSessionEntity(
            sessionId = "session_001",
            roomName = "Living Room",
            propertyId = "property_001",
            anchorHeading = 0f,
            frameCount = 12,
            sensorMode = "INVALID_MODE",
            panoramaPath = null,
            panoramaWidth = 0,
            panoramaHeight = 0,
            status = "PENDING",
            startTimestampMs = 1000,
            completionTimestampMs = null
        )
        assertFalse(entity.isValid())
    }

    @Test
    fun scanSessionEntity_isValid_completedMissingPanorama() {
        val entity = ScanSessionEntity(
            sessionId = "session_001",
            roomName = "Living Room",
            propertyId = "property_001",
            anchorHeading = 0f,
            frameCount = 12,
            sensorMode = "GYRO_PRIMARY",
            panoramaPath = null,  // Missing for COMPLETED status
            panoramaWidth = 0,
            panoramaHeight = 0,
            status = "COMPLETED",
            startTimestampMs = 1000,
            completionTimestampMs = 2000
        )
        assertFalse(entity.isValid())
    }

    @Test
    fun scanSessionEntity_isValid_invalidTimestampOrder() {
        val entity = ScanSessionEntity(
            sessionId = "session_001",
            roomName = "Living Room",
            propertyId = "property_001",
            anchorHeading = 0f,
            frameCount = 12,
            sensorMode = "GYRO_PRIMARY",
            panoramaPath = "/cache/panorama.jpg",
            panoramaWidth = 4096,
            panoramaHeight = 2048,
            status = "COMPLETED",
            startTimestampMs = 2000,
            completionTimestampMs = 1000  // Earlier than start
        )
        assertFalse(entity.isValid())
    }

    @Test
    fun scanSessionEntity_create() {
        val entity = ScanSessionEntity.create(
            sessionId = "session_001",
            roomName = "Master Bedroom",
            propertyId = "property_001",
            anchorHeading = 45f,
            sensorMode = "GYRO_PRIMARY"
        )

        assertEquals("session_001", entity.sessionId)
        assertEquals("Master Bedroom", entity.roomName)
        assertEquals("property_001", entity.propertyId)
        assertEquals(45f, entity.anchorHeading)
        assertEquals(0, entity.frameCount)
        assertEquals("GYRO_PRIMARY", entity.sensorMode)
        assertEquals("PENDING", entity.status)
        assertTrue(entity.startTimestampMs > 0)
    }
}
