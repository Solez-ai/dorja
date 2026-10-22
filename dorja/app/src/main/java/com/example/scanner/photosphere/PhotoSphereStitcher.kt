package com.example.scanner.photosphere

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

data class StitchResult(
    val outputFile: File,
    val widthPx: Int,
    val heightPx: Int,
    val frameCount: Int,
    val longitudeSpanDegrees: Float,
    val latitudeSpanDegrees: Float,
    val centerLongitudeDegrees: Float,
    val centerLatitudeDegrees: Float,
)

/**
 * Pose-driven spherical equirectangular stitcher for Dorja's 360° Photo Sphere & Horizon Scanner,
 * matching `360-photo-app`'s default pipeline (`MultibandBlender` + `EquirectangularRenderer` +
 * `ExposureCompensation` + masked `UnsharpMask`).
 *
 * Why this produces clean, crystal-clear panoramas without any blocky/pixelated artifacts:
 * 1. Decodes each captured still at high resolution (`stitchFrameMaxEdgePx = 2048`) in its exact
 *    upright portrait orientation (`decodeUprightBitmap` + `correctFovOrientation`).
 * 2. Balances per-frame brightness with `ExposureCompensation.solveGains`.
 * 3. Renders via **Burt–Adelson Laplacian Pyramid Multi-Band Blending**
 *    ([MultibandEquirectangularRenderer]): high frequencies (text, clock hands, furniture edges)
 *    are blended across a tight transition at the center-dominance boundary while low frequencies
 *    (wall illumination & color gradients) blend smoothly across the overlap — **zero coarse
 *    label-grid stair-step blocks (`11×11` pixelation) and zero ghosting**.
 * 4. Applies a masked Unsharp Mask pass and smooth zenith/nadir spherical cap synthesis so
 *    looking straight up or down in Normal or Fisheye mode never shows black voids.
 */
