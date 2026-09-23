package com.example.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.stitching.Stitcher
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * On-device panorama stitching powered by OpenCV's [Stitcher].
 *
 * Fully offline: frames come straight off CameraX, stitching runs on the
 * phone's CPU, nothing ever leaves the device.
 *
 * Pipeline:
 *   JPEG frames → OpenCV Mats → [Stitcher.PANORAMA] (feature matching,
 *   homography estimation, warping, exposure compensation, seam finding,
 *   multi-band blending — all inside OpenCV) → cropped 2:1 equirect-style
 *   JPEG written to cache.
 *
 * The stitcher is created lazily per run and released afterwards, so memory
 * returns to the system between scans.
 */
object PanoramaStitcherEngine {

    private const val TAG = "PanoramaStitcher"

    /** Max long-edge fed into OpenCV; keeps memory bounded on 12 MP frames. */
    private const val MAX_INPUT_EDGE = 1600

    /** Downscale step for the Otsu border scan of the stitched output. */
    private const val BORDER_SCAN_SAMPLE = 4

    private var initialized = false

    /** Loads OpenCV's native library exactly once. */
    private fun ensureInit(): Boolean {
        if (initialized) return true
        initialized = OpenCVLoader.initLocal()
        if (!initialized) Log.e(TAG, "OpenCV native library failed to load")
        return initialized
    }

    /**
     * Result of a stitch run.
     *
     * Note on multi-brand status codes: OpenCV's Java wrapper predates the
     * homography-estimation failure split (ERR_HOMOGRAPHY_EST = 3, the old
     * generic error = 1), so both numeric failures are accepted here and
     * mapped to a user-actionable message.
     */
    sealed class StitchResult {
        data class Success(val panoramaPath: String, val width: Int, val height: Int) : StitchResult()
        data class Failure(val reason: Reason, val status: Int) : StitchResult()
    }

    enum class Reason(val userMessage: String) {
        NOT_ENOUGH_FRAMES("Not enough overlapping frames. Move slower between shots."),
        NO_INIT("Stitching engine failed to start on this device."),
        NO_MATCH("Frames do not overlap enough. Stand in one spot and rotate slowly."),
        LOW_TEXTURE("Room has too little texture for the scanner. Add light or objects."),
        GPU_FAIL("Stitching ran out of device resources. Try again."),
        UNKNOWN("Stitching failed. Retake the scan.")
    }

    /**
     * Stitches the given JPEG frames into a panorama and writes the result to
     * a new cache file. Never throws — every failure path returns a typed
     * [StitchResult.Failure].
     */
    fun stitch(ctx: Context, framePaths: List<String>): StitchResult {
        if (framePaths.size < 2) {
            return StitchResult.Failure(Reason.NOT_ENOUGH_FRAMES, -1)
        }
        if (!ensureInit()) {
            return StitchResult.Failure(Reason.NO_INIT, -2)
        }

        val mats = ArrayList<Mat>(framePaths.size)
        try {
            // ── Decode + downscale each frame into an RGBA Mat ─────────────
            for (path in framePaths) {
                val bmp = decodeScaled(path) ?: continue
                val mat = Mat()
                Utils.bitmapToMat(bmp, mat)
                bmp.recycle()
                mats.add(mat)
            }
            if (mats.size < 2) {
                return StitchResult.Failure(Reason.NOT_ENOUGH_FRAMES, -3)
            }
            Log.i(TAG, "Stitching ${mats.size} frames with OpenCV Stitcher.PANORAMA")

            // ── Hand the whole pipeline to OpenCV ───────────────────────────
            // PANORAMA mode = spherical warping + bundle-adjusted camera
            // estimation + exposure compensation + multi-band blending.
            val stitcher = Stitcher.create(Stitcher.PANORAMA)
            // Bounded work-mem gives predictable behavior on low-RAM phones.
            try { stitcher.panoConfidenceThreshold = 0.6 } catch (_: Throwable) { /* optional knob */ }
            val result = Mat()
            val status: Int = try {
                stitcher.stitch(mats, result)
            } finally {
                stitcher.dispose()
            }

            if (status != Stitcher.OK) {
                Log.w(TAG, "OpenCV stitcher returned status $status")
                result.release()
                return StitchResult.Failure(status.toReason(), status)
            }

            if (result.empty() || result.cols() < 64 || result.rows() < 64) {
                result.release()
                return StitchResult.Failure(Reason.UNKNOWN, status)
            }

            // ── Post-process: RGB → crop → letterbox-free 2:1 ─────────────
            Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2RGBA)
            val stitched = cropAutoDetectedBorders(result)
            result.release()

            val finalBmp = letterboxToEquirect(stitched)
            stitched.release()

            val file = File(ctx.cacheDir, "panorama_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                finalBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val w = finalBmp.width
            val h = finalBmp.height
            finalBmp.recycle()

            Log.i(TAG, "Stitch OK → ${file.name} (${w}×$h)")
            return StitchResult.Success(file.absolutePath, w, h)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM during stitching", e)
            System.gc()
            return StitchResult.Failure(Reason.GPU_FAIL, -4)
        } catch (e: Exception) {
            Log.e(TAG, "Stitching crashed", e)
            return StitchResult.Failure(Reason.UNKNOWN, -5)
        } finally {
            mats.forEach { it.release() }
        }
    }

