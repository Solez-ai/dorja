package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Manages continuous strip capture for horizontal sweep panoramas.
 * 
 * Replaces FrameCaptureManager's discrete photo capture with continuous frame-by-frame
 * processing using CameraX ImageAnalysis. Extracts vertical strips from camera frames
 * as the user pans horizontally.
 */
class ContinuousCaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    // Executor for image processing (single thread to maintain order)
    private val imageExecutor = Executors.newSingleThreadExecutor()
    
    // Captured strips storage
    private val capturedStrips = mutableListOf<PanoramaStrip>()
    
    // Last captured strip heading (to determine when to capture next)
    private var lastCapturedHeading: Float? = null
    
    // Sweep configuration
    private var sweepConfig: SweepConfig? = null
    
    // Callbacks
    var onStripCaptured: ((PanoramaStrip) -> Unit)? = null
    var onCaptureError: ((Exception) -> Unit)? = null
    
    /**
     * Starts continuous capture with the given sweep configuration.
     * Creates an ImageAnalysis use case that processes every camera frame.
     * 
     * @param imageAnalysis CameraX ImageAnalysis use case
     * @param config Sweep configuration defining capture parameters
     */
    fun startContinuousCapture(
        imageAnalysis: ImageAnalysis,
        config: SweepConfig,
        currentHeading: Float
    ) {
        sweepConfig = config
        lastCapturedHeading = currentHeading
        capturedStrips.clear()
        
        imageAnalysis.setAnalyzer(imageExecutor) { imageProxy ->
            processFrame(imageProxy)
        }
        
        Log.d(TAG, "Continuous capture started: ${config.sweepAngle}° sweep")
    }
    
    /**
     * Processes a single camera frame.
     * Decides whether to extract and capture a strip based on heading change.
     */
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val currentHeading = getCurrentHeading() ?: run {
                imageProxy.close()
                return
            }
            
            // Check if we should capture a strip at this heading
            if (shouldCaptureStripAt(currentHeading)) {
                val strip = extractVerticalStrip(imageProxy, currentHeading)
                
                if (strip != null) {
                    capturedStrips.add(strip)
                    lastCapturedHeading = currentHeading
                    onStripCaptured?.invoke(strip)
                    
                    Log.d(TAG, "Strip captured: heading=$currentHeading, total=${capturedStrips.size}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}", e)
            onCaptureError?.invoke(e)
        } finally {
            imageProxy.close()
        }
    }
    
    /**
     * Determines if a strip should be captured at the current heading.
     * Captures every N degrees based on sweep config.
     */
    private fun shouldCaptureStripAt(currentHeading: Float): Boolean {
        val config = sweepConfig ?: return false
        val lastHeading = lastCapturedHeading ?: return true // First strip
        
        // Calculate heading difference
        var diff = currentHeading - lastHeading
        
        // Handle 360°/0° wrap-around
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        
        // Check if we've rotated enough for next strip
        val absD iff = kotlin.math.abs(diff)
        return absDiff >= config.stripIntervalDegrees
    }
    
    /**
     * Extracts a vertical strip from the camera frame.
     * The strip is taken from the center of the frame (most stable area).
     * 
     * @param imageProxy Camera frame image
     * @param heading Heading when this strip was captured
     * @return PanoramaStrip or null if extraction failed
     */
    private fun extractVerticalStrip(
        imageProxy: ImageProxy,
        heading: Float
    ): PanoramaStrip? {
        try {
            // Get image dimensions
            val width = imageProxy.width
            val height = imageProxy.height
            
            // Calculate strip dimensions
            // Strip width: 30-50 pixels (configurable based on sweep speed)
            val stripWidth = 40
            val stripX = (width - stripWidth) / 2 // Center of frame
            
            // Extract strip from YUV image
            val stripBitmap = extractStripFromYUV(
                imageProxy,
                Rect(stripX, 0, stripX + stripWidth, height)
            ) ?: return null
            
            return PanoramaStrip(
                image = stripBitmap,
                heading = heading,
                width = stripWidth,
                height = height,
                timestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract strip: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Extracts a strip region from a YUV_420_888 image (CameraX default format).
     * Converts YUV to RGB and extracts the specified rectangle.
     */
    private fun extractStripFromYUV(
        imageProxy: ImageProxy,
        rect: Rect
    ): Bitmap? {
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            Log.w(TAG, "Unsupported image format: ${imageProxy.format}")
            return null
        }
        
        val yPlane = imageProxy.planes[0]
        val uPlane = imageProxy.planes[1]
        val vPlane = imageProxy.planes[2]
        
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        
        // Create bitmap for the strip
        val stripBitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        
        // Convert YUV to RGB for the strip region
        // This is a simplified version - production should use RenderScript or native code
        val pixels = IntArray(rect.width() * rect.height())
        
        for (y in rect.top until rect.bottom) {
            for (x in rect.left until rect.right) {
                val pixelIndex = (y - rect.top) * rect.width() + (x - rect.left)
                
                // Get Y, U, V values
                val yIndex = y * yPlane.rowStride + x
                val uvIndex = (y / 2) * uPlane.rowStride + (x / 2)
                
                val yValue = (yBuffer.get(yIndex).toInt() and 0xFF)
                val uValue = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val vValue = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128
                
                // YUV to RGB conversion
                val r = (yValue + 1.370705f * vValue).toInt().coerceIn(0, 255)
                val g = (yValue - 0.337633f * uValue - 0.698001f * vValue).toInt().coerceIn(0, 255)
                val b = (yValue + 1.732446f * uValue).toInt().coerceIn(0, 255)
                
                pixels[pixelIndex] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        
        stripBitmap.setPixels(pixels, 0, rect.width(), 0, 0, rect.width(), rect.height())
        return stripBitmap
    }
    
    /**
     * Gets the current compass heading from sensor fusion.
     * This should be wired to SensorFusionManager's latest orientation.
     */
    private fun getCurrentHeading(): Float? {
        // TODO: Wire to SensorFusionManager
        // For now, return null to indicate no heading available
        return currentHeadingCache
    }
    
    // Temporary cache for current heading (will be updated by SensorFusionManager callback)
    private var currentHeadingCache: Float? = null
    
    /**
     * Updates the current heading cache.
     * Should be called from SensorFusionManager's orientation update callback.
     */
    fun updateCurrentHeading(heading: Float) {
        currentHeadingCache = heading
    }
    
    /**
     * Stops continuous capture and cleans up resources.
     */
    fun stopCapture() {
        // ImageAnalysis analyzer will be cleared by CameraX lifecycle
        Log.d(TAG, "Continuous capture stopped. Captured ${capturedStrips.size} strips")
    }
    
    /**
     * Gets all captured strips for stitching.
     */
    fun getCapturedStrips(): List<PanoramaStrip> {
        return capturedStrips.toList()
    }
    
    /**
     * Gets the number of strips captured so far.
     */
    fun getStripCount(): Int {
        return capturedStrips.size
    }
    
    /**
     * Clears all captured strips (for retake).
     */
    fun clearStrips() {
        capturedStrips.clear()
        lastCapturedHeading = null
    }
    
    /**
     * Releases resources and shuts down executor.
     */
    fun release() {
        try {
            imageExecutor.shutdown()
            capturedStrips.clear()
            Log.d(TAG, "ContinuousCaptureManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing ContinuousCaptureManager: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "ContinuousCaptureManager"
    }
}

/**
 * Represents a single vertical strip extracted from a panorama sweep.
 */
data class PanoramaStrip(
    /**
     * The strip image (vertical slice from camera frame).
     */
    val image: Bitmap,
    
    /**
     * Compass heading when this strip was captured (degrees, [0, 360)).
     */
    val heading: Float,
    
    /**
     * Width of the strip in pixels.
     */
    val width: Int,
    
    /**
     * Height of the strip in pixels.
     */
    val height: Int,
    
    /**
     * Timestamp when strip was captured (milliseconds since epoch).
     */
    val timestamp: Long
) {
    /**
     * Calculates the horizontal position where this strip should be placed
     * in the final panorama based on its heading.
     * 
     * @param sweepConfig The sweep configuration
     * @param panoramaWidth Total width of the output panorama
     * @return X-coordinate in pixels where this strip should be placed
     */
    fun calculateXPosition(sweepConfig: SweepConfig, panoramaWidth: Int): Int {
        val progress = sweepConfig.calculateProgress(heading)
        return (progress * panoramaWidth).toInt().coerceIn(0, panoramaWidth - width)
    }
    
    /**
     * Releases the bitmap memory.
     */
    fun recycle() {
        if (!image.isRecycled) {
            image.recycle()
        }
    }
}
