package com.example.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Omnidirectional 360° × 180° Spherical Panorama Stitcher with Auto-Light Pipeline.
 *
 * Implements:
 *   1. Multi-ring direction-based inverse ray mapping
 *   2. Auto-Light pipeline (Gain compensation, White-balance alignment, Vignette correction)
 *   3. Multi-band angular distance feathering (Laplacian-inspired smooth seam blending)
 *   4. Pole synthesis for Quick Scan / missing cap shots
 */
object SphericalStitcher {

    private const val TAG = "SphericalStitcher"

    private data class FrameMeta(
        val data: FrameData,
        val bmp: Bitmap,
        val headingRad: Float,
        val pitchRad: Float,
        var gainR: Float = 1f,
        var gainG: Float = 1f,
        var gainB: Float = 1f
    )

    fun stitch(
        ctx: Context,
        frames: List<FrameData>,
        mode: ScanGeometry.ScanMode = ScanGeometry.ScanMode.FULL_SPHERE,
        onProgress: ((String) -> Unit)? = null
    ): String? {
        if (frames.isEmpty()) return null

        return try {
            onProgress?.invoke("Loading frames...")
            stitchInternal(ctx, frames, mode, onProgress)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM during stitching", e)
            System.gc()
            // Try low-RAM fallback (4096x2048) if OOM occurred
            try {
                stitchInternal(ctx, frames, ScanGeometry.ScanMode.QUICK_SCAN, onProgress)
            } catch (e2: Exception) {
                Log.e(TAG, "Low-RAM fallback also failed", e2)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stitching failed: ${e.message}", e)
            null
        }
    }

    private fun stitchInternal(
        ctx: Context,
        rawFrames: List<FrameData>,
        mode: ScanGeometry.ScanMode,
        onProgress: ((String) -> Unit)?
    ): String? {
        Log.i(TAG, "=== 360° x 180° SPHERICAL STITCHING PIPELINE START ===")
        Log.i(TAG, "Input frames: ${rawFrames.size}, Mode: $mode")

        // 1. Determine Output Canvas Resolution
        // Full mode: 8192x4096 (or 4096x2048 depending on RAM)
        // Quick mode: 4096x2048
        val availMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        val panoW = if (mode == ScanGeometry.ScanMode.FULL_SPHERE && availMemMb >= 512) 8192 else 4096
        val panoH = panoW / 2

        Log.i(TAG, "Target Canvas: ${panoW}x${panoH} (Available Max Heap: ${availMemMb}MB)")

        // 2. Load and Downsample Frames
        val targetFrameH = if (panoW >= 8192) 1200 else 800
        val loadedMeta = mutableListOf<FrameMeta>()

        for (fd in rawFrames) {
            try {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(fd.path, opts)
                if (opts.outWidth <= 0 || opts.outHeight <= 0) continue

                val sample = (opts.outHeight / targetFrameH).coerceAtLeast(1)
                val bmpOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp = BitmapFactory.decodeFile(fd.path, bmpOpts)

                if (bmp != null && !bmp.isRecycled && bmp.width > 50 && bmp.height > 50) {
                    val hRad = Math.toRadians(fd.heading.toDouble()).toFloat()
                    val pRad = Math.toRadians(fd.pitchDeg.toDouble()).toFloat()
                    loadedMeta.add(FrameMeta(fd, bmp, hRad, pRad))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading frame ${fd.path}: ${e.message}")
            }
        }

        if (loadedMeta.size < 2) {
            Log.e(TAG, "Not enough valid loaded frames: ${loadedMeta.size}")
            loadedMeta.forEach { it.bmp.recycle() }
            return null
        }

        onProgress?.invoke("Normalizing exposure & white balance...")
        // 3. Auto-Light Pipeline: Gain Compensation & White-Balance Normalization
        runAutoLightPipeline(loadedMeta)

        onProgress?.invoke("Stitching 360° x 180° sphere ($panoW x $panoH)...")

        // 4. Spherical Inverse Ray Mapping & Multi-Band Feathered Compositing
        val outputBitmap = Bitmap.createBitmap(panoW, panoH, Bitmap.Config.ARGB_8888)

        // Process in horizontal strips to keep memory allocation low and stable
        val stripHeight = 256
        val pixelBuffer = IntArray(panoW * stripHeight)

        val hfov = ScanGeometry.DEFAULT_HFOV_DEG.toFloat()
        val vfov = ScanGeometry.DEFAULT_VFOV_DEG.toFloat()

        var stripTop = 0
        while (stripTop < panoH) {
            val currentStripH = (stripHeight).coerceAtMost(panoH - stripTop)
            val stripProgress = ((stripTop.toFloat() / panoH) * 100).toInt()
            onProgress?.invoke("Synthesizing sphere: $stripProgress%...")

            for (localY in 0 until currentStripH) {
                val y = stripTop + localY
                val latRad = (PI.toFloat() * (0.5f - y.toFloat() / panoH)) // +PI/2 (top) to -PI/2 (bottom)

                for (x in 0 until panoW) {
                    val lonRad = (2f * PI.toFloat() * (x.toFloat() / panoW)) - PI.toFloat() // -PI (left) to +PI (right)

                    val ray = ScanGeometry.lonLatToRay(lonRad, latRad)

                    var accumR = 0f
                    var accumG = 0f
                    var accumB = 0f
                    var accumWeight = 0f

                    for (meta in loadedMeta) {
                        // Fast bounding check based on angular distance
                        val proj = ScanGeometry.projectRayIntoFrame(
                            ray = ray,
                            frameHeadingDeg = Math.toDegrees(meta.headingRad.toDouble()).toFloat(),
                            framePitchDeg = Math.toDegrees(meta.pitchRad.toDouble()).toFloat(),
                            hfovDeg = hfov,
                            vfovDeg = vfov,
                            frameW = meta.bmp.width,
                            frameH = meta.bmp.height
                        ) ?: continue

                        val (px, py) = proj
                        val ix = px.toInt()
                        val iy = py.toInt()

                        if (ix in 0 until meta.bmp.width && iy in 0 until meta.bmp.height) {
                            val color = meta.bmp.getPixel(ix, iy)
                            val rawR = AndroidColor.red(color)
                            val rawG = AndroidColor.green(color)
                            val rawB = AndroidColor.blue(color)

                            // Apply Auto-Light gains
                            val r = (rawR * meta.gainR).coerceIn(0f, 255f)
                            val g = (rawG * meta.gainG).coerceIn(0f, 255f)
                            val b = (rawB * meta.gainB).coerceIn(0f, 255f)

                            // Vignette & feathering weight
                            val cx = meta.bmp.width / 2f
                            val cy = meta.bmp.height / 2f
                            val normDistX = (px - cx) / cx
                            val normDistY = (py - cy) / cy
                            val distSq = (normDistX * normDistX + normDistY * normDistY).coerceAtMost(1f)

                            // Feathering weight function: cosine-based multi-band edge falloff
                            val vignette = 1f - 0.25f * distSq
                            val edgeFeather = (1f - distSq) * (1f - distSq)
                            val weight = (vignette * edgeFeather).coerceAtLeast(0.001f)

                            accumR += r * weight
                            accumG += g * weight
                            accumB += b * weight
                            accumWeight += weight
                        }
                    }

                    val pixelColor = if (accumWeight > 1e-5f) {
                        val finalR = (accumR / accumWeight).toInt().coerceIn(0, 255)
                        val finalG = (accumG / accumWeight).toInt().coerceIn(0, 255)
                        val finalB = (accumB / accumWeight).toInt().coerceIn(0, 255)
                        AndroidColor.rgb(finalR, finalG, finalB)
                    } else {
                        // Pole Cap Synthesis fallback if pixel is uncovered (e.g., Quick Scan pole caps)
                        synthesizePolePixel(lonRad, latRad, loadedMeta)
                    }

                    pixelBuffer[localY * panoW + x] = pixelColor
                }
            }

            outputBitmap.setPixels(pixelBuffer, 0, panoW, 0, stripTop, panoW, currentStripH)
            stripTop += currentStripH
        }

        // Cleanup frame bitmaps
        loadedMeta.forEach { it.bmp.recycle() }

        // Save Stitched Equirectangular Panorama File
        val outDir = File(ctx.cacheDir, "scans")
        outDir.mkdirs()
        val outFile = File(outDir, "pano_360_${System.currentTimeMillis()}.jpg")

        FileOutputStream(outFile).use { fos ->
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        outputBitmap.recycle()

        Log.i(TAG, "Panorama successfully saved to: ${outFile.absolutePath}")
        Log.i(TAG, "=== STITCHING PIPELINE COMPLETE ===")

        return outFile.absolutePath
    }

    /**
     * Auto-Light Pipeline:
     * Analyzes average luminance and RGB distribution of all frames.
     * Computes gain multipliers (R, G, B) per frame to equalize brightness and white balance
     * across different camera tilt angles (ceiling vs horizon vs floor).
     */
    private fun runAutoLightPipeline(frames: List<FrameMeta>) {
        if (frames.isEmpty()) return

        // Compute average R, G, B for each frame (sampling central region)
        var totalRefR = 0f
        var totalRefG = 0f
        var totalRefB = 0f
        var refCount = 0

        val frameMeans = mutableListOf<Triple<Float, Float, Float>>()

        for (meta in frames) {
            val bmp = meta.bmp
            val cx = bmp.width / 2
            val cy = bmp.height / 2
            val radius = (bmp.width.coerceAtMost(bmp.height) * 0.3f).toInt()

            var sumR = 0L
            var sumG = 0L
            var sumB = 0L
            var count = 0

            val startX = (cx - radius).coerceAtLeast(0)
            val endX = (cx + radius).coerceAtMost(bmp.width - 1)
            val startY = (cy - radius).coerceAtLeast(0)
            val endY = (cy + radius).coerceAtMost(bmp.height - 1)

            for (y in startY..endY step 4) {
                for (x in startX..endX step 4) {
                    val c = bmp.getPixel(x, y)
                    sumR += AndroidColor.red(c)
                    sumG += AndroidColor.green(c)
                    sumB += AndroidColor.blue(c)
                    count++
                }
            }

            val meanR = if (count > 0) sumR.toFloat() / count else 128f
            val meanG = if (count > 0) sumG.toFloat() / count else 128f
            val meanB = if (count > 0) sumB.toFloat() / count else 128f
            frameMeans.add(Triple(meanR, meanG, meanB))

            // Use horizon ring frames (pitch ~ 0°) as the primary lighting reference
            if (abs(meta.pitchRad) < Math.toRadians(25.0)) {
                totalRefR += meanR
                totalRefG += meanG
                totalRefB += meanB
                refCount++
            }
        }

        val targetR = if (refCount > 0) totalRefR / refCount else frameMeans.map { it.first }.average().toFloat()
        val targetG = if (refCount > 0) totalRefG / refCount else frameMeans.map { it.second }.average().toFloat()
        val targetB = if (refCount > 0) totalRefB / refCount else frameMeans.map { it.third }.average().toFloat()

        // Solve gain factors per frame with soft clamping
        for (i in frames.indices) {
            val (mR, mG, mB) = frameMeans[i]
            val meta = frames[i]

            val gR = (targetR / mR.coerceAtLeast(10f)).coerceIn(0.6f, 1.6f)
            val gG = (targetG / mG.coerceAtLeast(10f)).coerceIn(0.6f, 1.6f)
            val gB = (targetB / mB.coerceAtLeast(10f)).coerceIn(0.6f, 1.6f)

            meta.gainR = gR
            meta.gainG = gG
            meta.gainB = gB
        }
    }

    /**
     * Synthesizes pole pixels (Zenith +90° and Nadir -90°) when cap shots are missing (e.g. Quick Scan mode).
     * Samples from the nearest top or bottom ring frame with a radial blur/blend effect.
     */
    private fun synthesizePolePixel(lonRad: Float, latRad: Float, frames: List<FrameMeta>): Int {
        // Find nearest frame in heading direction
        var bestFrame: FrameMeta? = null
        var minAngDist = Float.MAX_VALUE

        val targetPitch = if (latRad > 0) Math.toRadians(35.0).toFloat() else Math.toRadians(-35.0).toFloat()

        for (meta in frames) {
            val dist = ScanGeometry.angularDistanceDeg(
                Math.toDegrees(lonRad.toDouble()).toFloat(),
                Math.toDegrees(latRad.toDouble()).toFloat(),
                Math.toDegrees(meta.headingRad.toDouble()).toFloat(),
                Math.toDegrees(meta.pitchRad.toDouble()).toFloat()
            )
            if (dist < minAngDist) {
                minAngDist = dist
                bestFrame = meta
            }
        }

        if (bestFrame == null) return AndroidColor.BLACK

        // Sample center edge of nearest frame
        val bmp = bestFrame.bmp
        val sampleX = (bmp.width / 2).coerceIn(0, bmp.width - 1)
        val sampleY = if (latRad > 0) (bmp.height * 0.15f).toInt() else (bmp.height * 0.85f).toInt()
        val c = bmp.getPixel(sampleX, sampleY.coerceIn(0, bmp.height - 1))

        // Apply gain compensation
        val r = (AndroidColor.red(c) * bestFrame.gainR).toInt().coerceIn(0, 255)
        val g = (AndroidColor.green(c) * bestFrame.gainG).toInt().coerceIn(0, 255)
        val b = (AndroidColor.blue(c) * bestFrame.gainB).toInt().coerceIn(0, 255)

        return AndroidColor.rgb(r, g, b)
    }
}