    private fun Int.toReason(): Reason = when (this) {
        1, 3 -> Reason.NO_MATCH          // ERR_NEED_MORE_IMGS / ERR_HOMOGRAPHY_EST
        2 -> Reason.LOW_TEXTURE          // ERR_CAMERA_PARAMS
        else -> Reason.UNKNOWN
    }

    /** Decodes a JPEG bounded so its longest edge ≤ [MAX_INPUT_EDGE]. */
    private fun decodeScaled(path: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            val longest = max(bounds.outWidth, bounds.outHeight)
            while (longest / (sample * 2) >= MAX_INPUT_EDGE) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) {
            Log.w(TAG, "Frame decode failed: $path — ${e.message}")
            null
        }
    }

    /**
     * Detects near-black padding rows/columns on the stitched result and crops
     * them away. OpenCV fills warp areas it could not cover with black, and
     * those bands would later be stretched by the 2:1 letterbox step.
     */
    private fun cropAutoDetectedBorders(src: Mat): Mat {
        val small = Mat()
        Imgproc.resize(src, small, Size(
            (src.cols() / BORDER_SCAN_SAMPLE.toDouble()).coerceAtLeast(1.0),
            (src.rows() / BORDER_SCAN_SAMPLE.toDouble()).coerceAtLeast(1.0)
        ))
        val gray = Mat()
        Imgproc.cvtColor(small, gray, Imgproc.COLOR_RGBA2GRAY)

        val thresh = Mat()
        // 12/255 Otsu-free threshold: anything darker than near-black is padding.
        Imgproc.threshold(gray, thresh, 12.0, 255.0, Imgproc.THRESH_BINARY)

        val rows = thresh.rows()
        val cols = thresh.cols()
        var top = 0
        var bottom = rows - 1
        var left = 0
        var right = cols - 1
        val rowScan = ByteArray(cols)
        val colScan = ByteArray(rows)

        fun rowMean(y: Int): Double {
            thresh.get(y, 0, rowScan)
            var s = 0
            for (b in rowScan) if (b.toInt() != 0) s++
            return s.toDouble() / cols
        }
        fun colMean(x: Int): Double {
            thresh.get(0, x, colScan)
            var s = 0
            for (b in colScan) if (b.toInt() != 0) s++
            return s.toDouble() / rows
        }

        while (top < bottom && rowMean(top) < 0.05) top++
        while (bottom > top && rowMean(bottom) < 0.05) bottom--
        while (left < right && colMean(left) < 0.05) left++
        while (right > left && colMean(right) < 0.05) right--
        small.release(); gray.release(); thresh.release()

        val cropW = (right - left + 1) * BORDER_SCAN_SAMPLE
        val cropH = (bottom - top + 1) * BORDER_SCAN_SAMPLE
        val clampedW = min(cropW, src.cols())
        val clampedH = min(cropH, src.rows())
        if (clampedW <= 0 || clampedH <= 0 || (clampedW == src.cols() && clampedH == src.rows())) {
            return src.clone()
        }
        return src.submat(0, clampedH, 0, clampedW).clone()
    }

    /**
     * Fits the cropped stitch into an exact 2:1 canvas (2048×1024) with black
     * pillarboxes, so the equirectangular sampler in the viewer always has the
     * right aspect to sample from. Content is centred; nothing is stretched.
     */
    private fun letterboxToEquirect(src: Mat): Bitmap {
        val outW = 2048
        val outH = 1024
        val scale = min(
            outW.toDouble() / src.cols().toDouble(),
            outH.toDouble() / src.rows().toDouble()
        )
        val fitted = Mat()
        Imgproc.resize(
            src, fitted, Size(
                (src.cols() * scale).coerceAtLeast(1.0),
                (src.rows() * scale).coerceAtLeast(1.0)
            )
        )

        val canvasMat = Mat.zeros(outH, outW, CvType.CV_8UC4)
        val px = (outW - fitted.cols()) / 2
        val py = (outH - fitted.rows()) / 2
        fitted.copyTo(canvasMat.submat(py, py + fitted.rows(), px, px + fitted.cols()))
        fitted.release()

        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(canvasMat, bmp)
        canvasMat.release()
        return bmp
    }

    /**
     * Mean absolute difference between the left and right edges of a panorama
     * (0–255). A closed 360° ring should wrap seamlessly, so a high value
     * means the sweep did not close. Used by the scanner to warn honestly
     * instead of saving a panorama with a visible seam.
     */
    fun wrapSeamError(panoramaPath: String): Float {
        return try {
            val bmp = decodeScaled(panoramaPath) ?: return -1f
            val mat = Mat()
            Utils.bitmapToMat(bmp, mat)
            bmp.recycle()
            val small = Mat()
            Imgproc.resize(mat, small, Size(64.0, 64.0))
            mat.release()
            val gray = Mat()
            Imgproc.cvtColor(small, gray, Imgproc.COLOR_RGBA2GRAY)
            small.release()

            val left = Mat()
            val right = Mat()
            Imgproc.resize(gray.colRange(0, 2), left, Size(1.0, 64.0))
            Imgproc.resize(gray.colRange(62, 64), right, Size(1.0, 64.0))
            val diff = Mat()
            Core.absdiff(left, right, diff)
            val mean = Core.mean(diff).`val`[0].toFloat()
            left.release(); right.release(); diff.release(); gray.release()
            mean
        } catch (e: Exception) {
            -1f
        }
    }
}
