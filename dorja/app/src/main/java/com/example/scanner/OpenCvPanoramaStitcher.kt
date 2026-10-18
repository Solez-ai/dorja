package com.example.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import kotlin.math.max

/**
 * Kotlin bridge to the native OpenCV panorama stitcher (`libdorja_panorama.so`).
 *
 * The engine itself is a 1:1 port of the PanoramaStitching-Android-OpenCV JNI
 * core (thePerfectPanorama strategy): downscale to ~1000px, cv::Stitcher in
 * PANORAMA mode with -1 resolutions, horizontal wave correction, lenient
 * confidence threshold.
 *
 * Unlike the 2016 reference — whose prebuilt .so files only shipped
 * armeabi-v7a/x86 and therefore could not run on modern phones — this native
 * library is compiled for arm64-v8a and armeabi-v7a by the Gradle build.
 *
 * We deliberately do NOT depend on the org.opencv Java bindings AAR: it is a
 * monolithic ~110MB-per-ABI native payload and its Java API doesn't include
 * the stitching module anyway. Frames and results cross the JNI boundary as
 * raw ARGB int arrays instead.
 */
object OpenCvPanoramaStitcher {

    private const val TAG = "OpenCvPanoramaStitcher"

    /** Downsampled capture resolution fed to the stitcher (long edge, px). */
    private const val FRAME_LONG_EDGE = 2048

    init {
        System.loadLibrary("dorja_panorama")
    }

    /**
     * Stitches the given JPEG frames into one panorama.
     *
     * @param frames JPEG files captured around the ring.
     * @param outputJpeg destination file for the stitched result.
     * @return [StitchResult] on success, null on failure (insufficient
     *         overlap, fewer than 2 decodable frames, stitcher confidence
     *         rejection, ...).
     */
    fun stitch(frames: List<File>, outputJpeg: File): StitchResult? {
        require(frames.size >= 2) { "Need at least 2 frames to stitch" }

        val decoded = frames.mapNotNull { path -> decodeDownsampled(path) }
        if (decoded.size < 2) {
            Log.e(TAG, "Decoding produced <2 usable frames")
            return null
        }

        val widths = IntArray(decoded.size) { decoded[it].third.first }
        val heights = IntArray(decoded.size) { decoded[it].third.second }
        val pixels = decoded.map { it.second }.toTypedArray()

        // Dimension layout: [w, h, pixels...] — see panorama_stitcher.cpp.
        val native = try {
            nativeStitch(decoded.size, widths, heights, pixels)
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native stitcher not loaded", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Stitching crashed", e)
            null
        }

        if (native == null || native.size < 2) {
            Log.e(TAG, "Stitcher returned null — scene likely failed to match")
            return null
        }

        val outW = native[0]
        val outH = native[1]
        if (outW <= 0 || outH <= 0) return null

        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(native, 2, outW, 0, 0, outW, outH)

        // JPEG (quality 90) keeps the panorama file ~10x smaller than PNG
        // at visually indistinguishable quality — part of the app-size budget.
        outputJpeg.parentFile?.mkdirs()
        outputJpeg.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
        Log.i(TAG, "Saved stitched panorama ${bitmap.width}x${bitmap.height} -> ${outputJpeg.path}")

        val result = StitchResult(
            path = outputJpeg.absolutePath,
            width = bitmap.width,
            height = bitmap.height
        )
        bitmap.recycle()
        return result
    }

    data class StitchResult(val path: String, val width: Int, val height: Int)

    /**
     * Decodes a JPEG bounded to [FRAME_LONG_EDGE] on the long edge using the
     * power-of-two inSampleSize trick (bounded decode, no full-size resize),
     * then returns its pixels as ARGB ints + dimensions.
     */
    private fun decodeDownsampled(file: File): Triple<File, IntArray, Pair<Int, Int>> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        var longEdge = max(bounds.outWidth, bounds.outHeight)
        while (longEdge / (sample * 2) >= FRAME_LONG_EDGE) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts)
            ?: return Triple(file, IntArray(0), 0 to 0)
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        return Triple(file, pixels, w to h)
    }

    private external fun nativeStitch(
        frameCount: Int,
        widths: IntArray,
        heights: IntArray,
        pixels: Array<IntArray>
    ): IntArray?
}
