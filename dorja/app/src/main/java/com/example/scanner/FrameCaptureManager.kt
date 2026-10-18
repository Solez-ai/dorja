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

    suspend fun captureFrame(): Boolean = withContext(Dispatchers.Main) {
        try {
            imageCapture?.takePicture(
                ImageCapture.OutputFileOptions.Builder(
                    context.cacheDir
                ).build(),
                Dispatchers.IO.asExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val path = outputFileResults.savedUri?.path ?: return
                        frames.add(FrameData(path = path))
                    }

                    override fun onError(exception: ImageCapture.OnError) {
                        // Handle error
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
