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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
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
 * Pose-driven spherical equirectangular stitcher for Dorja's 360° Photo Sphere & Horizon Scanner.
 *
 * Implements:
 * 1. Exact upright portrait frame decoding (`decodeUprightBitmap`) + `correctFovOrientation` so
 *    captured frames are never sideways or tilted against the rotation-vector pose.
 * 2. Global photometric exposure gain compensation (`ExposureCompensation.solveGains`).
 * 3. Multi-label graph-cut seam carving (`SeamSolver` + `SeamWeights` + `SeamFeather`) so each
 *    interior pixel is painted at 100% sharpness by a single winning frame, with a narrow feather
 *    only across the seam cut — eliminating wide-overlap ghosting and preserving all room details.
 * 4. Full `360° × 180°` spherical equirectangular output with smooth zenith/nadir spherical cap
 *    fill for any uncaptured pole gap, plus a masked Unsharp Mask pass for crisp detail.
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
        private const val LABEL_GRID_WIDTH = 384
        private const val SMOOTH_LAMBDA = 10.0
        private const val BORDER_PENALTY = 1000.0
        private const val FEATHER_FULL_RES_PIXELS = 14
        private const val MAX_SEAM_PASSES = 2
        private const val UNSHARP_AMOUNT = 0.32f
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

            val intrinsics = FrameIntrinsics.forLens(
                widthPx = w,
                heightPx = h,
                horizontalFovDegrees = frameFov.horizontalDegrees,
                verticalFovDegrees = frameFov.verticalDegrees,
                distortion = optics.radialDistortion,
            )

            // Use the exact quaternion-averaged sensor pose recorded during the stationary dwell.
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
            onProgress(0.03f + 0.15f * ((index + 1).toFloat() / frames.size))
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

        // 2. Always render onto a full 360° × 180° 2:1 equirectangular sphere so looking up/down
        // in both Normal and Fisheye viewer modes has complete spherical geometry with zero black cutoffs.
        val canvasWidth = deviceProfile.stitchCanvasWidthPx
        val canvasHeight = deviceProfile.stitchCanvasHeightPx
        val pivotRatio = pivotModel.ratio
        val longitudeSpanDegrees = 360f
        val centerLongitudeDegrees = 0f
        val latitudeSpanDegrees = 180f
        val centerLatitudeDegrees = 0f

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

        // 3. Compute Graph-Cut Seam Carving (`SeamSolver` + `SeamFeather`) so each pixel is painted
        // at 100% full detail by a single winning frame, with a narrow feather only across seam cuts.
        val seams = computeSeamWeights(
            frames = decoded,
            footprints = footprints,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            gains = gains,
            pivotRatio = pivotRatio,
            longitudeSpanDegrees = longitudeSpanDegrees,
            centerLongitudeDegrees = centerLongitudeDegrees,
            latitudeSpanDegrees = latitudeSpanDegrees,
            centerLatitudeDegrees = centerLatitudeDegrees,
            onProgress = { seamFrac ->
                onProgress(0.18f + 0.20f * seamFrac)
            },
        )

        // Precompute lon trig tables for the canvas
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
        val rowHasCoverage = BooleanArray(canvasHeight)
        val fallbackBackdrop = computeNeutralBackdropColor(decoded, gains)

        // 4. Render the sphere row-by-row using the seam-carved assignment
        for (row in 0 until canvasHeight) {
            if ((row and 15) == 0) {
                coroutineContext.ensureActive()
                onProgress(0.38f + 0.50f * (row.toFloat() / canvasHeight))
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
                    outPixels[rowOffset + col] = 0
                }
                continue
            }

            var anyPixelInRow = false
            for (col in 0 until canvasWidth) {
                val wx = sinLon[col] * cosLat
                val wy = cosLon[col] * cosLat
                val wz = sinLat

                var sumR = 0.0
                var sumG = 0.0
                var sumB = 0.0
                var sumW = 0.0

                // Fallback tracking in case seam grid cell had no label due to subsampling
                var bestFallbackR = 0.0
                var bestFallbackG = 0.0
                var bestFallbackB = 0.0
                var bestFallbackWeight = 0.0

                for (k in 0 until activeFrameCount) {
                    val fi = activeIndices[k]
                    val seamW = seams.weightFor(fi, row, col).toDouble()

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

                    if (seamW > 0.0) {
                        sumR += r * seamW
                        sumG += g * seamW
                        sumB += b * seamW
                        sumW += seamW
                    } else {
                        val centerDom = across * down
                        if (centerDom > bestFallbackWeight) {
                            bestFallbackWeight = centerDom
                            bestFallbackR = r
                            bestFallbackG = g
                            bestFallbackB = b
                        }
                    }
                }

                if (sumW > 1e-6) {
                    val ir = (sumR / sumW).roundToInt().coerceIn(0, 255)
                    val ig = (sumG / sumW).roundToInt().coerceIn(0, 255)
                    val ib = (sumB / sumW).roundToInt().coerceIn(0, 255)
                    outPixels[rowOffset + col] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
                    anyPixelInRow = true
                } else if (bestFallbackWeight > 0.0) {
                    val ir = bestFallbackR.roundToInt().coerceIn(0, 255)
                    val ig = bestFallbackG.roundToInt().coerceIn(0, 255)
                    val ib = bestFallbackB.roundToInt().coerceIn(0, 255)
                    outPixels[rowOffset + col] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
                    anyPixelInRow = true
                } else {
                    // Mark uncovered pixel with alpha = 0 so spherical pole/gap infill can fill it cleanly
                    outPixels[rowOffset + col] = 0
                }
            }
            rowHasCoverage[row] = anyPixelInRow
        }

        // 5. Apply masked Unsharp Mask on all captured pixels before pole infill so room details pop
        coroutineContext.ensureActive()
        onProgress(0.90f)
        applyMaskedUnsharpMask(outPixels, canvasWidth, canvasHeight, UNSHARP_AMOUNT)

        // 6. Smoothly fill any uncaptured zenith/nadir pole caps or small gaps by interpolating
        // from the nearest captured boundary pixels (so looking straight up/down never shows black voids)
        coroutineContext.ensureActive()
        onProgress(0.94f)
        fillUncapturedSphericalCaps(outPixels, canvasWidth, canvasHeight, rowHasCoverage, fallbackBackdrop)

        // 7. Write JPEG atomically and inject GPano XMP metadata
        outputFile.parentFile?.mkdirs()
        val tempFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        val outBitmap = Bitmap.createBitmap(outPixels, canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(tempFile).use { fos ->
                outBitmap.compress(Bitmap.CompressFormat.JPEG, 94, fos)
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

    private suspend fun computeSeamWeights(
        frames: List<DecodedFrame>,
        footprints: List<CanvasFootprint>,
        canvasWidth: Int,
        canvasHeight: Int,
        gains: FloatArray,
        pivotRatio: Double,
        longitudeSpanDegrees: Float,
        centerLongitudeDegrees: Float,
        latitudeSpanDegrees: Float,
        centerLatitudeDegrees: Float,
        onProgress: (Float) -> Unit,
    ): SeamWeights {
        val scale = max(1, (canvasWidth + LABEL_GRID_WIDTH - 1) / LABEL_GRID_WIDTH)
        val gridWidth = (canvasWidth + scale - 1) / scale
        val gridHeight = (canvasHeight + scale - 1) / scale
        val nodeCount = gridWidth * gridHeight

        val nodeLabelCount = IntArray(nodeCount)
        val nodeMean = FloatArray(nodeCount * 3)

        var capacity = max(nodeCount * 3, 16)
        var packedLabels = IntArray(capacity)
        var packedData = FloatArray(capacity)
        var packedColors = FloatArray(capacity * 3)
        var packedSize = 0

        val tempCover = IntArray(frames.size)
        val tempColor = FloatArray(frames.size * 3)
        val tempCol = DoubleArray(frames.size)
        val tempRow = DoubleArray(frames.size)

        for (gridRow in 0 until gridHeight) {
            if ((gridRow and 7) == 0) {
                coroutineContext.ensureActive()
                onProgress(0.6f * (gridRow.toFloat() / gridHeight))
            }
            val centerRow = min(gridRow * scale + scale / 2, canvasHeight - 1)
            val latitude = Math.toRadians(
                Equirectangular.latitudeDegrees(
                    centerRow, canvasHeight, latitudeSpanDegrees, centerLatitudeDegrees,
                )
            )
            val cosLat = cos(latitude)
            val sinLat = sin(latitude)

            for (gridCol in 0 until gridWidth) {
                val centerCol = min(gridCol * scale + scale / 2, canvasWidth - 1)
                val longitude = Math.toRadians(
                    Equirectangular.longitudeDegrees(
                        centerCol, canvasWidth, longitudeSpanDegrees, centerLongitudeDegrees,
                    )
                )
                val dirX = sin(longitude) * cosLat
                val dirY = cos(longitude) * cosLat
                val dirZ = sinLat

                var count = 0
                for (fi in frames.indices) {
                    val fp = footprints[fi]
                    if (fp.isEmpty || centerRow < fp.startRow || centerRow >= fp.startRow + fp.rowSpan) {
                        continue
                    }
                    val frame = frames[fi]
                    val basis = frame.basis
                    val intr = frame.intrinsics
                    val depth = basis.depthOf(dirX, dirY, dirZ) - pivotRatio
                    if (depth <= MIN_DEPTH) continue

                    val cx = intr.centerXPx
                    val cy = intr.centerYPx
                    val idealCol = cx + intr.focalXPx * basis.lateralOf(dirX, dirY, dirZ) / depth
                    val idealRow = cy - intr.focalYPx * basis.verticalOf(dirX, dirY, dirZ) / depth

                    var srcCol = idealCol
                    var srcRow = idealRow
                    val radial = intr.radial
                    if (radial != null) {
                        val dx = idealCol - cx
                        val dy = idealRow - cy
                        val factor = radialFactor(radial, dx * dx + dy * dy)
                        srcCol = cx + dx * factor
                        srcRow = cy + dy * factor
                    }

                    val colInt = srcCol.roundToInt()
                    val rowInt = srcRow.roundToInt()
                    if (colInt < 0 || colInt >= frame.width || rowInt < 0 || rowInt >= frame.height) continue

                    val pixel = frame.pixels[rowInt * frame.width + colInt]
                    val gain = gains.getOrElse(fi) { 1f }
                    tempCover[count] = fi
                    tempColor[count * 3] = ((pixel ushr 16) and 0xFF) * gain
                    tempColor[count * 3 + 1] = ((pixel ushr 8) and 0xFF) * gain
                    tempColor[count * 3 + 2] = (pixel and 0xFF) * gain
                    tempCol[count] = srcCol
                    tempRow[count] = srcRow
                    count++
                }

                val node = gridRow * gridWidth + gridCol
                if (count == 0) continue
                nodeLabelCount[node] = count

                var meanR = 0f
                var meanG = 0f
                var meanB = 0f
                for (k in 0 until count) {
                    meanR += tempColor[k * 3]
                    meanG += tempColor[k * 3 + 1]
                    meanB += tempColor[k * 3 + 2]
                }
                meanR /= count
                meanG /= count
                meanB /= count
                val meanBase = node * 3
                nodeMean[meanBase] = meanR
                nodeMean[meanBase + 1] = meanG
                nodeMean[meanBase + 2] = meanB

                for (k in 0 until count) {
                    if (packedSize >= capacity) {
                        capacity *= 2
                        packedLabels = packedLabels.copyOf(capacity)
                        packedData = packedData.copyOf(capacity)
                        packedColors = packedColors.copyOf(capacity * 3)
                    }
                    val r = tempColor[k * 3]
                    val g = tempColor[k * 3 + 1]
                    val b = tempColor[k * 3 + 2]
                    val dr = r - meanR
                    val dg = g - meanG
                    val db = b - meanB
                    var data = (dr * dr + dg * dg + db * db).toDouble()
                    val intr = frames[tempCover[k]].intrinsics
                    val across = 1.0 - abs(tempCol[k] - intr.centerXPx) / intr.centerXPx
                    val down = 1.0 - abs(tempRow[k] - intr.centerYPx) / intr.centerYPx
                    val borderCost = if (across <= 0.0 || down <= 0.0) {
                        BORDER_PENALTY
                    } else {
                        BORDER_PENALTY * (1.0 - across * down)
                    }
                    data += borderCost

                    packedLabels[packedSize] = tempCover[k]
                    packedData[packedSize] = data.toFloat()
                    val colorBase = packedSize * 3
                    packedColors[colorBase] = r
                    packedColors[colorBase + 1] = g
                    packedColors[colorBase + 2] = b
                    packedSize++
                }
            }
        }

        if (packedSize == 0) {
            return SeamWeights(
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                scale = scale,
                labelMap = IntArray(nodeCount) { -1 },
                loserLabel = IntArray(nodeCount) { -1 },
                loserWeight = FloatArray(nodeCount),
            )
        }

        val grid = PackedGrid(
            width = gridWidth,
            height = gridHeight,
            labelCount = frames.size,
            nodeLabelCount = nodeLabelCount,
            nodeLabels = packedLabels,
            nodeData = packedData,
            nodeColors = packedColors,
            nodeMean = nodeMean,
        )

        var expansionsDone = 0
        val totalExpansions = max(1, frames.size * MAX_SEAM_PASSES)
        val labels = SeamSolver.solve(
            grid = grid,
            smoothLambda = SMOOTH_LAMBDA,
            maxPasses = MAX_SEAM_PASSES,
            onExpansion = {
                expansionsDone++
                onProgress(0.6f + 0.35f * (expansionsDone.toFloat() / totalExpansions).coerceIn(0f, 1f))
            },
            wrapLongitude = longitudeSpanDegrees >= 360f,
        )

        val halfWidth = max(1, FEATHER_FULL_RES_PIXELS / scale)
        val (loserLabel, loserWeight) = SeamFeather.derive(
            labelMap = labels,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            halfWidth = halfWidth,
            wrapLongitude = longitudeSpanDegrees >= 360f,
        )
        onProgress(1.0f)
        return SeamWeights(gridWidth, gridHeight, scale, labels, loserLabel, loserWeight)
    }

    /**
     * Decodes a captured JPEG frame into its exact upright portrait `Bitmap`.
     *
     * Key invariant (matching `360-photo-app`'s `PhotoSphereStitcher.readFrame` + `applyExifRotation`):
     * - First apply any explicit EXIF orientation (`ROTATE_90`, `ROTATE_180`, `ROTATE_270`, flips).
     * - Because Dorja's scanner is locked to portrait (`SCREEN_ORIENTATION_PORTRAIT`) with a 4:3
     *   sensor aspect ratio, an upright frame MUST be taller than wide (`height > width`).
     * - If CameraX already software-rotated the JPEG pixels to portrait (`height > width`) and
     *   wrote `ORIENTATION_UNDEFINED` or `ORIENTATION_NORMAL`, we MUST NOT rotate it again!
     * - Conversely, if the OEM camera HAL wrote raw landscape sensor pixels (`width > height`)
     *   without rotating them, we rotate by `fallbackRotationDegrees` (`90°`) so the frame
     *   always lands upright on the sphere.
     */
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

        // Check dimensions after EXIF rotation: if the image is still landscape (width > height),
        // then the raw sensor buffer was not rotated to portrait by either CameraX or EXIF, so
        // apply fallbackRotationDegrees (typically 90° clockwise) to make it upright portrait.
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

    /**
     * Checks that the two field-of-view angles describe the axes of a decoded
     * [widthPx]×[heightPx] frame, swapping them when they evidently do not
     * (from `360-photo-app`'s `PhotoSphereStitcher.correctFovOrientation`).
     */
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

    /**
     * Applies a fast 3×3 masked unsharp mask (`out = src + amount * (src - blur)`) to all
     * captured pixels (`alpha == 0xFF`) so room edges, furniture, and text remain tack-sharp.
     */
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

    /**
     * Smoothly fills any uncaptured spherical pole caps (zenith / nadir) or uncaptured gaps
     * by propagating and horizontally smoothing the nearest captured boundary pixels toward the
     * zenith/nadir pole average color. Ensures there are NEVER black voids when looking up or down!
     */
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

        // 1. Fill any horizontal gaps inside partially-covered rows by nearest-column interpolation
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

        // 3. Smoothly synthesize the Zenith Cap (rows 0 until firstCoveredRow) from firstCoveredRow
        if (firstCoveredRow > 0) {
            val topEdgeOff = firstCoveredRow * width
            var avgR = 0.0
            var avgG = 0.0
            var avgB = 0.0
            for (c in 0 until width) {
                val p = pixels[topEdgeOff + c]
                avgR += (p ushr 16) and 0xFF
                avgG += (p ushr 8) and 0xFF
                avgB += p and 0xFF
            }
            avgR /= width
            avgG /= width
            avgB /= width

            for (row in 0 until firstCoveredRow) {
                // t = 0 at the zenith pole (row 0), t = 1 right at firstCoveredRow
                val t = row.toDouble() / firstCoveredRow.toDouble()
                val smoothT = t * t * (3.0 - 2.0 * t)
                val rowOff = row * width
                // Wide circular box-blur window that widens toward the pole
                val blurHalf = ((1.0 - smoothT) * (width / 12.0)).roundToInt().coerceIn(2, width / 4)
                for (c in 0 until width) {
                    val pA = pixels[topEdgeOff + ((c - blurHalf + width) % width)]
                    val pB = pixels[topEdgeOff + c]
                    val pC = pixels[topEdgeOff + ((c + blurHalf) % width)]
                    val edgeR = (((pA ushr 16) and 0xFF) + ((pB ushr 16) and 0xFF) * 2 + ((pC ushr 16) and 0xFF)) * 0.25
                    val edgeG = (((pA ushr 8) and 0xFF) + ((pB ushr 8) and 0xFF) * 2 + ((pC ushr 8) and 0xFF)) * 0.25
                    val edgeB = ((pA and 0xFF) + (pB and 0xFF) * 2 + (pC and 0xFF)) * 0.25

                    val r = (avgR * (1.0 - smoothT) + edgeR * smoothT).roundToInt().coerceIn(0, 255)
                    val g = (avgG * (1.0 - smoothT) + edgeG * smoothT).roundToInt().coerceIn(0, 255)
                    val b = (avgB * (1.0 - smoothT) + edgeB * smoothT).roundToInt().coerceIn(0, 255)
                    pixels[rowOff + c] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        // 4. Smoothly synthesize the Nadir Cap (rows lastCoveredRow + 1 until height) from lastCoveredRow
        if (lastCoveredRow < height - 1) {
            val botEdgeOff = lastCoveredRow * width
            var avgR = 0.0
            var avgG = 0.0
            var avgB = 0.0
            for (c in 0 until width) {
                val p = pixels[botEdgeOff + c]
                avgR += (p ushr 16) and 0xFF
                avgG += (p ushr 8) and 0xFF
                avgB += p and 0xFF
            }
            avgR /= width
            avgG /= width
            avgB /= width

            val span = (height - 1 - lastCoveredRow).coerceAtLeast(1)
            for (row in (lastCoveredRow + 1) until height) {
                // t = 1 right at lastCoveredRow, t = 0 at the nadir pole (height - 1)
                val t = 1.0 - (row - lastCoveredRow).toDouble() / span.toDouble()
                val smoothT = t * t * (3.0 - 2.0 * t)
                val rowOff = row * width
                val blurHalf = ((1.0 - smoothT) * (width / 12.0)).roundToInt().coerceIn(2, width / 4)
                for (c in 0 until width) {
                    val pA = pixels[botEdgeOff + ((c - blurHalf + width) % width)]
                    val pB = pixels[botEdgeOff + c]
                    val pC = pixels[botEdgeOff + ((c + blurHalf) % width)]
                    val edgeR = (((pA ushr 16) and 0xFF) + ((pB ushr 16) and 0xFF) * 2 + ((pC ushr 16) and 0xFF)) * 0.25
                    val edgeG = (((pA ushr 8) and 0xFF) + ((pB ushr 8) and 0xFF) * 2 + ((pC ushr 8) and 0xFF)) * 0.25
                    val edgeB = ((pA and 0xFF) + (pB and 0xFF) * 2 + (pC and 0xFF)) * 0.25

                    val r = (avgR * (1.0 - smoothT) + edgeR * smoothT).roundToInt().coerceIn(0, 255)
                    val g = (avgG * (1.0 - smoothT) + edgeG * smoothT).roundToInt().coerceIn(0, 255)
                    val b = (avgB * (1.0 - smoothT) + edgeB * smoothT).roundToInt().coerceIn(0, 255)
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
