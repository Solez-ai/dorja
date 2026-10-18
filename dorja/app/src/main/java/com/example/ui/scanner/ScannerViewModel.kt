package com.example.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scanner.AlignLevel
import com.example.scanner.CaptureState
import com.example.scanner.Orientation
import com.example.scanner.SensorFusionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScannerViewModel : ViewModel() {

    private val _captureState = MutableStateFlow(CaptureState.WAITING_FOR_INITIAL_ANCHOR)
    val captureState: StateFlow<CaptureState> = _captureState

    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom

    private val _alignLevel = MutableStateFlow(AlignLevel.RED)
    val alignLevel: StateFlow<AlignLevel> = _alignLevel

    private val _orientation = MutableStateFlow(Orientation())
    val orientation: StateFlow<Orientation> = _orientation

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount

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
        _captureState.value = CaptureState.WAITING_FOR_INITIAL_ANCHOR
        _frameCount.value = 0
        _alignLevel.value = AlignLevel.RED
    }
}
