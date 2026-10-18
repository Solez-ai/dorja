package com.example.scanner

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FrameCaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {

    private var imageCapture: ImageCapture? = null
    private val frames = mutableListOf<FrameData>()

    suspend fun captureFrame(heading: Float = 0f): Boolean = withContext(Dispatchers.Main) {
        try {
            imageCapture?.takePicture(
                ImageCapture.OutputFileOptions.Builder(
                    context.cacheDir
                ).build(),
                Dispatchers.IO.asExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val path = outputFileResults.savedUri?.path ?: return
                        frames.add(FrameData(path = path, heading = heading))
                    }

                    override fun onError(exception: ImageCapture.ImageCaptureException) {
                        // Handle error - log but don't crash
                        android.util.Log.e("FrameCaptureManager", "Capture error: ${exception.message}")
                    }
                }
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getFrames() = frames.toList()

    companion object {
        private const val TAG = "FrameCaptureManager"
    }
}
