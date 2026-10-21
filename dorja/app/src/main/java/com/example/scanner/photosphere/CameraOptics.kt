package com.example.scanner.photosphere

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import android.util.SizeF
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sqrt
import kotlin.math.tan

private const val TAG = "CameraOptics"

private val DEFAULT_SENSOR_FIELD_OF_VIEW = FieldOfView(
    horizontalDegrees = 66f,
    verticalDegrees = 52f,
)

private const val MAIN_LENS_DIAGONAL_FOV_DEGREES = 78f
private const val MAX_ESTIMATE_DISAGREEMENT = 1.25f

data class SphereOptics(
    val fieldOfView: FieldOfView,
    val radialDistortion: RadialDistortion?,
    val portraitRotationDegrees: Int,
)

@Composable
fun rememberSphereOptics(
    cameraId: String? = null,
    streamAspectRatio: Float = 0f,
): SphereOptics {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration, cameraId, streamAspectRatio) {
        estimateSphereOptics(
            context = context,
            displayRotation = context.currentDisplayRotation(),
            profile = SphereDeviceProfile.forDevice(context),
            cameraId = cameraId,
            streamAspectRatio = streamAspectRatio,
        )
    }
}

internal fun estimateSphereOptics(
    context: Context,
    displayRotation: Int,
    profile: SphereDeviceProfile,
    cameraId: String? = null,
    streamAspectRatio: Float = 0f,
): SphereOptics {
    var sensorFieldOfView = DEFAULT_SENSOR_FIELD_OF_VIEW
    var sensorOrientation = 90
    var radialDistortion: RadialDistortion? = null

    val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    if (manager != null) {
        try {
            val id = cameraId ?: captureBackCameraId(context, profile)
            if (id != null) {
                val characteristics = manager.getCameraCharacteristics(id)
                sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

                val array =
                    characteristics.get(
                        CameraCharacteristics.SENSOR_INFO_PRE_CORRECTION_ACTIVE_ARRAY_SIZE
                    ) ?: characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val physicalSize =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val focalLengthMm = selectFocalLengthMm(
                    focalLengthsMm =
                        characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS),
                    sensorDiagonalMm = physicalSize.diagonalMm(),
                    preferWidest = profile.preferWidestCamera,
                )

                val fromIntrinsics = array?.let { active ->
                    characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION)
                        ?.takeIf { it.size >= 2 && it[0] > 0f && it[1] > 0f }
                        ?.let { intrinsic ->
                            FieldOfView(
                                horizontalDegrees =
                                    fieldOfViewDegrees(active.width().toFloat(), intrinsic[0]),
                                verticalDegrees =
                                    fieldOfViewDegrees(active.height().toFloat(), intrinsic[1]),
                            )
                        }
                }
                val fromGeometry = if (physicalSize != null && focalLengthMm != null) {
                    FieldOfView(
                        horizontalDegrees = fieldOfViewDegrees(physicalSize.width, focalLengthMm),
                        verticalDegrees = fieldOfViewDegrees(physicalSize.height, focalLengthMm),
                    )
                } else {
                    null
                }

                sensorFieldOfView = reconcileFieldOfView(fromIntrinsics, fromGeometry)
                if (array != null) {
                    sensorFieldOfView = croppedToStreamAspect(
                        sensor = sensorFieldOfView,
                        sensorAspectRatio = array.width().toFloat() / array.height(),
                        streamAspectRatio = streamAspectRatio,
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val kappa = characteristics.get(CameraCharacteristics.LENS_DISTORTION)
                    if (kappa != null && kappa.size >= 3) {
                        radialDistortion = RadialDistortion(
                            coefficients = doubleArrayOf(
                                kappa[0].toDouble(),
                                kappa[1].toDouble(),
                                kappa[2].toDouble(),
                            ),
                        ).takeIf { it.isSignificant }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falling back to default field of view", e)
        }
    }

    val relativeRotation = ((sensorOrientation - displayRotation.toDegrees()) % 360 + 360) % 360
    val fieldOfView =
        if (relativeRotation % 180 == 90) sensorFieldOfView.transposed() else sensorFieldOfView
    return SphereOptics(
        fieldOfView = fieldOfView,
        radialDistortion = radialDistortion,
        portraitRotationDegrees = relativeRotation,
    )
}

internal fun selectFocalLengthMm(
    focalLengthsMm: FloatArray?,
    sensorDiagonalMm: Float,
    preferWidest: Boolean,
): Float? {
    val candidates = focalLengthsMm?.filter { it > 0f }?.distinct().orEmpty()
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()
    if (preferWidest) return candidates.minOrNull()
    if (sensorDiagonalMm <= 0f) return candidates.maxOrNull()

    return candidates.minByOrNull { focal ->
        abs(fieldOfViewDegrees(sensorDiagonalMm, focal) - MAIN_LENS_DIAGONAL_FOV_DEGREES)
    }
}

private fun SizeF?.diagonalMm(): Float =
    this?.takeIf { it.width > 0f && it.height > 0f }
        ?.let { sqrt(it.width * it.width + it.height * it.height) }
        ?: 0f

internal fun reconcileFieldOfView(
    fromIntrinsics: FieldOfView?,
    fromGeometry: FieldOfView?,
): FieldOfView {
    if (fromIntrinsics == null) return fromGeometry ?: DEFAULT_SENSOR_FIELD_OF_VIEW
    if (fromGeometry == null) return fromIntrinsics

    val ratio = fromIntrinsics.horizontalDegrees / fromGeometry.horizontalDegrees
    val agrees = ratio <= MAX_ESTIMATE_DISAGREEMENT && ratio >= 1f / MAX_ESTIMATE_DISAGREEMENT
    return if (agrees) fromIntrinsics else fromGeometry
}

internal fun croppedToStreamAspect(
    sensor: FieldOfView,
    sensorAspectRatio: Float,
    streamAspectRatio: Float,
): FieldOfView {
    if (sensorAspectRatio <= 0f || streamAspectRatio <= 0f) return sensor
    val tanHorizontal = tanHalf(sensor.horizontalDegrees)
    val tanVertical = tanHalf(sensor.verticalDegrees)
    return if (streamAspectRatio >= sensorAspectRatio) {
        FieldOfView(
            horizontalDegrees = sensor.horizontalDegrees,
            verticalDegrees = degreesFromTanHalf(tanHorizontal / streamAspectRatio),
        )
    } else {
        FieldOfView(
            horizontalDegrees = degreesFromTanHalf(tanVertical * streamAspectRatio),
            verticalDegrees = sensor.verticalDegrees,
        )
    }
}

private fun fieldOfViewDegrees(extent: Float, focal: Float): Float =
    degreesFromTanHalf(extent / (2f * focal))

private fun tanHalf(degrees: Float): Float = tan(Math.toRadians(degrees / 2.0)).toFloat()

private fun degreesFromTanHalf(tangent: Float): Float =
    Math.toDegrees(2.0 * atan(tangent.toDouble())).toFloat().coerceIn(1f, 179f)

private fun Int.toDegrees(): Int = when (this) {
    Surface.ROTATION_90 -> 90
    Surface.ROTATION_180 -> 180
    Surface.ROTATION_270 -> 270
    else -> 0
}
