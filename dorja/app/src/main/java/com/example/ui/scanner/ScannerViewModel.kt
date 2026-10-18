package com.example.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanner.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScannerViewModel : ViewModel() {

    // NEW: Sweep panorama state
    private val _sweepState = MutableStateFlow(SweepCaptureState.READY_TO_START)
    val sweepState: StateFlow<SweepCaptureState> = _sweepState

    private val _sweepProgress = MutableStateFlow(0f)
    val sweepProgress: StateFlow<Float> = _sweepProgress

    private val _sweepDirection = MutableStateFlow(SweepDirection.LEFT_TO_RIGHT)
    val sweepDirection: StateFlow<SweepDirection> = _sweepDirection

    private val _motionVelocity = MutableStateFlow(0f)
    val motionVelocity: StateFlow<Float> = _motionVelocity

    private val _stripsCaptured = MutableStateFlow(0)
    val stripsCaptured: StateFlow<Int> = _stripsCaptured

    // LEGACY: Keep for backward compatibility during migration
    private val _captureState = MutableStateFlow(CaptureState.WAITING_FOR_INITIAL_ANCHOR)
    val captureState: StateFlow<CaptureState> = _captureState

    private val _alignLevel = MutableStateFlow(AlignLevel.RED)
    val alignLevel: StateFlow<AlignLevel> = _alignLevel

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount

    // Common state
    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom

    private val _orientation = MutableStateFlow(Orientation())
    val orientation: StateFlow<Orientation> = _orientation

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // NEW: Sweep panorama functions
    fun updateSweepState(state: SweepCaptureState) {
        _sweepState.value = state
    }

    fun updateSweepProgress(progress: Float) {
        _sweepProgress.value = progress.coerceIn(0f, 1f)
    }

    fun updateSweepDirection(direction: SweepDirection) {
        _sweepDirection.value = direction
    }

    fun updateMotionVelocity(velocity: Float) {
        _motionVelocity.value = velocity
    }

    fun incrementStripsCaptured() {
        _stripsCaptured.value += 1
    }

    fun setError(message: String) {
        _errorMessage.value = message
        _sweepState.value = SweepCaptureState.ERROR
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun startNewSweep() {
        _sweepState.value = SweepCaptureState.READY_TO_START
        _sweepProgress.value = 0f
        _stripsCaptured.value = 0
        _motionVelocity.value = 0f
        _errorMessage.value = null
    }

    fun pauseSweep() {
        if (_sweepState.value == SweepCaptureState.SWEEPING_IN_PROGRESS) {
            _sweepState.value = SweepCaptureState.PAUSED
        }
    }

    fun resumeSweep() {
        if (_sweepState.value == SweepCaptureState.PAUSED) {
            _sweepState.value = SweepCaptureState.SWEEPING_IN_PROGRESS
        }
    }

    fun completeSweep() {
        _sweepState.value = SweepCaptureState.SWEEP_COMPLETE
        _sweepProgress.value = 1f
    }

    // LEGACY: Keep for backward compatibility
    fun updateCaptureState(state: CaptureState) {
        _captureState.value = state
    }

    fun updateZoom(zoom: Float) {
        _currentZoom.value = zoom
    }

    fun updateAlignLevel(level: AlignLevel) {
        _alignLevel.value = level
    }

    fun updateOrientation(orientation: Orientation) {
        _orientation.value = orientation
    }

    fun incrementFrameCount() {
        _frameCount.value += 1
    }

    fun resetScan() {
        // Legacy reset
        _captureState.value = CaptureState.WAITING_FOR_INITIAL_ANCHOR
        _frameCount.value = 0
        _alignLevel.value = AlignLevel.RED
        
        // New sweep reset
        startNewSweep()
    }
}

