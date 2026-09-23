package com.example.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.AKAZE
import org.opencv.features2d.DescriptorMatcher
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * On-device panorama stitching powered by OpenCV.
 *
 * Fully offline: frames come straight off CameraX, stitching runs on the
 * phone's CPU, nothing ever leaves the device.
 *
 * Pipeline (all OpenCV, no homemade CV):
 *   JPEG frames → RGBA Mats → AKAZE keypoints/descriptors → BFMatcher knn
 *   between consecutive frames → Lowe ratio test → RANSAC homography
 *   (Calib3d.findHomography) → chained transform per frame into the frame-0
 *   coordinate system → corner-projected canvas bounds → per-frame exposure
 *   gains → perspective warp → feathered-alpha blend → border crop → 2:1
 *   letterboxed equirect-style JPEG in cache.
 */
object PanoramaStitcherEngine {

    private const val TAG = "PanoramaStitcher"

    /** Max long-edge fed into the pipeline; keeps memory bounded on 12 MP frames. */
    private const val MAX_INPUT_EDGE = 1600

    /** Downscale step for the border scan of the stitched output. */
    private const val BORDER_SCAN_SAMPLE = 4

    /** Lowe ratio test threshold for good matches. */
    private const val MATCH_RATIO = 0.72

    /** Minimum good matches before RANSAC is attempted for a pair. */
    private const val MIN_GOOD_MATCHES = 12

    /** RANSAC reprojection threshold in pixels. */
    private const val RANSAC_REPROJ_THRESH = 4.0

    /** Feather ramp width in pixels for seam blending. */
    private const val FEATHER_PX = 48.0

    private var initialized = false

    /** Loads OpenCV's native library exactly once. */
    private fun ensureInit(): Boolean {
        if (initialized) return true
        initialized = OpenCVLoader.initLocal()
        if (!initialized) Log.e(TAG, "OpenCV native library failed to load")
        return initialized
    }

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

            val result = buildMosaic(mats)
                ?: return StitchResult.Failure(Reason.NO_MATCH, 1)
            if (result.empty() || result.cols() < 64 || result.rows() < 64) {
                result.release()
                return StitchResult.Failure(Reason.NO_MATCH, 1)
            }

            // ── Post-process: crop → 2:1 letterboxed output ────────────────
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

    // ═══════════════════════════════════════════════════════════════
    //  Feature-based mosaic pipeline
    // ═══════════════════════════════════════════════════════════════

    /** Detection/descriptor state for one frame. */
    private class FrameFeatures(
        val gray: Mat,
        val keypoints: MatOfKeyPoint,
        val descriptors: Mat
    )

    private fun releaseFeatures(features: List<FrameFeatures>) {
        for (f in features) {
            f.gray.release()
            f.keypoints.release()
            f.descriptors.release()
        }
    }

