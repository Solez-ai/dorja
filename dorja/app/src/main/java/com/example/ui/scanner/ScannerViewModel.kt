package com.example.ui.scanner

import com.example.scanner.CaptureState
import com.example.scanner.FrameData
import com.example.scanner.Orientation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * State machine for the 360 ring capture — the guided counterpart of the
 * reference app's manual Capture+Save flow.
 *
 * The ring seam-free design: 12 yaw targets evenly spaced 30 degrees apart,
 * starting from wherever the user is aiming when they press Start. The user
 * physically turns to each target; when the reticle is aligned and the phone
 * is steady for [HOLD_MS], the shutter fires by itself — the interactive
 * "where to point / when to take the shot" guidance the wrap-around capture
 * needs, instead of the reference's blind shutter button.
 */
class ScannerViewModel {

    companion object {
        const val TARGET_COUNT = 12
        const val RING_STEP_DEG = 360f / TARGET_COUNT
        const val YAW_TOLERANCE_DEG = 5f
        const val PITCH_TOLERANCE_DEG = 10f
        const val MAX_VELOCITY_DEG = 12f   // deg/s — steady enough to shoot
        const val HOLD_MS = 700L           // aligned-and-steady dwell before shutter
    }

    private val _captureState = MutableStateFlow(CaptureState.WAITING_FOR_INITIAL_ANCHOR)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private val _orientation = MutableStateFlow(Orientation())
    val orientation: StateFlow<Orientation> = _orientation.asStateFlow()

    private val _targetIndex = MutableStateFlow(0)
    val targetIndex: StateFlow<Int> = _targetIndex.asStateFlow()

    private val _captured = MutableStateFlow<List<FrameData>>(emptyList())
    val captured: StateFlow<List<FrameData>> = _captured.asStateFlow()

    private val _stitchStage = MutableStateFlow<String?>(null)
    val stitchStage: StateFlow<String?> = _stitchStage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Increments when the VM wants the screen to fire the shutter. */
    private val _captureTick = MutableStateFlow(0)
    val captureTick: StateFlow<Int> = _captureTick.asStateFlow()

    /** Burst counter exposed for the ghost overlay + rail. */
    private val _capturedCount = MutableStateFlow(0)
    val capturedCount: StateFlow<Int> = _capturedCount.asStateFlow()

    /** Re-triggers whenever a new frame lands so the ghost image reloads. */
    private val _ghostTick = MutableStateFlow(0)
    val ghostTick: StateFlow<Int> = _ghostTick.asStateFlow()
    val lastFramePath: String? get() = _captured.value.lastOrNull()?.path

    var startHeading: Float = Float.NaN
        private set

    // --- alignment internals -------------------------------------------------
    private var alignedSince: Long = 0L
    private var lastHeading: Float = Float.NaN
    private var lastHeadingAt: Long = 0L
    private var smoothedVelocity: Float = 0f

    val targets: List<Float>
        get() {
            val start = if (startHeading.isNaN()) 0f else startHeading
            return List(TARGET_COUNT) { i -> (start + i * RING_STEP_DEG) % 360f }
        }

    fun startScan() {
        startHeading = _orientation.value.headingDeg
        _targetIndex.value = 0
        _captured.value = emptyList()
        _capturedCount.value = 0
        _stitchStage.value = null
        _errorMessage.value = null
        alignedSince = 0L
        smoothedVelocity = 0f
        lastHeading = Float.NaN
        _captureState.value = CaptureState.MOVING_TO_TARGET
    }

    fun retryScan() {
        _captureState.value = CaptureState.WAITING_FOR_INITIAL_ANCHOR
        startHeading = _orientation.value.headingDeg
        _targetIndex.value = 0
        _captured.value = emptyList()
        _capturedCount.value = 0
        _stitchStage.value = null
        _errorMessage.value = null
    }

    fun updateOrientation(o: Orientation) {
        _orientation.value = o
        if (_captureState.value != CaptureState.MOVING_TO_TARGET) return

        // Smoothed angular velocity (deg/s) — steadiness gate before shooting.
        val now = System.currentTimeMillis()
        if (!lastHeading.isNaN()) {
            val raw = abs(wrapDelta(o.headingDeg - lastHeading)) * 1000f /
                (now - lastHeadingAt).coerceAtLeast(1L)
            // Low-pass keeps sensor jitter from flapping the toggle.
            smoothedVelocity = 0.7f * smoothedVelocity + 0.3f * raw
        }
        lastHeading = o.headingDeg
        lastHeadingAt = now

        val target = targets[_targetIndex.value]
        val yawErr = abs(wrapDelta(o.headingDeg - target))
        val aligned = yawErr <= YAW_TOLERANCE_DEG &&
            abs(o.pitchDeg) <= PITCH_TOLERANCE_DEG &&
            smoothedVelocity <= MAX_VELOCITY_DEG

        if (aligned) {
            if (alignedSince == 0L) alignedSince = now
            if (now - alignedSince >= HOLD_MS) {
                alignedSince = 0L
                _captureState.value = CaptureState.STABILIZING
                _captureTick.value += 1 // screen takes the shot and reports back
            }
        } else {
            alignedSince = 0L
        }
    }

    /** Hard failure path (capture IO error, camera driver hiccup). */
    fun captureStateCaptureFailed() {
        _captureState.value = CaptureState.ERROR
        _errorMessage.value = "scan_error_capture"
    }

    /** Manual shutter (button press) — bypasses the auto-align gate. */
    fun manualCapture() {
        if (_captureState.value == CaptureState.MOVING_TO_TARGET) {
            _captureState.value = CaptureState.STABILIZING
            _captureTick.value += 1
        }
    }

    fun onFrameCaptured(path: String) {
        val idx = _targetIndex.value
        val heading = targets.getOrNull(idx) ?: _orientation.value.headingDeg
        _captured.value = _captured.value + FrameData(
            path = path,
            heading = heading,
            pitchDeg = _orientation.value.pitchDeg,
            row = 0,
            col = idx
        )
        _capturedCount.value = _captured.value.size
        _ghostTick.value += 1

        val next = idx + 1
        if (next >= TARGET_COUNT) {
            _targetIndex.value = TARGET_COUNT - 1
            _captureState.value = CaptureState.COMPLETED
        } else {
            _targetIndex.value = next
            _captureState.value = CaptureState.MOVING_TO_TARGET
        }
    }

    fun setStitchStage(stage: String?) { _stitchStage.value = stage }
    fun setError(message: String?) { _errorMessage.value = message }

    /** Angular distance wrapped into [-180, 180]. */
    private fun wrapDelta(delta: Float): Float {
        var d = delta % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }
}
