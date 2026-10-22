package com.example.scanner.photosphere

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class CapturedFrame(
    val target: SphereTarget,
    val orientation: OrientationData,
    val imageBytes: ByteArray = ByteArray(0),
    val capturedAtMillis: Long,
    val imageFile: File? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CapturedFrame) return false
        return target == other.target &&
            orientation == other.orientation &&
            capturedAtMillis == other.capturedAtMillis &&
            imageFile?.absolutePath == other.imageFile?.absolutePath &&
            imageBytes.contentEquals(other.imageBytes)
    }

    override fun hashCode(): Int {
        var result = target.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + (imageFile?.absolutePath?.hashCode() ?: 0)
        result = 31 * result + imageBytes.contentHashCode()
        result = 31 * result + capturedAtMillis.hashCode()
        return result
    }
}

data class SphereCaptureSessionState(
    val scope: SphereCaptureScope = SphereCaptureScope.FullSphere,
    val targets: List<SphereTarget> = emptyList(),
    val capturedFrames: List<CapturedFrame> = emptyList(),
    val activeTargetId: Int? = null,
    val alignment: AlignmentState = AlignmentState(),
    val isCapturingFrame: Boolean = false,
) {
    private val capturedIds: Set<Int> = capturedFrames.mapTo(HashSet()) { it.target.id }

    val activeTarget: SphereTarget? =
        targets.firstOrNull { it.id == activeTargetId && it.id !in capturedIds }
            ?: targets.firstOrNull { it.id !in capturedIds }

    val remainingCount: Int get() = (targets.size - capturedIds.size).coerceAtLeast(0)
    val isComplete: Boolean get() = targets.isNotEmpty() && remainingCount == 0
    val canUndo: Boolean get() = capturedFrames.isNotEmpty() && !isCapturingFrame
    val canFinishEarly: Boolean get() = capturedFrames.size >= MIN_FRAMES_TO_STITCH && !isCapturingFrame

    fun isCaptured(target: SphereTarget): Boolean = target.id in capturedIds

    companion object {
        const val MIN_FRAMES_TO_STITCH = 3
    }
}

