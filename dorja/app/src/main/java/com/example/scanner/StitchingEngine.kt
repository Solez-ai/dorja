package com.example.scanner

import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real stitching engine — replaces the old placeholder that just slept two
 * seconds and returned true. The work is delegated to
 * [OpenCvPanoramaStitcher], the native cv::Stitcher port.
 *
 * cv::Stitcher on a phone camera at 1000px working scale typically needs
 * well over the old 30s timeout, so the budget is 4 minutes here.
 */
class StitchingEngine {

    /**
     * Stitches [frames] (JPEGs on disk) into [outputJpeg].
     *
     * @return the stitched JPEG path on success, null on failure.
     */
    suspend fun stitchFrames(frames: List<FrameData>, outputJpeg: File): String? {
        val jpegFiles = frames.map { File(it.path) }
        return withTimeoutOrNull(BUDGET_MS) {
            withContext(Dispatchers.Default) {
                try {
                    Log.d(TAG, "Starting stitching for ${frames.size} frames")
                    val result = OpenCvPanoramaStitcher.stitch(jpegFiles, outputJpeg)
                    Log.d(TAG, "Stitching ${if (result != null) "succeeded" else "failed"}")
                    result?.path
                } catch (e: Exception) {
                    Log.e(TAG, "Stitching failed: ${e.message}")
                    null
                }
            }
        }
    }

    companion object {
        private const val TAG = "StitchingEngine"
        private const val BUDGET_MS = 4L * 60_000L
    }
}
