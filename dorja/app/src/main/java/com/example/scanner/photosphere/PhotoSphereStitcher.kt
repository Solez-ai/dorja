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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
 * Pose-driven spherical equirectangular stitcher for Dorja's Horizon Ring & Photo Sphere scanner.
 *
 * Uses the captured rotation-vector camera poses, `PivotModel.HandheldBodySwivel`, camera intrinsics
 * + radial distortion, photometric gain compensation (`ExposureCompensation`), and sharp-seam
 * feathered spherical back-projection. Runs in pure Kotlin on `Bitmap` pixel buffers so it has zero
 * dependency on OpenCV Java wrappers and never fails on featureless indoor walls.
 */
class PhotoSphereStitcher(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private class DecodedFrame(
        val pixels: IntArray,
        val width: Int,
        val height: Int,
        var basis: CameraBasis,
        val intrinsics: FrameIntrinsics,
        val meanLuma: Double,
    )

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
        val fov = optics.fieldOfView

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

            val frameFov = if (w > h && fov.horizontalDegrees < fov.verticalDegrees) {
                fov.transposed()
            } else if (h > w && fov.horizontalDegrees > fov.verticalDegrees) {
                fov.transposed()
            } else {
                fov
            }

            val intrinsics = FrameIntrinsics.forLens(
                widthPx = w,
                heightPx = h,
                horizontalFovDegrees = frameFov.horizontalDegrees,
                verticalFovDegrees = frameFov.verticalDegrees,
                distortion = optics.radialDistortion,
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

        // Solve global exposure gain compensation across overlapping frame pairs
        val overlapPairs = ArrayList<Pair<Int, Int>>()
        for (i in 0 until decoded.size) {
            for (j in i + 1 until decoded.size) {
                if (angularOverlap(
                        decoded[i].basis,
                        decoded[j].basis,
                        fov.horizontalDegrees,
                        fov.verticalDegrees,
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

        // Refine camera poses across overlapping frames (sub-degree visual alignment,
        // bounded to ±3° so featureless walls remain anchored to the gyro pose)
        if (decoded.size >= 2 && overlapPairs.isNotEmpty()) {
            refineOverlappingPoses(decoded, overlapPairs, pivotModel.ratio)
        }

        // Determine equirectangular canvas angular window and pixel dimensions
        val fullCanvasWidth = deviceProfile.stitchCanvasWidthPx
        val fullCanvasHeight = deviceProfile.stitchCanvasHeightPx
        val pivotRatio = pivotModel.ratio

        val longitudeSpanDegrees = 360f
        val centerLongitudeDegrees = 0f
        val latitudeSpanDegrees: Float
        val centerLatitudeDegrees: Float
        val canvasWidth: Int
        val canvasHeight: Int

        if (scope == SphereCaptureScope.Ring) {
            // Compute the vertical latitude band actually covered by the horizon ring frames
            var minRow = fullCanvasHeight
            var maxRow = 0
            for (frame in decoded) {
                val fp = FrameFootprint.compute(
                    basis = frame.basis,
                    intrinsics = frame.intrinsics,
                    canvasWidth = fullCanvasWidth,
                    canvasHeight = fullCanvasHeight,
                    marginPx = 2,
                    pivotRatio = pivotRatio,
                )
                if (!fp.isEmpty) {
                    minRow = min(minRow, fp.startRow)
                    maxRow = max(maxRow, fp.startRow + fp.rowSpan)
                }
            }
            if (minRow >= maxRow) {
                val halfBand = (fov.verticalDegrees * 0.52f).coerceIn(22f, 85f)
                latitudeSpanDegrees = halfBand * 2f
                centerLatitudeDegrees = 0f
            } else {
                val topLat = Equirectangular.latitudeDegrees(minRow, fullCanvasHeight).toFloat()
                val bottomLat = Equirectangular.latitudeDegrees((maxRow - 1).coerceAtLeast(minRow), fullCanvasHeight).toFloat()
                // Trim outer 4% top/bottom so minor hand pitch wobble gives a clean rectangular horizon strip
                val rawSpan = (topLat - bottomLat).coerceIn(36f, 160f)
                latitudeSpanDegrees = (rawSpan * 0.92f).coerceIn(36f, 160f)
                centerLatitudeDegrees = ((topLat + bottomLat) * 0.5f).coerceIn(-30f, 30f)
            }
            canvasWidth = fullCanvasWidth
            canvasHeight = ((canvasWidth * (latitudeSpanDegrees / 360f)).roundToInt())
                .coerceIn(512, fullCanvasHeight)
        } else {
            latitudeSpanDegrees = 180f
            centerLatitudeDegrees = 0f
            canvasWidth = fullCanvasWidth
            canvasHeight = fullCanvasHeight
        }

        // Precompute footprints on the target canvas
        val footprints = decoded.map { frame ->
            FrameFootprint.compute(
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
        }

        // Precompute lon/lat trig tables for the canvas
        val sinLon = DoubleArray(canvasWidth)
        val cosLon = DoubleArray(canvasWidth)
        for (col in 0 until canvasWidth) {
            val lonRad = Math.toRadians(
                Equirectangular.longitudeDegrees(
                    col = col,
                    width = canvasWidth,
                    longitudeSpanDegrees = longitudeSpanDegrees,
                    centerLongitudeDegrees = centerLongitudeDegrees,
                )
            )
            sinLon[col] = sin(lonRad)
            cosLon[col] = cos(lonRad)
        }

        val outPixels = IntArray(canvasWidth * canvasHeight)
        val backgroundRgb = computeNeutralBackdropColor(decoded, gains)

        // Render row-by-row with sharp raised-power feather blending (power 6 sharpness weight so
        // the frame whose optical axis is closest to each ray dominates the seam, preventing
        // double-edges while keeping smooth transitions across uniform wall surfaces)
        for (row in 0 until canvasHeight) {
            if ((row and 15) == 0) {
                coroutineContext.ensureActive()
                onProgress(0.20f + 0.72f * (row.toFloat() / canvasHeight))
            }

            val latRad = Math.toRadians(
                Equirectangular.latitudeDegrees(
                    row = row,
                    height = canvasHeight,
                    latitudeSpanDegrees = latitudeSpanDegrees,
                    centerLatitudeDegrees = centerLatitudeDegrees,
                )
            )
            val cosLat = cos(latRad)
            val sinLat = sin(latRad)
            val rowOffset = row * canvasWidth

            // Filter frames whose row footprint includes this row
            var activeFrameCount = 0
            val activeIndices = IntArray(decoded.size)
            for (fi in decoded.indices) {
                val fp = footprints[fi]
                if (!fp.isEmpty && row >= fp.startRow && row < fp.startRow + fp.rowSpan) {
                    activeIndices[activeFrameCount++] = fi
                }
            }

            if (activeFrameCount == 0) {
                for (col in 0 until canvasWidth) {
                    outPixels[rowOffset + col] = backgroundRgb
                }
                continue
            }

            for (col in 0 until canvasWidth) {
                val wx = sinLon[col] * cosLat
                val wy = cosLon[col] * cosLat
                val wz = sinLat

                var sumR = 0.0
                var sumG = 0.0
                var sumB = 0.0
                var sumW = 0.0

                for (k in 0 until activeFrameCount) {
                    val fi = activeIndices[k]
                    val frame = decoded[fi]
                    val basis = frame.basis
                    val depth = basis.depthOf(wx, wy, wz) - pivotRatio
                    if (depth <= MIN_DEPTH) continue

                    val intr = frame.intrinsics
                    val cx = intr.centerXPx
                    val cy = intr.centerYPx
                    val idealCol = cx + intr.focalXPx * basis.lateralOf(wx, wy, wz) / depth
                    val idealRow = cy - intr.focalYPx * basis.verticalOf(wx, wy, wz) / depth

                    val across = 1.0 - abs(idealCol - cx) / cx
                    if (across <= 0.0) continue
                    val down = 1.0 - abs(idealRow - cy) / cy
                    if (down <= 0.0) continue

                    val radial = intr.radial
                    val srcCol: Double
                    val srcRow: Double
                    if (radial != null) {
                        val dx = idealCol - cx
                        val dy = idealRow - cy
                        val r2 = dx * dx + dy * dy
                        val factor = radialFactor(radial, r2)
                        srcCol = cx + dx * factor
                        srcRow = cy + dy * factor
                    } else {
                        srcCol = idealCol
                        srcRow = idealRow
                    }

                    if (srcCol < 0.0 || srcCol >= intr.widthPx - 1.001 ||
                        srcRow < 0.0 || srcRow >= intr.heightPx - 1.001
                    ) {
                        continue
                    }

                    val linear = across * down
                    val w2 = linear * linear
                    // Power-6 center-weighted seam dominance: sharp on foreground features,
                    // smooth across walls
                    val weight = w2 * w2 * w2
                    val gain = gains[fi].toDouble()

                    val x0 = floor(srcCol).toInt().coerceIn(0, frame.width - 1)
                    val y0 = floor(srcRow).toInt().coerceIn(0, frame.height - 1)
                    val x1 = (x0 + 1).coerceAtMost(frame.width - 1)
                    val y1 = (y0 + 1).coerceAtMost(frame.height - 1)
                    val fx = srcCol - x0
                    val fy = srcRow - y0
                    val oneMinusFx = 1.0 - fx
                    val oneMinusFy = 1.0 - fy

                    val p00 = frame.pixels[y0 * frame.width + x0]
                    val p10 = frame.pixels[y0 * frame.width + x1]
                    val p01 = frame.pixels[y1 * frame.width + x0]
                    val p11 = frame.pixels[y1 * frame.width + x1]

                    val w00 = oneMinusFx * oneMinusFy
                    val w10 = fx * oneMinusFy
                    val w01 = oneMinusFx * fy
                    val w11 = fx * fy

                    val r = (((p00 ushr 16) and 0xFF) * w00 +
                        ((p10 ushr 16) and 0xFF) * w10 +
                        ((p01 ushr 16) and 0xFF) * w01 +
                        ((p11 ushr 16) and 0xFF) * w11) * gain
                    val g = (((p00 ushr 8) and 0xFF) * w00 +
                        ((p10 ushr 8) and 0xFF) * w10 +
                        ((p01 ushr 8) and 0xFF) * w01 +
                        ((p11 ushr 8) and 0xFF) * w11) * gain
                    val b = ((p00 and 0xFF) * w00 +
                        (p10 and 0xFF) * w10 +
                        (p01 and 0xFF) * w01 +
                        (p11 and 0xFF) * w11) * gain

                    sumR += r * weight
                    sumG += g * weight
                    sumB += b * weight
                    sumW += weight
                }

                outPixels[rowOffset + col] = if (sumW > 1e-9) {
                    val ir = (sumR / sumW).roundToInt().coerceIn(0, 255)
                    val ig = (sumG / sumW).roundToInt().coerceIn(0, 255)
                    val ib = (sumB / sumW).roundToInt().coerceIn(0, 255)
                    (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
                } else {
                    backgroundRgb
                }
            }
        }

        // Write JPEG atomically and inject GPano XMP metadata
        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        val outBitmap = Bitmap.createBitmap(outPixels, canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(tempFile).use { fos ->
                outBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
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
        while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > maxEdgePx * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts) ?: return null

        val exifRotation = readExifRotationDegrees(bytes) ?: fallbackRotationDegrees
        val longest = max(raw.width, raw.height)
        val scale = if (longest > maxEdgePx) maxEdgePx.toFloat() / longest.toFloat() else 1f

        if (exifRotation == 0 && scale >= 0.999f) return raw

        val matrix = Matrix().apply {
            if (scale < 0.999f) postScale(scale, scale)
            if (exifRotation != 0) postRotate(exifRotation.toFloat())
        }
        val transformed = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (transformed !== raw) raw.recycle()
        return transformed
    }

    private fun readExifRotationDegrees(bytes: ByteArray): Int? = runCatching {
        val exif = ExifInterface(ByteArrayInputStream(bytes))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_NORMAL -> 0
            else -> null
        }
    }.getOrNull()

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
        val r = (sumR / count * 0.45).roundToInt().coerceIn(16, 220)
        val g = (sumG / count * 0.45).roundToInt().coerceIn(16, 220)
        val b = (sumB / count * 0.45).roundToInt().coerceIn(16, 220)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun refineOverlappingPoses(
        frames: List<DecodedFrame>,
        overlapPairs: List<Pair<Int, Int>>,
        pivotRatio: Double,
    ) {
        val initialRotations = frames.map { it.basis.toRotationMatrix() }
        val edges = ArrayList<RotationMath.RotationEdge>()

        for ((i, j) in overlapPairs) {
            val fa = frames[i]
            val fb = frames[j]
            val midX = fa.basis.forwardX + fb.basis.forwardX
            val midY = fa.basis.forwardY + fb.basis.forwardY
            val midZ = fa.basis.forwardZ + fb.basis.forwardZ
            val midLen = sqrt(midX * midX + midY * midY + midZ * midZ)
            if (midLen < 1e-3) continue

            val centerLon = Equirectangular.longitudeOf(midX / midLen, midY / midLen)
            val centerLat = Equirectangular.latitudeOf(midZ / midLen)

            // Collect textured sample points in the overlap zone around (centerLon, centerLat)
            val sampleLon = DoubleArray(49)
            val sampleLat = DoubleArray(49)
            val sampleLumaA = DoubleArray(49)
            var sampleCount = 0

            for (gy in -3..3) {
                for (gx in -3..3) {
                    val lon = centerLon + gx * 3.0
                    val lat = (centerLat + gy * 4.0).coerceIn(-75.0, 75.0)
                    val dir = Equirectangular.direction(lon, lat)
                    val projA = projectDirection(fa.basis, fa.intrinsics, dir[0], dir[1], dir[2], pivotRatio)
                        ?: continue
                    val projB = projectDirection(fb.basis, fb.intrinsics, dir[0], dir[1], dir[2], pivotRatio)
                        ?: continue
                    if (projA[0] < 8 || projA[0] >= fa.width - 8 || projA[1] < 8 || projA[1] >= fa.height - 8) continue
                    if (projB[0] < 8 || projB[0] >= fb.width - 8 || projB[1] < 8 || projB[1] >= fb.height - 8) continue

                    val lumaCenter = sampleLuma(fa.pixels, fa.width, projA[0].toInt(), projA[1].toInt())
                    val lumaRight = sampleLuma(fa.pixels, fa.width, projA[0].toInt() + 3, projA[1].toInt())
                    val lumaDown = sampleLuma(fa.pixels, fa.width, projA[0].toInt(), projA[1].toInt() + 3)
                    val grad = abs(lumaRight - lumaCenter) + abs(lumaDown - lumaCenter)
                    if (grad < 10.0) continue

                    sampleLon[sampleCount] = lon
                    sampleLat[sampleCount] = lat
                    sampleLumaA[sampleCount] = lumaCenter
                    sampleCount++
                }
            }

            if (sampleCount < 8) continue

            var bestDLon = 0.0
            var bestDLat = 0.0
            var bestCost = Double.MAX_VALUE
            var zeroCost = 0.0

            val gainRatio = if (fb.meanLuma > 1.0) fa.meanLuma / fb.meanLuma else 1.0

            var dLat = -1.5
            while (dLat <= 1.501) {
                var dLon = -2.5
                while (dLon <= 2.501) {
                    var cost = 0.0
                    var valid = 0
                    for (s in 0 until sampleCount) {
                        val dir = Equirectangular.direction(sampleLon[s] + dLon, sampleLat[s] + dLat)
                        val projB = projectDirection(fb.basis, fb.intrinsics, dir[0], dir[1], dir[2], pivotRatio)
                            ?: continue
                        val lumaB = sampleLuma(fb.pixels, fb.width, projB[0].toInt(), projB[1].toInt()) * gainRatio
                        val diff = abs(sampleLumaA[s] - lumaB)
                        cost += diff
                        valid++
                    }
                    if (valid >= sampleCount * 3 / 4) {
                        // Small quadratic regularizer towards sensor pose (0, 0)
                        val reg = 0.35 * (dLon * dLon + dLat * dLat)
                        val avgCost = (cost / valid) + reg
                        if (abs(dLon) < 1e-4 && abs(dLat) < 1e-4) {
                            zeroCost = cost / valid
                        }
                        if (avgCost < bestCost) {
                            bestCost = avgCost
                            bestDLon = dLon
                            bestDLat = dLat
                        }
                    }
                    dLon += 0.25
                }
                dLat += 0.25
            }

            if (zeroCost > 0.0 && bestCost < zeroCost * 0.92) {
                val poseB = fb.basis.toPose()
                val adjustedBasisB = CameraBasis.of(
                    poseB.copy(
                        yawDegrees = normalizeDegrees(poseB.yawDegrees - bestDLon.toFloat()),
                        pitchDegrees = (poseB.pitchDegrees + bestDLat.toFloat()).coerceIn(-89f, 89f),
                        matrix = null,
                    )
                )
                val rotA = initialRotations[i]
                val rotB = adjustedBasisB.toRotationMatrix()
                // edge.rotation maps frame i's camera axes to frame j's: R_j^T * R_i
                val rel = RotationMath.multiply(RotationMath.transpose(rotB), rotA)
                edges.add(
                    RotationMath.RotationEdge(
                        from = i,
                        to = j,
                        rotation = rel,
                        weight = sampleCount.toDouble(),
                    )
                )
            } else {
                // Keep sensor relative rotation as a soft tie
                val rotA = initialRotations[i]
                val rotB = initialRotations[j]
                val rel = RotationMath.multiply(RotationMath.transpose(rotB), rotA)
                edges.add(
                    RotationMath.RotationEdge(
                        from = i,
                        to = j,
                        rotation = rel,
                        weight = 4.0,
                    )
                )
            }
        }

        if (edges.isNotEmpty()) {
            val refined = RotationMath.averageRotations(initialRotations, edges, iterations = 10)
            for (idx in frames.indices) {
                frames[idx].basis = CameraBasis.fromRotationMatrix(refined[idx])
            }
        }
    }

    private fun sampleLuma(pixels: IntArray, width: Int, x: Int, y: Int): Double {
        val c = pixels[y * width + x]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
