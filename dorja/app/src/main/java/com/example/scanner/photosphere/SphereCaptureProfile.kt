@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
@file:SuppressLint("UnsafeOptInUsageError")

package com.example.scanner.photosphere

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.interop.Camera2Interop

data class SphereCaptureProfile(
    val focusMode: FocusMode,
    val aeLockSupported: Boolean,
    val awbLockSupported: Boolean,
    val opticalStabilizationSupported: Boolean,
)

enum class FocusMode {
    FIXED_FOCUS,
    FOCUS_POINT,
}

internal fun resolveFocusMode(minimumFocusDistance: Float?): FocusMode =
    if (minimumFocusDistance != null && minimumFocusDistance <= 0f) {
        FocusMode.FIXED_FOCUS
    } else {
        FocusMode.FOCUS_POINT
    }

internal fun resolveLockSupport(available: Boolean?): Boolean = available ?: true

internal fun resolveOpticalStabilization(stabilizationModes: IntArray?): Boolean =
    stabilizationModes?.contains(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) == true

internal fun resolveSphereCaptureProfile(
    context: Context,
    profile: SphereDeviceProfile,
    cameraId: String? = null,
): SphereCaptureProfile {
    val characteristics = runCatching {
        if (cameraId != null) {
            (context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager)
                ?.getCameraCharacteristics(cameraId)
        } else {
            backCameraCharacteristics(context, profile)
        }
    }.getOrNull()
    return SphereCaptureProfile(
        focusMode = resolveFocusMode(
            minimumFocusDistance =
                characteristics?.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE),
        ),
        aeLockSupported = resolveLockSupport(
            characteristics?.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE)
        ),
        awbLockSupported = resolveLockSupport(
            characteristics?.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE)
        ),
        opticalStabilizationSupported = resolveOpticalStabilization(
            characteristics?.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        ),
    )
}

internal fun widestBackCameraId(context: Context): String? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
    var bestId: String? = null
    var bestFocal = Float.MAX_VALUE
    for (id in manager.cameraIdList) {
        val characteristics = runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
            ?: continue
        if (characteristics.get(CameraCharacteristics.LENS_FACING) !=
            CameraCharacteristics.LENS_FACING_BACK
        ) {
            continue
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            characteristics.physicalCameraIds.isNotEmpty()
        ) {
            continue
        }
        val focal = characteristics
            .get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.minOrNull()
            ?: continue
        val array = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            ?: continue
        if (array.width.toLong() * array.height < MIN_WIDE_SENSOR_PIXELS) continue
        if (focal < bestFocal) {
            bestFocal = focal
            bestId = id
        }
    }
    return bestId
}

private const val MIN_WIDE_SENSOR_PIXELS = 6_000_000L

internal fun defaultBackCameraId(context: Context): String? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
    return manager.cameraIdList.firstOrNull { id ->
        runCatching {
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }.getOrDefault(false)
    }
}

internal fun captureBackCameraId(context: Context, profile: SphereDeviceProfile): String? =
    if (profile.preferWidestCamera) {
        widestBackCameraId(context) ?: defaultBackCameraId(context)
    } else {
        defaultBackCameraId(context)
    }

private fun backCameraCharacteristics(context: Context, profile: SphereDeviceProfile): CameraCharacteristics? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
    val id = captureBackCameraId(context, profile) ?: return null
    return runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
}

internal fun applySphereCaptureOptions(
    extender: Camera2Interop.Extender<*>,
    profile: SphereCaptureProfile,
) {
    when (profile.focusMode) {
        FocusMode.FIXED_FOCUS -> {
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF,
            )
        }
        FocusMode.FOCUS_POINT -> {
            extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO,
            )
        }
    }
}

internal fun applyStillImageOptions(
    extender: Camera2Interop.Extender<*>,
    profile: SphereCaptureProfile,
) {
    if (profile.aeLockSupported) {
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
    }
    if (profile.awbLockSupported) {
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
    }
    if (profile.opticalStabilizationSupported) {
        extender.setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON,
        )
    }
}