    /**
     * Builds a blended mosaic from ordered frames.
     * Returns null when frames cannot be reliably matched.
     */
    private fun buildMosaic(framesIn: List<Mat>): Mat? {
        // 1. AKAZE features per frame
        val detector = AKAZE.create()
        val features = ArrayList<FrameFeatures>(framesIn.size)
        for (mat in framesIn) {
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            val kp = MatOfKeyPoint()
            val desc = Mat()
            detector.detectAndCompute(gray, Mat(), kp, desc)
            features.add(FrameFeatures(gray, kp, desc))
        }
        detector.clear()

        val transforms = ArrayList<Mat>(features.size)
        try {
            // 2+3. Consecutive pairwise homographies H[i]: frame i → frame i-1
            val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
            val pairH = ArrayList<Mat?>(features.size - 1)
            for (i in 0 until features.size - 1) {
                pairH.add(estimatePairHomography(matcher, features[i], features[i + 1]))
            }
            matcher.clear()

            // Placement chain: transforms[i] maps frame i's pixels into frame
            // 0's coordinate system. gemm(prev, h) = prev * h applies h first
            // (i → i-1), then prev (i-1 → ... → 0). A missing link means the
            // sweep had a gap — stop there, later frames would float.
            transforms.add(Mat.eye(3, 3, CvType.CV_64FC1))
            for (i in 1 until features.size) {
                val h = pairH[i - 1] ?: break
                val prev = transforms[i - 1]
                val composed = Mat()
                Core.gemm(prev, h, 1.0, Mat(), 0.0, composed)
                h.release()
                transforms.add(composed)
            }
            if (transforms.size < 2) {
                releaseFeatures(features)
                transforms.forEach { it.release() }
                return null
            }
            val usable = transforms.size

            // 4. Canvas bounds: project each frame's corners through its
            //    transform into frame-0 coordinates.
            var minX = Double.MAX_VALUE
            var minY = Double.MAX_VALUE
            var maxX = -Double.MAX_VALUE
            var maxY = -Double.MAX_VALUE
            val corners = MatOfPoint2f()
            val projected = MatOfPoint2f()
            for (i in 0 until usable) {
                val w = features[i].gray.cols().toDouble()
                val h = features[i].gray.rows().toDouble()
                corners.fromArray(
                    Point(0.0, 0.0),
                    Point(w - 1.0, 0.0),
                    Point(w - 1.0, h - 1.0),
                    Point(0.0, h - 1.0)
                )
                Core.perspectiveTransform(corners, projected, transforms[i])
                for (p in projected.toArray()) {
                    minX = min(minX, p.x); maxX = max(maxX, p.x)
                    minY = min(minY, p.y); maxY = max(maxY, p.y)
                }
            }
            corners.release()
            projected.release()

            if (maxX <= minX || maxY <= minY) {
                releaseFeatures(features)
                transforms.forEach { it.release() }
                return null
            }
            val canvasW = (maxX - minX).toInt().coerceIn(64, 8192)
            val canvasH = (maxY - minY).toInt().coerceIn(64, 4096)
            val offsetX = -minX
            val offsetY = -minY
            Log.i(TAG, "Mosaic canvas: ${canvasW}×${canvasH} from $usable frames")

            // 5. Exposure gains chained from frame 0.
            val gains = exposureGains(features, usable)

            // 6. Warp each frame and blend with feathered weights.
            val accColor = Mat.zeros(canvasH, canvasW, CvType.CV_32FC4)
            val accWeight = Mat.zeros(canvasH, canvasW, CvType.CV_32FC1)
            val shift = Mat.eye(3, 3, CvType.CV_64FC1)
            shift.put(0, 2, offsetX)
            shift.put(1, 2, offsetY)

            for (i in 0 until usable) {
                val shifted = Mat()
                Core.gemm(shift, transforms[i], 1.0, Mat(), 0.0, shifted)

                val warped8 = Mat()
                Imgproc.warpPerspective(
                    framesIn[i], warped8, shifted,
                    Size(canvasW.toDouble(), canvasH.toDouble()),
                    Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT,
                    Scalar(0.0, 0.0, 0.0, 0.0)
                )

                // Feather weight: linear ramp from the frame edges inward.
                val fw = framesIn[i].cols()
                val fh = framesIn[i].rows()
                val maskSrc = Mat(fh, fw, CvType.CV_32FC1)
                val maskRow = FloatArray(fw)
                for (y in 0 until fh) {
                    val ey = min(y.toDouble(), (fh - 1 - y).toDouble())
                    for (x in 0 until fw) {
                        val ex = min(x.toDouble(), (fw - 1 - x).toDouble())
                        maskRow[x] = min(1.0, min(ex, ey) / FEATHER_PX).toFloat()
                    }
                    maskSrc.put(y, 0, maskRow)
                }
                val wMat = Mat()
                Imgproc.warpPerspective(
                    maskSrc, wMat, shifted,
                    Size(canvasW.toDouble(), canvasH.toDouble()),
                    Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(0.0)
                )
                maskSrc.release()

                // Premultiply colour by weight, accumulate.
                val warpedF = Mat()
                warped8.convertTo(warpedF, CvType.CV_32FC4)
                warped8.release()
                val chs = ArrayList<Mat>(4)
                Core.split(warpedF, chs)
                for (c in 0 until 3) {
                    Core.multiply(chs[c], wMat, chs[c])
                }
                Core.merge(chs, warpedF)
                chs.forEach { it.release() }
                Core.add(accColor, warpedF, accColor)
                Core.add(accWeight, wMat, accWeight)
                warpedF.release()
                wMat.release()
                shifted.release()
            }
            shift.release()
            transforms.forEach { it.release() }
            transforms.clear()
            releaseFeatures(features)

            // 7. Normalise RGB by accumulated weight; alpha from coverage.
            val denom = Mat()
            Core.max(accWeight, Scalar(1e-4), denom)
            val outChs = ArrayList<Mat>(4)
            Core.split(accColor, outChs)
            for (c in 0 until 3) {
                Core.divide(outChs[c], denom, outChs[c])
            }
            val alpha = Mat()
            Imgproc.threshold(accWeight, alpha, 0.02, 255.0, Imgproc.THRESH_BINARY)
            outChs[3].release()
            outChs[3] = alpha
            Core.merge(outChs, accColor)
            outChs.forEach { it.release() }
            denom.release()
            accWeight.release()

            val out = Mat()
            accColor.convertTo(out, CvType.CV_8UC4)
            accColor.release()
            return out
        } catch (e: Exception) {
            Log.e(TAG, "Mosaic pipeline failed", e)
            releaseFeatures(features)
            transforms.forEach { it.release() }
            return null
        }
    }