class PhotoSphereStitcher(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private class DecodedFrame(
        val pixels: IntArray,
        val width: Int,
        val height: Int,
        val basis: CameraBasis,
        val intrinsics: FrameIntrinsics,
        val meanLuma: Double,
    )

    companion object {
        private const val UNSHARP_AMOUNT = 0.35f
    }

    suspend fun stitchToFile(
        frames: List<CapturedFrame>,
        optics: SphereOptics,
        scope: SphereCaptureScope,
        outputFile: File,
        deviceProfile: SphereDeviceProfile = SphereDeviceProfile.forDevice(),
        pivotModel: PivotModel = PivotModel.HandheldBodySwivel,
        onProgress: (Float) -> Unit = {},
    ): StitchResult = withContext(dispatcher) {
        require(frames.isNotEmpty()) { "At least 1 captured frame is required to stitch." }
        onProgress(0.03f)

        val decoded = ArrayList<DecodedFrame>(frames.size)
        var horizontalFov = optics.fieldOfView.horizontalDegrees
        var verticalFov = optics.fieldOfView.verticalDegrees
        var fovChecked = false

        for ((index, captured) in frames.withIndex()) {
            coroutineContext.ensureActive()
            val bitmap = decodeUprightBitmap(
                bytes = captured.imageBytes,
                maxEdgePx = deviceProfile.stitchFrameMaxEdgePx,
                fallbackRotationDegrees = optics.portraitRotationDegrees,
            ) ?: continue

            val w = bitmap.width
            val h = bitmap.height
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            bitmap.recycle()

            if (!fovChecked) {
                val corrected = correctFovOrientation(w, h, horizontalFov, verticalFov)
                if (corrected != null) {
                    horizontalFov = corrected.first
                    verticalFov = corrected.second
                }
                fovChecked = true
            }

            val frameFov = if (w > h && horizontalFov < verticalFov) {
                FieldOfView(verticalFov, horizontalFov)
            } else if (h > w && horizontalFov > verticalFov) {
                FieldOfView(verticalFov, horizontalFov)
            } else {
                FieldOfView(horizontalFov, verticalFov)
            }

            // Use pinhole intrinsics by default (matching 360-photo-app's distortionEnabled = false
            // default, since OEM LENS_DISTORTION tables often over-warp frame borders).
            val intrinsics = FrameIntrinsics.fromFieldOfView(
                widthPx = w,
                heightPx = h,
                horizontalFovDegrees = frameFov.horizontalDegrees,
                verticalFovDegrees = frameFov.verticalDegrees,
            )

            val pose = CameraPose(
                yawDegrees = captured.orientation.yawDegrees,
                pitchDegrees = captured.orientation.pitchDegrees,
                rollDegrees = captured.orientation.rollDegrees,
                matrix = captured.orientation.cameraBasis?.let { b ->
                    DoubleArray(b.size) { b[it].toDouble() }
                },
            )
            val basis = CameraBasis.of(pose)
            val meanLuma = computeMeanLuminance(pixels, w, h)

            decoded.add(
                DecodedFrame(
                    pixels = pixels,
                    width = w,
                    height = h,
                    basis = basis,
                    intrinsics = intrinsics,
                    meanLuma = meanLuma,
                )
            )
            onProgress(0.03f + 0.17f * ((index + 1).toFloat() / frames.size))
        }

        require(decoded.isNotEmpty()) { "Failed to decode captured frames." }

        // 1. Solve global exposure gain compensation across overlapping frame pairs
        val overlapPairs = ArrayList<Pair<Int, Int>>()
        for (i in 0 until decoded.size) {
            for (j in i + 1 until decoded.size) {
                if (angularOverlap(
                        decoded[i].basis,
                        decoded[j].basis,
                        horizontalFov,
                        verticalFov,
                    ) != null
                ) {
                    overlapPairs.add(i to j)
                }
            }
        }
        val gains = ExposureCompensation.solveGains(
            meanLuma = DoubleArray(decoded.size) { decoded[it].meanLuma },
            edges = overlapPairs,
        )

        // 2. Full 360° × 180° 2:1 equirectangular canvas
        val canvasWidth = deviceProfile.stitchCanvasWidthPx
        val canvasHeight = deviceProfile.stitchCanvasHeightPx
        val pivotRatio = pivotModel.ratio
        val longitudeSpanDegrees = 360f
        val centerLongitudeDegrees = 0f
        val latitudeSpanDegrees = 180f
        val centerLatitudeDegrees = 0f

        val frameInputs = decoded.mapIndexed { idx, frame ->
            val footprint = FrameFootprint.compute(
                basis = frame.basis,
                intrinsics = frame.intrinsics,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                marginPx = 2,
                pivotRatio = pivotRatio,
                longitudeSpanDegrees = longitudeSpanDegrees,
                centerLongitudeDegrees = centerLongitudeDegrees,
                latitudeSpanDegrees = latitudeSpanDegrees,
                centerLatitudeDegrees = centerLatitudeDegrees,
            )
            MultibandEquirectangularRenderer.FrameInput(
                pixels = frame.pixels,
                width = frame.width,
                height = frame.height,
                basis = frame.basis,
                intrinsics = frame.intrinsics,
                footprint = footprint,
                gain = gains.getOrElse(idx) { 1f },
            )
        }

        val outPixels = IntArray(canvasWidth * canvasHeight)
        val rowHasCoverage = BooleanArray(canvasHeight)
        val fallbackBackdrop = computeNeutralBackdropColor(decoded, gains)

        // 3. Render with Burt–Adelson Laplacian Pyramid Multi-Band Blending (smooth low-frequency
        // illumination transitions + crisp full-resolution high-frequency detail, zero blocky seams)
        val callerContext = coroutineContext
        MultibandEquirectangularRenderer.renderFullSphere(
            frames = frameInputs,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            pivotRatio = pivotRatio,
            longitudeSpanDegrees = longitudeSpanDegrees,
            centerLongitudeDegrees = centerLongitudeDegrees,
            latitudeSpanDegrees = latitudeSpanDegrees,
            centerLatitudeDegrees = centerLatitudeDegrees,
            outPixels = outPixels,
            rowHasCoverage = rowHasCoverage,
            onProgress = { p ->
                onProgress(0.20f + 0.68f * p)
            },
            checkCancelled = {
                callerContext.ensureActive()
            },
        )

        // 4. Apply masked Unsharp Mask on all captured pixels so fine room details are tack-sharp
        coroutineContext.ensureActive()
        onProgress(0.90f)
        applyMaskedUnsharpMask(outPixels, canvasWidth, canvasHeight, UNSHARP_AMOUNT)

        // 5. Smoothly fill any uncaptured zenith/nadir spherical caps
        coroutineContext.ensureActive()
        onProgress(0.95f)
        fillUncapturedSphericalCaps(outPixels, canvasWidth, canvasHeight, rowHasCoverage, fallbackBackdrop)

        // 6. Write high-quality JPEG atomically and inject GPano XMP metadata
        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        val outBitmap = Bitmap.createBitmap(outPixels, canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(tempFile).use { fos ->
                outBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                fos.flush()
            }
        } finally {
            outBitmap.recycle()
        }

        if (!tempFile.renameTo(outputFile)) {
            tempFile.copyTo(outputFile, overwrite = true)
            tempFile.delete()
        }

        GPanoXmpInjector.inject(
            jpegFile = outputFile,
            croppedWidthPx = canvasWidth,
            croppedHeightPx = canvasHeight,
            longitudeSpanDegrees = longitudeSpanDegrees,
            latitudeSpanDegrees = latitudeSpanDegrees,
            centerLatitudeDegrees = centerLatitudeDegrees,
        )

        onProgress(1.0f)

        StitchResult(
            outputFile = outputFile,
            widthPx = canvasWidth,
            heightPx = canvasHeight,
            frameCount = decoded.size,
            longitudeSpanDegrees = longitudeSpanDegrees,
            latitudeSpanDegrees = latitudeSpanDegrees,
            centerLongitudeDegrees = centerLongitudeDegrees,
            centerLatitudeDegrees = centerLatitudeDegrees,
        )
    }

    private fun decodeUprightBitmap(
        bytes: ByteArray,
        maxEdgePx: Int,
        fallbackRotationDegrees: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxEdgePx) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null

        val exifOrientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_UNDEFINED)

        val matrix = Matrix()
        var rotates90 = false
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90f)
                rotates90 = true
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
                rotates90 = true
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1f, -1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                rotates90 = true
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
                rotates90 = true
            }
        }

        val widthAfterExif = if (rotates90) raw.height else raw.width
        val heightAfterExif = if (rotates90) raw.width else raw.height
        if (widthAfterExif > heightAfterExif && fallbackRotationDegrees % 180 != 0) {
            matrix.postRotate(fallbackRotationDegrees.toFloat())
        }

        val longest = max(raw.width, raw.height)
        val scale = if (longest > maxEdgePx) maxEdgePx.toFloat() / longest.toFloat() else 1f
        if (scale < 0.999f) {
            matrix.postScale(scale, scale)
        }

        if (matrix.isIdentity) return raw

        val transformed = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (transformed !== raw) raw.recycle()
        return transformed
    }

    private fun correctFovOrientation(
        widthPx: Int,
        heightPx: Int,
        horizontalFovDegrees: Float,
        verticalFovDegrees: Float,
    ): Pair<Float, Float>? {
        if (widthPx <= 0 || heightPx <= 0) return null
        val horizontalTan = tan(Math.toRadians(horizontalFovDegrees / 2.0))
        val verticalTan = tan(Math.toRadians(verticalFovDegrees / 2.0))
        if (horizontalTan <= 0.0 || verticalTan <= 0.0) return null
        val horizontalFocal = widthPx / 2.0 / horizontalTan
        val verticalFocal = heightPx / 2.0 / verticalTan
        val ratio =
            if (horizontalFocal >= verticalFocal) verticalFocal / horizontalFocal
            else horizontalFocal / verticalFocal
        val aspect = widthPx.toDouble() / heightPx
        val transposedRatio = if (aspect >= 1.0) 1.0 / (aspect * aspect) else aspect * aspect
        if (ratio >= sqrt(transposedRatio)) return null
        return verticalFovDegrees to horizontalFovDegrees
    }

    private fun applyMaskedUnsharpMask(
        pixels: IntArray,
        width: Int,
        height: Int,
        amount: Float,
    ) {
        if (amount <= 0f || width < 3 || height < 3) return
        val copy = pixels.copyOf()
        for (row in 1 until height - 1) {
            val rowOff = row * width
            val prevOff = (row - 1) * width
            val nextOff = (row + 1) * width
            for (col in 1 until width - 1) {
                val idx = rowOff + col
                val center = copy[idx]
                if ((center ushr 24) == 0) continue

                val cLeft = copy[idx - 1]
                val cRight = copy[idx + 1]
                val cUp = copy[prevOff + col]
                val cDown = copy[nextOff + col]
                if ((cLeft ushr 24) == 0 || (cRight ushr 24) == 0 ||
                    (cUp ushr 24) == 0 || (cDown ushr 24) == 0
                ) {
                    continue
                }

                val rC = (center ushr 16) and 0xFF
                val gC = (center ushr 8) and 0xFF
                val bC = center and 0xFF
                val luma = 0.2126f * rC + 0.7152f * gC + 0.0722f * bC
                if (luma < 20f) continue

                val blurR = (rC * 4 +
                    ((cLeft ushr 16) and 0xFF) +
                    ((cRight ushr 16) and 0xFF) +
                    ((cUp ushr 16) and 0xFF) +
                    ((cDown ushr 16) and 0xFF)) * 0.125f
                val blurG = (gC * 4 +
                    ((cLeft ushr 8) and 0xFF) +
                    ((cRight ushr 8) and 0xFF) +
                    ((cUp ushr 8) and 0xFF) +
                    ((cDown ushr 8) and 0xFF)) * 0.125f
                val blurB = (bC * 4 +
                    (cLeft and 0xFF) +
                    (cRight and 0xFF) +
                    (cUp and 0xFF) +
                    (cDown and 0xFF)) * 0.125f

                val outR = (rC + amount * (rC - blurR)).roundToInt().coerceIn(0, 255)
                val outG = (gC + amount * (gC - blurG)).roundToInt().coerceIn(0, 255)
                val outB = (bC + amount * (bC - blurB)).roundToInt().coerceIn(0, 255)
                pixels[idx] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }
    }

    private fun fillUncapturedSphericalCaps(
        pixels: IntArray,
        width: Int,
        height: Int,
        rowHasCoverage: BooleanArray,
        fallbackBackdrop: Int,
    ) {
        val firstCoveredRow = rowHasCoverage.indexOfFirst { it }
        if (firstCoveredRow < 0) {
            pixels.fill(fallbackBackdrop)
            return
        }

        // 1. Smoothly blend any horizontal gaps inside partially-covered rows
        for (row in 0 until height) {
            if (!rowHasCoverage[row]) continue
            val rowOff = row * width
            var firstCol = -1
            for (c in 0 until width) {
                if ((pixels[rowOff + c] ushr 24) != 0) {
                    firstCol = c
                    break
                }
            }
            if (firstCol < 0) continue

            var lastColor = pixels[rowOff + firstCol]
            for (step in 0 until width) {
                val c = (firstCol + step) % width
                val px = pixels[rowOff + c]
                if ((px ushr 24) != 0) {
                    lastColor = px
                } else {
                    pixels[rowOff + c] = lastColor
                }
            }
        }

        // 2. Fill any empty internal rows between firstCoveredRow and lastCoveredRow
        val lastCoveredRow = rowHasCoverage.indexOfLast { it }
        var lastValidRow = firstCoveredRow
        for (row in firstCoveredRow..lastCoveredRow) {
            if (rowHasCoverage[row]) {
                lastValidRow = row
            } else {
                System.arraycopy(pixels, lastValidRow * width, pixels, row * width, width)
            }
        }

        // 3. Smoothly synthesize the Zenith Cap (rows 0 until firstCoveredRow) using a continuous
        // horizontal prefix-sum box average so there are zero step artifacts
        if (firstCoveredRow > 0) {
            val topEdgeOff = firstCoveredRow * width
            var avgR = 0.0
            var avgG = 0.0
            var avgB = 0.0
            val edgeR = IntArray(width)
            val edgeG = IntArray(width)
            val edgeB = IntArray(width)
            for (c in 0 until width) {
                val p = pixels[topEdgeOff + c]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                edgeR[c] = r
                edgeG[c] = g
                edgeB[c] = b
                avgR += r
                avgG += g
                avgB += b
            }
            avgR /= width
            avgG /= width
            avgB /= width

            for (row in 0 until firstCoveredRow) {
                val t = row.toDouble() / firstCoveredRow.toDouble()
                val smoothT = t * t * (3.0 - 2.0 * t)
                val rowOff = row * width
                val step = max(1, ((1.0 - smoothT) * (width / 24.0)).roundToInt())
                for (c in 0 until width) {
                    var sr = 0
                    var sg = 0
                    var sb = 0
                    for (k in -2..2) {
                        val idx = ((c + k * step) % width + width) % width
                        sr += edgeR[idx]
                        sg += edgeG[idx]
                        sb += edgeB[idx]
                    }
                    val blurR = sr * 0.2
                    val blurG = sg * 0.2
                    val blurB = sb * 0.2

                    val r = (avgR * (1.0 - smoothT) + blurR * smoothT).roundToInt().coerceIn(0, 255)
                    val g = (avgG * (1.0 - smoothT) + blurG * smoothT).roundToInt().coerceIn(0, 255)
                    val b = (avgB * (1.0 - smoothT) + blurB * smoothT).roundToInt().coerceIn(0, 255)
                    pixels[rowOff + c] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        // 4. Smoothly synthesize the Nadir Cap (rows lastCoveredRow + 1 until height)
        if (lastCoveredRow < height - 1) {
            val botEdgeOff = lastCoveredRow * width
            var avgR = 0.0
            var avgG = 0.0
            var avgB = 0.0
            val edgeR = IntArray(width)
            val edgeG = IntArray(width)
            val edgeB = IntArray(width)
            for (c in 0 until width) {
                val p = pixels[botEdgeOff + c]
                val r = (p ushr 16) and 0xFF
                val g = (p ushr 8) and 0xFF
                val b = p and 0xFF
                edgeR[c] = r
                edgeG[c] = g
                edgeB[c] = b
                avgR += r
                avgG += g
                avgB += b
            }
            avgR /= width
            avgG /= width
            avgB /= width

            val span = (height - 1 - lastCoveredRow).coerceAtLeast(1)
            for (row in (lastCoveredRow + 1) until height) {
                val t = 1.0 - (row - lastCoveredRow).toDouble() / span.toDouble()
                val smoothT = t * t * (3.0 - 2.0 * t)
                val rowOff = row * width
                val step = max(1, ((1.0 - smoothT) * (width / 24.0)).roundToInt())
                for (c in 0 until width) {
                    var sr = 0
                    var sg = 0
                    var sb = 0
                    for (k in -2..2) {
                        val idx = ((c + k * step) % width + width) % width
                        sr += edgeR[idx]
                        sg += edgeG[idx]
                        sb += edgeB[idx]
                    }
                    val blurR = sr * 0.2
                    val blurG = sg * 0.2
                    val blurB = sb * 0.2

                    val r = (avgR * (1.0 - smoothT) + blurR * smoothT).roundToInt().coerceIn(0, 255)
                    val g = (avgG * (1.0 - smoothT) + blurG * smoothT).roundToInt().coerceIn(0, 255)
                    val b = (avgB * (1.0 - smoothT) + blurB * smoothT).roundToInt().coerceIn(0, 255)
                    pixels[rowOff + c] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
    }

    private fun computeMeanLuminance(pixels: IntArray, width: Int, height: Int): Double {
        if (pixels.isEmpty()) return 0.0
        var sum = 0.0
        var count = 0
        val step = 4
        var y = 0
        while (y < height) {
            val rowOffset = y * width
            var x = 0
            while (x < width) {
                val c = pixels[rowOffset + x]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                sum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                count++
                x += step
            }
            y += step
        }
        return if (count > 0) sum / count else 0.0
    }

    private fun computeNeutralBackdropColor(frames: List<DecodedFrame>, gains: FloatArray): Int {
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0
        for (i in frames.indices) {
            val frame = frames[i]
            val gain = gains.getOrElse(i) { 1f }.toDouble()
            val step = max(1, frame.pixels.size / 256)
            var idx = 0
            while (idx < frame.pixels.size) {
                val c = frame.pixels[idx]
                sumR += ((c ushr 16) and 0xFF) * gain
                sumG += ((c ushr 8) and 0xFF) * gain
                sumB += (c and 0xFF) * gain
                count++
                idx += step
            }
        }
        if (count == 0) return 0xFF141820.toInt()
        val r = (sumR / count * 0.75).roundToInt().coerceIn(24, 235)
        val g = (sumG / count * 0.75).roundToInt().coerceIn(24, 235)
        val b = (sumB / count * 0.75).roundToInt().coerceIn(24, 235)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
