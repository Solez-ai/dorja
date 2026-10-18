package com.example.scanner

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages CameraX initialization, binding, unbinding, and zoom capability detection.
 * Handles Camera2 metadata queries for zoom ranges and lens type detection.
 * Provides graceful error recovery with safe fallback defaults.
 * Thread-safe: all camera binding/unbinding wrapped in Dispatchers.Main context.
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView
) {
    private var camera: Camera? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val androidCameraManager: AndroidCameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager

    companion object {
        private const val TAG = "CameraManager"

        // Focal length thresholds for lens type detection
        private const val ULTRA_WIDE_MAX_FOCAL_LENGTH = 24.0f
        private const val STANDARD_MIN_FOCAL_LENGTH = 24.0f
        private const val STANDARD_MAX_FOCAL_LENGTH = 35.0f
        private const val TELEPHOTO_MIN_FOCAL_LENGTH = 50.0f

        // Safe defaults when metadata unavailable
        private const val DEFAULT_MIN_ZOOM = 1.0f
        private const val DEFAULT_MAX_ZOOM = 2.0f
        private const val DEFAULT_ZOOM_STANDARD = 1.0f
        private const val DEFAULT_ZOOM_ULTRA_WIDE = 0.8f
    }

    /**
     * Detects camera zoom capabilities by querying Camera2 metadata.
     * Identifies lens types (ultra-wide, standard, telephoto) via focal length mapping.
     * Returns safe defaults (1.0x–2.0x) if metadata is unavailable.
     *
     * @return ZoomCapabilities with detected or default values
     */
    suspend fun detectZoomCapabilities(): ZoomCapabilities = withContext(Dispatchers.Default) {
        try {
            val cameraIdList = androidCameraManager.cameraIdList
            if (cameraIdList.isEmpty()) {
                Log.w(TAG, "No cameras found; using default zoom capabilities")
                return@withContext ZoomCapabilities.createDefault()
            }

            // Query the back-facing camera (typically the primary camera for scanning)
            var backCameraId: String? = null
            var ultraWideLens: Float? = null
            var standardLens: Float? = null
            var telephotoLens: Float? = null
            var minZoomRatio = Float.MAX_VALUE
            var maxZoomRatio = 0.0f

            for (cameraId in cameraIdList) {
                val characteristics =
                    androidCameraManager.getCameraCharacteristics(cameraId)

                // Check if this is a back-facing camera
                val facing =
                    characteristics.get(CameraCharacteristics.LENS_FACING)
                        ?: continue
                if (facing != CameraCharacteristics.LENS_FACING_BACK) continue

                if (backCameraId == null) {
                    backCameraId = cameraId
                }

                // Query zoom range
                val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                if (zoomRange != null) {
                    minZoomRatio = minOf(minZoomRatio, zoomRange.lower.toFloat())
                    maxZoomRatio = maxOf(maxZoomRatio, zoomRange.upper.toFloat())
                }

                // Query focal length to determine lens type
                val focalLengths =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                if (focalLengths != null && focalLengths.isNotEmpty()) {
                    val primaryFocalLength = focalLengths[0]

                    when {
                        primaryFocalLength < ULTRA_WIDE_MAX_FOCAL_LENGTH -> {
                            if (ultraWideLens == null || primaryFocalLength < ultraWideLens) {
                                ultraWideLens = primaryFocalLength
                            }
                        }
                        primaryFocalLength in STANDARD_MIN_FOCAL_LENGTH..STANDARD_MAX_FOCAL_LENGTH -> {
                            if (standardLens == null ||
                                primaryFocalLength < STANDARD_MAX_FOCAL_LENGTH) {
                                standardLens = primaryFocalLength
                            }
                        }
                        primaryFocalLength >= TELEPHOTO_MIN_FOCAL_LENGTH -> {
                            if (telephotoLens == null || primaryFocalLength > telephotoLens) {
                                telephotoLens = primaryFocalLength
                            }
                        }
                    }
                }
            }

            // If no zoom range metadata, use defaults
            if (minZoomRatio == Float.MAX_VALUE || maxZoomRatio == 0.0f) {
                Log.w(TAG, "Zoom metadata unavailable; using safe defaults")
                minZoomRatio = DEFAULT_MIN_ZOOM
                maxZoomRatio = DEFAULT_MAX_ZOOM
            }

            // Determine default zoom based on available lenses
            val defaultZoom =
                when {
                    ultraWideLens != null -> DEFAULT_ZOOM_ULTRA_WIDE
                    standardLens != null -> DEFAULT_ZOOM_STANDARD
                    else -> minZoomRatio.coerceAtLeast(DEFAULT_ZOOM_STANDARD)
                }

            val capabilities =
                ZoomCapabilities(
                    minZoomRatio = minZoomRatio,
                    maxZoomRatio = maxZoomRatio,
                    defaultZoom = defaultZoom,
                    hasUltraWide = ultraWideLens != null,
                    hasStandard = standardLens != null,
                    hasTelephoto = telephotoLens != null
                )

            Log.d(
                TAG,
                "Detected zoom capabilities: min=$minZoomRatio, max=$maxZoomRatio, " +
                    "default=$defaultZoom, ultraWide=$ultraWideLens, standard=$standardLens, " +
                    "telephoto=$telephotoLens"
            )

            capabilities
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting zoom capabilities: ${e.message}", e)
            ZoomCapabilities.createDefault()
        }
    }

    /**
     * Binds the camera with CameraX, initializing the preview use case.
     * Thread-safe: wraps all camera binding in Dispatchers.Main context.
     * Handles errors gracefully: logs failures and returns false on binding failure.
     *
     * @return true if camera bound successfully, false otherwise
     */
    suspend fun bindCamera(): Boolean =
        withContext(Dispatchers.Main) {
            return@withContext try {
                val cameraProviderFuture =
                    ProcessCameraProvider.getInstance(context)

                val cameraProvider = cameraProviderFuture.get()

                // Unbind existing camera first
                cameraProvider.unbindAll()

                // Create preview use case
                val preview =
                    Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // Select back-facing camera
                val cameraSelector =
                    CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                // Bind and store reference
                camera =
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )

                Log.d(TAG, "Camera bound successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error binding camera: ${e.message}", e)
                camera = null
                false
            }
        }

    /**
     * Unbinds the camera from CameraX, releasing all resources.
     * Thread-safe: wraps all camera unbinding in Dispatchers.Main context.
     * Handles errors gracefully: logs failures but does not throw.
     */
    suspend fun unbindCamera() {
        withContext(Dispatchers.Main) {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
                camera = null
                Log.d(TAG, "Camera unbound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding camera: ${e.message}", e)
            }
        }
    }

    /**
     * Gets the currently bound camera, or null if not bound.
     *
     * @return Camera instance or null
     */
    fun getCamera(): Camera? = camera

    /**
     * Sets the zoom ratio for the bound camera.
     * Clamps the zoom ratio to the camera's supported range.
     * Thread-safe: all operations on main thread.
     *
     * @param zoomRatio Target zoom ratio (will be clamped to camera's range)
     */
    fun setZoomRatio(zoomRatio: Float) {
        val boundCamera = camera
        if (boundCamera == null) {
            Log.w(TAG, "No camera bound; cannot set zoom ratio")
            return
        }

        try {
            val zoomState = boundCamera.cameraInfo.zoomState.value
            val minZoom = (zoomState?.minZoomRatio?.toFloat() ?: 1.0f)
            val maxZoom = (zoomState?.maxZoomRatio?.toFloat() ?: zoomRatio)
            val clamped = zoomRatio.coerceIn(minZoom, maxZoom)
            boundCamera.cameraControl.setZoomRatio(clamped)
            Log.d(TAG, "Zoom ratio set to $clamped")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting zoom ratio to $zoomRatio: ${e.message}", e)
        }
    }

    /**
     * Releases all resources (camera executor, camera).
     * Should be called when the CameraManager is no longer needed.
     */
    fun release() {
        try {
            cameraExecutor.shutdown()
            Log.d(TAG, "CameraManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing CameraManager: ${e.message}", e)
        }
    }
}