    /**
     * AKAZE match + Lowe ratio + RANSAC between two frames.
     * Returns H mapping points in [a] to points in [b], or null if the pair
     * does not share enough confident matches.
     */
    private fun estimatePairHomography(
        matcher: DescriptorMatcher,
        a: FrameFeatures,
        b: FrameFeatures
    ): Mat? {
        val knn = ArrayList<MatOfDMatch>()
        matcher.knnMatch(a.descriptors, b.descriptors, knn, 2)
        val goodList = ArrayList<DMatch>(knn.size)
        for (m in knn) {
            val arr = m.toArray()
            if (arr.size == 2 && arr[0].distance < MATCH_RATIO * arr[1].distance) {
                goodList.add(arr[0])
            }
            m.release()
        }
        knn.clear()
        if (goodList.size < MIN_GOOD_MATCHES) return null

        val kpA = a.keypoints.toArray()
        val kpB = b.keypoints.toArray()
        val src = ArrayList<Point>(goodList.size)
        val dst = ArrayList<Point>(goodList.size)
        for (d in goodList) {
            src.add(kpA[d.queryIdx].pt)
            dst.add(kpB[d.trainIdx].pt)
        }
        val srcPts = MatOfPoint2f()
        val dstPts = MatOfPoint2f()
        srcPts.fromList(src)
        dstPts.fromList(dst)

        val inlierMask = Mat()
        val h = Calib3d.findHomography(
            srcPts, dstPts, Calib3d.RANSAC, RANSAC_REPROJ_THRESH, inlierMask, 2000, 0.995
        )
        // inlierMask is CV_8U: 255 = inlier. Read it out row-by-row.
        var inliers = 0
        if (!h.empty()) {
            val row = ByteArray(inlierMask.cols())
            for (y in 0 until inlierMask.rows()) {
                inlierMask.get(y, 0, row)
                for (v in row) if (v.toInt() != 0) inliers++
            }
        }
        inlierMask.release()
        srcPts.release()
        dstPts.release()
        if (h.empty() || inliers < MIN_GOOD_MATCHES / 2) {
            h.release()
            Log.d(TAG, "Pair rejected: ${goodList.size} good matches, $inliers inliers")
            return null
        }
        Log.d(TAG, "Pair matched: ${goodList.size} good, $inliers inliers")
        return h
    }

    /**
     * Exposure levelling: mean luminance ratio between consecutive frames,
     * chained into multiplicative gains clamped to [0.6, 1.6]. Frame 0 is
     * the reference.
     */
    private fun exposureGains(features: List<FrameFeatures>, usable: Int): FloatArray {
        val gains = FloatArray(usable) { 1f }
        if (usable < 2) return gains
        for (i in 1 until usable) {
            val a = meanLum(features[i - 1].gray)
            val b = meanLum(features[i].gray)
            gains[i] = if (a != null && b != null && b > 10f) {
                (gains[i - 1] * (a / b)).coerceIn(0.6f, 1.6f)
            } else {
                gains[i - 1]
            }
        }
        return gains
    }

    private fun meanLum(gray: Mat): Float? {
        val small = Mat()
        Imgproc.resize(gray, small, Size(64.0, 64.0))
        val mean = Core.mean(small).`val`[0].toFloat()
        small.release()
        return if (mean > 0f) mean else null
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
     * them away. Uncovered warp areas render as transparent/black padding, and
     * those bands would otherwise be stretched by the 2:1 letterbox step.
     */
    private fun cropAutoDetectedBorders(src: Mat): Mat {
        val small = Mat()
        Imgproc.resize(
            src, small, Size(
                (src.cols() / BORDER_SCAN_SAMPLE.toDouble()).coerceAtLeast(1.0),
                (src.rows() / BORDER_SCAN_SAMPLE.toDouble()).coerceAtLeast(1.0)
            )
        )
        val gray = Mat()
        Imgproc.cvtColor(small, gray, Imgproc.COLOR_RGBA2GRAY)

        val thresh = Mat()
        // Anything darker than near-black is padding.
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

        val cropW = min((right - left + 1) * BORDER_SCAN_SAMPLE, src.cols())
        val cropH = min((bottom - top + 1) * BORDER_SCAN_SAMPLE, src.rows())
        if (cropW <= 0 || cropH <= 0 || (cropW == src.cols() && cropH == src.rows())) {
            return src.clone()
        }
        return src.submat(0, cropH, 0, cropW).clone()
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
     * means the sweep did not close. -1 when it cannot be measured.
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