class SphereCaptureCoordinator(
    fieldOfView: FieldOfView = FieldOfView.TYPICAL_PORTRAIT,
    private val overlapFraction: Float = SphereTargetGrid.DEFAULT_OVERLAP_FRACTION,
    defaultScope: SphereCaptureScope = SphereCaptureScope.FullSphere,
    thresholdDegrees: Float = AlignmentGate.DEFAULT_THRESHOLD_DEGREES,
    requiredDwellMillis: Long = AlignmentGate.DEFAULT_DWELL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var fieldOfView: FieldOfView = fieldOfView
    private var scope: SphereCaptureScope = defaultScope
    private var baseTargets: List<SphereTarget> =
        SphereTargetGrid.generate(fieldOfView, overlapFraction, scope)

    private val gate = AlignmentGate(
        thresholdDegrees = thresholdDegrees,
        requiredDwellMillis = requiredDwellMillis,
    )

    private val _state = MutableStateFlow(SphereCaptureSessionState(scope = scope, targets = baseTargets))
    val state: StateFlow<SphereCaptureSessionState> = _state.asStateFlow()

    private var isAnchored = false
    private val dwellSamples = ArrayList<OrientationData>()

    fun onFieldOfViewChanged(newFieldOfView: FieldOfView) {
        if (newFieldOfView == fieldOfView) return
        fieldOfView = newFieldOfView
        if (_state.value.capturedFrames.isNotEmpty()) return
        rebuildFreshPlan()
    }

    fun setScope(newScope: SphereCaptureScope) {
        if (newScope == scope) return
        scope = newScope
        if (_state.value.capturedFrames.isNotEmpty()) return
        rebuildFreshPlan()
    }

    private fun rebuildFreshPlan() {
        baseTargets = SphereTargetGrid.generate(fieldOfView, overlapFraction, scope)
        isAnchored = false
        dwellSamples.clear()
        gate.reset()
        _state.value = SphereCaptureSessionState(scope = scope, targets = baseTargets)
    }

    fun selectTarget(targetId: Int) {
        val current = _state.value
        if (current.isCapturingFrame) return
        val chosen = current.targets.firstOrNull { it.id == targetId && !current.isCaptured(it) }
            ?: return
        if (chosen.id == current.activeTargetId) return
        dwellSamples.clear()
        gate.reset()
        _state.value = current.copy(activeTargetId = chosen.id, alignment = AlignmentState())
    }

    fun onOrientationSample(orientation: OrientationData): SphereTarget? {
        if (!isAnchored && orientation.hasFix) {
            anchorPlanTo(orientation.yawDegrees)
        }

        val current = _state.value
        if (current.isCapturingFrame || current.isComplete) return null

        val remaining = current.targets.filterNot { current.isCaptured(it) }
        val target = TargetSelection.chooseActiveTarget(
            remaining = remaining,
            currentActiveId = current.activeTargetId,
            lastCaptured = current.capturedFrames.lastOrNull()?.target,
            orientation = orientation,
        )
        if (target?.id != current.activeTargetId) {
            dwellSamples.clear()
        }

        val alignment = gate.evaluate(orientation, target)

        if (alignment.isWithinThreshold) {
            dwellSamples.add(orientation)
        } else {
            dwellSamples.clear()
        }

        _state.value = current.copy(
            activeTargetId = target?.id,
            alignment = alignment,
            isCapturingFrame = alignment.triggered,
        )
        return if (alignment.triggered) target else null
    }

    fun onFrameCaptured(
        target: SphereTarget,
        imageBytes: ByteArray = ByteArray(0),
        orientationAtCapture: OrientationData,
        imageFile: File? = null,
    ): CapturedFrame? {
        val current = _state.value
        if (!current.isCapturingFrame || current.activeTarget?.id != target.id) return null

        val samples = if (dwellSamples.isNotEmpty()) {
            dwellSamples + orientationAtCapture
        } else {
            listOf(orientationAtCapture)
        }
        val frame = CapturedFrame(
            target = target,
            orientation = meanOrientation(samples),
            imageBytes = imageBytes,
            capturedAtMillis = clock(),
            imageFile = imageFile,
        )
        val updatedFrames = current.capturedFrames + frame
        val capturedIds = updatedFrames.mapTo(HashSet()) { it.target.id }
        val remaining = current.targets.filterNot { it.id in capturedIds }
        val nextActive = TargetSelection.chooseActiveTarget(
            remaining = remaining,
            currentActiveId = null,
            lastCaptured = target,
            orientation = orientationAtCapture,
        )

        dwellSamples.clear()
        gate.reset()
        _state.value = current.copy(
            capturedFrames = updatedFrames,
            activeTargetId = nextActive?.id,
            alignment = AlignmentState(),
            isCapturingFrame = false,
        )
        return frame
    }

    fun onFrameCaptureFailed() {
        val current = _state.value
        if (!current.isCapturingFrame) return
        dwellSamples.clear()
        gate.reset()
        _state.value = current.copy(
            alignment = AlignmentState(),
            isCapturingFrame = false,
        )
    }

    fun undoLastCapture(): CapturedFrame? {
        val current = _state.value
        if (!current.canUndo) return null
        val undone = current.capturedFrames.last()
        runCatching { undone.imageFile?.delete() }
        dwellSamples.clear()
        gate.reset()
        _state.value = current.copy(
            capturedFrames = current.capturedFrames.dropLast(1),
            activeTargetId = undone.target.id,
            alignment = AlignmentState(),
        )
        return undone
    }

    fun restart() {
        val oldFrames = _state.value.capturedFrames
        oldFrames.forEach { frame ->
            runCatching { frame.imageFile?.delete() }
        }
        isAnchored = false
        dwellSamples.clear()
        gate.reset()
        _state.value = SphereCaptureSessionState(scope = scope, targets = baseTargets)
    }

    private fun anchorPlanTo(headingDegrees: Float) {
        isAnchored = true
        val anchored = baseTargets.map { target ->
            target.copy(yawDegrees = normalizeDegrees(target.yawDegrees + headingDegrees))
        }
        _state.value = _state.value.copy(
            targets = anchored,
            activeTargetId = anchored.firstOrNull()?.id,
        )
    }
}
