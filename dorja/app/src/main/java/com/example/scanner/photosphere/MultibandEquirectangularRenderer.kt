package com.example.scanner.photosphere

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure-Kotlin implementation of `360-photo-app`'s `MultibandBlender` + `EquirectangularRenderer`
 * Burt–Adelson Laplacian pyramid multi-band spherical renderer.
 *
 * Why multi-band pyramid blending eliminates all blockiness / stair-stepping / ghosting:
 * - Each frame is projected through the pinhole lens model (`FrameIntrinsics`) with radial
 *   distortion and handheld body-swivel parallax (`pivotRatio`) onto a pyramid of equirectangular
 *   levels (`0 .. L-1`, where level `l` has dimensions `(W shr l) × (H shr l)`).
 * - At each pyramid level `l`, frames are weighted by their continuous center-peaked feather mask
 *   `featherWeight(idealCol, idealRow, cx, cy) = ((1 - |dx|/cx) * (1 - |dy|/cy))^3`.
 * - Reconstruction proceeds from coarse (`L-1`) to fine (`0`):
 *   ```
 *   acc_L = color_L / (weight_L + ε)
 *   acc_l = EXPAND(acc_{l+1}) + (color_l - EXPAND(color_{l+1}) * r_l) / (weight_l + ε)
 *   where r_l = weight_l / (EXPAND(weight_{l+1}) + ε)
 *   ```
 * - High-frequency details (text on a calendar, clock hands, furniture edges) live in the finest
 *   Laplacian band (`l = 0`) and transition sharply right at the center-dominance midpoint, while
 *   low-frequency wall illumination and exposure differences live in the coarser bands (`l = 2..4`)
 *   and blend smoothly across the entire overlap — zero 11×11 pixelation blocks, zero stair-step
 *   jaggies, and zero double-exposure ghosting!
 */
internal object MultibandEquirectangularRenderer {

    private const val MAX_LEVELS = 5
    private const val WEIGHT_EPSILON = 1e-6f

    class FrameInput(
        val pixels: IntArray,
        val width: Int,
        val height: Int,
        val basis: CameraBasis,
        val intrinsics: FrameIntrinsics,
        val footprint: CanvasFootprint,
        val gain: Float,
    )

    private class PyramidLevel(
        val width: Int,
        val height: Int,
        val colorR: FloatArray,
        val colorG: FloatArray,
        val colorB: FloatArray,
        val weight: FloatArray,
    )

    private class ReconstructedRGB(
        val width: Int,
        val height: Int,
        val r: FloatArray,
        val g: FloatArray,
        val b: FloatArray,
    )

    fun levelCountFor(canvasHeight: Int): Int {
        if (canvasHeight <= 0) return 1
        var h = canvasHeight
        var levels = 1
        while (h > 16 && levels < MAX_LEVELS) {
            h /= 2
            levels++
        }
        return levels
    }

    fun levelSize(width: Int, height: Int, level: Int): Pair<Int, Int> {
        val shift = level.coerceAtLeast(0)
        return (width shr shift).coerceAtLeast(1) to (height shr shift).coerceAtLeast(1)
    }

    fun renderFullSphere(
        frames: List<FrameInput>,
        canvasWidth: Int,
        canvasHeight: Int,
        pivotRatio: Double,
        longitudeSpanDegrees: Float = 360f,
        centerLongitudeDegrees: Float = 0f,
        latitudeSpanDegrees: Float = 180f,
        centerLatitudeDegrees: Float = 0f,
        outPixels: IntArray,
        rowHasCoverage: BooleanArray,
        onProgress: (Float) -> Unit = {},
        checkCancelled: () -> Unit = {},
    ) {
        val levelCount = levelCountFor(canvasHeight)

        // Precompute box-downsampled mipmaps for each frame so coarser pyramid levels sample
        // pre-filtered anti-aliased source pixels (just like EquirectangularRenderer's INTER_AREA resize)
        val framePyramids = Array(frames.size) { fi ->
            buildFrameMipmaps(frames[fi], levelCount)
        }

        // 1. Accumulate coarse levels (levelCount - 1 downTo 1)
        val coarseLevels = arrayOfNulls<PyramidLevel>(levelCount)
        for (level in 1 until levelCount) {
            checkCancelled()
            val (lw, lh) = levelSize(canvasWidth, canvasHeight, level)
            val pyr = PyramidLevel(
                width = lw,
                height = lh,
                colorR = FloatArray(lw * lh),
                colorG = FloatArray(lw * lh),
                colorB = FloatArray(lw * lh),
                weight = FloatArray(lw * lh),
            )
            accumulateLevelBand(
                frames = frames,
                frameMipmaps = framePyramids,
                level = level,
                levelWidth = lw,
                levelHeight = lh,
                bandTop = 0,
                bandRows = lh,
                colorR = pyr.colorR,
                colorG = pyr.colorG,
                colorB = pyr.colorB,
                weight = pyr.weight,
                pivotRatio = pivotRatio,
                longitudeSpanDegrees = longitudeSpanDegrees,
                centerLongitudeDegrees = centerLongitudeDegrees,
                latitudeSpanDegrees = latitudeSpanDegrees,
                centerLatitudeDegrees = centerLatitudeDegrees,
            )
            coarseLevels[level] = pyr
            onProgress(0.12f * (level.toFloat() / levelCount))
        }

        // 2. Reconstruct coarse levels from top (levelCount - 1) down to level 1
        var coarseAcc: ReconstructedRGB? = null
        if (levelCount >= 2) {
            val top = coarseLevels[levelCount - 1]!!
            coarseAcc = normalizeLevel(top)
            for (level in (levelCount - 2) downTo 1) {
                checkCancelled()
                val fine = coarseLevels[level]!!
                val coarser = coarseLevels[level + 1]!!
                coarseAcc = reconstructFullLevel(
                    fine = fine,
                    coarser = coarser,
                    coarseAcc = coarseAcc!!,
                    wrapLongitude = longitudeSpanDegrees >= 360f,
                )
                // Free level + 1 arrays now that level is reconstructed
                coarseLevels[level + 1] = null
            }
        }

        // 3. Reconstruct level 0 (full resolution) in memory-friendly horizontal bands of 128 rows
        val bandHeight = 128
        val bandCount = (canvasHeight + bandHeight - 1) / bandHeight
        val level1 = if (levelCount >= 2) coarseLevels[1] else null
        val level1Acc = coarseAcc

        val bandColorR = FloatArray(canvasWidth * bandHeight)
        val bandColorG = FloatArray(canvasWidth * bandHeight)
        val bandColorB = FloatArray(canvasWidth * bandHeight)
        val bandWeight = FloatArray(canvasWidth * bandHeight)

        for (bandIdx in 0 until bandCount) {
            checkCancelled()
            val bandTop = bandIdx * bandHeight
            val rows = min(bandHeight, canvasHeight - bandTop)
            val count = canvasWidth * rows

            bandColorR.fill(0f, 0, count)
            bandColorG.fill(0f, 0, count)
            bandColorB.fill(0f, 0, count)
            bandWeight.fill(0f, 0, count)

            accumulateLevelBand(
                frames = frames,
                frameMipmaps = framePyramids,
                level = 0,
                levelWidth = canvasWidth,
                levelHeight = canvasHeight,
                bandTop = bandTop,
                bandRows = rows,
                colorR = bandColorR,
                colorG = bandColorG,
                colorB = bandColorB,
                weight = bandWeight,
                pivotRatio = pivotRatio,
                longitudeSpanDegrees = longitudeSpanDegrees,
                centerLongitudeDegrees = centerLongitudeDegrees,
                latitudeSpanDegrees = latitudeSpanDegrees,
                centerLatitudeDegrees = centerLatitudeDegrees,
            )

            if (level1 != null && level1Acc != null) {
                reconstructFineBandToPixels(
                    bandTop = bandTop,
                    bandRows = rows,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    bandColorR = bandColorR,
                    bandColorG = bandColorG,
                    bandColorB = bandColorB,
                    bandWeight = bandWeight,
                    coarser = level1,
                    coarseAcc = level1Acc,
                    wrapLongitude = longitudeSpanDegrees >= 360f,
                    outPixels = outPixels,
                    rowHasCoverage = rowHasCoverage,
                )
            } else {
                for (r in 0 until rows) {
                    var anyInRow = false
                    val rowOff = r * canvasWidth
                    val dstOff = (bandTop + r) * canvasWidth
                    for (c in 0 until canvasWidth) {
                        val idx = rowOff + c
                        val w = bandWeight[idx]
                        if (w > 1e-5f) {
                            val inv = 1f / (w + WEIGHT_EPSILON)
                            val ir = (bandColorR[idx] * inv).roundToInt().coerceIn(0, 255)
                            val ig = (bandColorG[idx] * inv).roundToInt().coerceIn(0, 255)
                            val ib = (bandColorB[idx] * inv).roundToInt().coerceIn(0, 255)
                            outPixels[dstOff + c] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
                            anyInRow = true
                        } else {
                            outPixels[dstOff + c] = 0
                        }
                    }
                    rowHasCoverage[bandTop + r] = anyInRow
                }
            }

            onProgress(0.12f + 0.88f * ((bandIdx + 1).toFloat() / bandCount))
        }
    }

    private class FrameMipmap(
        val pixels: IntArray,
        val width: Int,
        val height: Int,
        val scaleDivisor: Double,
    )

    private fun buildFrameMipmaps(frame: FrameInput, levelCount: Int): Array<FrameMipmap> {
        val list = ArrayList<FrameMipmap>(levelCount)
        list.add(FrameMipmap(frame.pixels, frame.width, frame.height, 1.0))
        var prevPixels = frame.pixels
        var prevW = frame.width
        var prevH = frame.height
        var scale = 1.0
        for (level in 1 until levelCount) {
            scale *= 2.0
            val nextW = (prevW / 2).coerceAtLeast(1)
            val nextH = (prevH / 2).coerceAtLeast(1)
            val nextPixels = IntArray(nextW * nextH)
            for (y in 0 until nextH) {
                val y0 = (y * 2).coerceAtMost(prevH - 1)
                val y1 = (y * 2 + 1).coerceAtMost(prevH - 1)
                val row0 = y0 * prevW
                val row1 = y1 * prevW
                val dstRow = y * nextW
                for (x in 0 until nextW) {
                    val x0 = (x * 2).coerceAtMost(prevW - 1)
                    val x1 = (x * 2 + 1).coerceAtMost(prevW - 1)
                    val p00 = prevPixels[row0 + x0]
                    val p10 = prevPixels[row0 + x1]
                    val p01 = prevPixels[row1 + x0]
                    val p11 = prevPixels[row1 + x1]
                    val r = (((p00 ushr 16) and 0xFF) + ((p10 ushr 16) and 0xFF) +
                        ((p01 ushr 16) and 0xFF) + ((p11 ushr 16) and 0xFF) + 2) shr 2
                    val g = (((p00 ushr 8) and 0xFF) + ((p10 ushr 8) and 0xFF) +
                        ((p01 ushr 8) and 0xFF) + ((p11 ushr 8) and 0xFF) + 2) shr 2
                    val b = ((p00 and 0xFF) + (p10 and 0xFF) +
                        (p01 and 0xFF) + (p11 and 0xFF) + 2) shr 2
                    nextPixels[dstRow + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            list.add(FrameMipmap(nextPixels, nextW, nextH, scale))
            prevPixels = nextPixels
            prevW = nextW
            prevH = nextH
        }
        return list.toTypedArray()
    }

    private fun accumulateLevelBand(
        frames: List<FrameInput>,
        frameMipmaps: Array<Array<FrameMipmap>>,
        level: Int,
        levelWidth: Int,
        levelHeight: Int,
        bandTop: Int,
        bandRows: Int,
        colorR: FloatArray,
        colorG: FloatArray,
        colorB: FloatArray,
        weight: FloatArray,
        pivotRatio: Double,
        longitudeSpanDegrees: Float,
        centerLongitudeDegrees: Float,
        latitudeSpanDegrees: Float,
        centerLatitudeDegrees: Float,
    ) {
        val scale = 1 shl level
        val sinLon = DoubleArray(levelWidth)
        val cosLon = DoubleArray(levelWidth)
        for (c in 0 until levelWidth) {
            val lonRad = Math.toRadians(
                Equirectangular.longitudeDegrees(
                    c, levelWidth, longitudeSpanDegrees, centerLongitudeDegrees,
                )
            )
            sinLon[c] = sin(lonRad)
            cosLon[c] = cos(lonRad)
        }

        for (fi in frames.indices) {
            val frame = frames[fi]
            val fp = scaleFootprint(frame.footprint, scale)
            if (fp.isEmpty) continue

            val rowStart = max(bandTop, fp.startRow)
            val rowEnd = min(bandTop + bandRows, fp.startRow + fp.rowSpan)
            if (rowEnd <= rowStart) continue

            val mip = frameMipmaps[fi][level]
            val basis = frame.basis
            val intr = frame.intrinsics
            val cx = intr.centerXPx
            val cy = intr.centerYPx
            val maxCol = intr.widthPx - 1.001
            val maxRow = intr.heightPx - 1.001
            val radial = intr.radial
            val gain = frame.gain
            val divisor = mip.scaleDivisor

            forEachColumnRun(fp, levelWidth, longitudeSpanDegrees) { colStart, colSpan ->
                for (row in rowStart until rowEnd) {
                    val latRad = Math.toRadians(
                        Equirectangular.latitudeDegrees(
                            row, levelHeight, latitudeSpanDegrees, centerLatitudeDegrees,
                        )
                    )
                    val cosLat = cos(latRad)
                    val sinLat = sin(latRad)
                    val bandRowOff = (row - bandTop) * levelWidth

                    for (colOffset in 0 until colSpan) {
                        val col = colStart + colOffset
                        val wx = sinLon[col] * cosLat
                        val wy = cosLon[col] * cosLat
                        val wz = sinLat

                        val depth = basis.depthOf(wx, wy, wz) - pivotRatio
                        if (depth <= MIN_DEPTH) continue

                        val idealCol = cx + intr.focalXPx * basis.lateralOf(wx, wy, wz) / depth
                        val idealRow = cy - intr.focalYPx * basis.verticalOf(wx, wy, wz) / depth

                        val fw = featherWeight(idealCol, idealRow, cx, cy).toFloat()
                        if (fw <= 0f) continue

                        val srcCol: Double
                        val srcRow: Double
                        if (radial != null) {
                            val dx = idealCol - cx
                            val dy = idealRow - cy
                            val factor = radialFactor(radial, dx * dx + dy * dy)
                            srcCol = cx + dx * factor
                            srcRow = cy + dy * factor
                        } else {
                            srcCol = idealCol
                            srcRow = idealRow
                        }

                        if (srcCol < 0.0 || srcCol >= maxCol || srcRow < 0.0 || srcRow >= maxRow) {
                            continue
                        }

                        val mipX = srcCol / divisor
                        val mipY = srcRow / divisor
                        val x0 = kotlin.math.floor(mipX).toInt().coerceIn(0, mip.width - 1)
                        val y0 = kotlin.math.floor(mipY).toInt().coerceIn(0, mip.height - 1)
                        val x1 = (x0 + 1).coerceAtMost(mip.width - 1)
                        val y1 = (y0 + 1).coerceAtMost(mip.height - 1)
                        val fx = (mipX - x0).toFloat()
                        val fy = (mipY - y0).toFloat()
                        val omFx = 1f - fx
                        val omFy = 1f - fy

                        val p00 = mip.pixels[y0 * mip.width + x0]
                        val p10 = mip.pixels[y0 * mip.width + x1]
                        val p01 = mip.pixels[y1 * mip.width + x0]
                        val p11 = mip.pixels[y1 * mip.width + x1]

                        val w00 = omFx * omFy
                        val w10 = fx * omFy
                        val w01 = omFx * fy
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

                        val dstIdx = bandRowOff + col
                        colorR[dstIdx] += r * fw
                        colorG[dstIdx] += g * fw
                        colorB[dstIdx] += b * fw
                        weight[dstIdx] += fw
                    }
                }
            }
        }
    }

    private fun normalizeLevel(level: PyramidLevel): ReconstructedRGB {
        val size = level.width * level.height
        val r = FloatArray(size)
        val g = FloatArray(size)
        val b = FloatArray(size)
        for (i in 0 until size) {
            val inv = 1f / (level.weight[i] + WEIGHT_EPSILON)
            r[i] = level.colorR[i] * inv
            g[i] = level.colorG[i] * inv
            b[i] = level.colorB[i] * inv
        }
        return ReconstructedRGB(level.width, level.height, r, g, b)
    }

    private fun reconstructFullLevel(
        fine: PyramidLevel,
        coarser: PyramidLevel,
        coarseAcc: ReconstructedRGB,
        wrapLongitude: Boolean,
    ): ReconstructedRGB {
        val fw = fine.width
        val fh = fine.height
        val outR = FloatArray(fw * fh)
        val outG = FloatArray(fw * fh)
        val outB = FloatArray(fw * fh)

        val cw = coarser.width
        val ch = coarser.height
        val sampleScratch = FloatArray(7)

        for (r in 0 until fh) {
            val cy = (r + 0.5f) * 0.5f - 0.5f
            val rowOff = r * fw
            for (c in 0 until fw) {
                val cx = (c + 0.5f) * 0.5f - 0.5f
                sampleCoarseBilinear(
                    coarser = coarser,
                    coarseAcc = coarseAcc,
                    cw = cw,
                    ch = ch,
                    cx = cx,
                    cy = cy,
                    wrapLongitude = wrapLongitude,
                    out = sampleScratch,
                )
                val accUpR = sampleScratch[0]
                val accUpG = sampleScratch[1]
                val accUpB = sampleScratch[2]
                val colUpR = sampleScratch[3]
                val colUpG = sampleScratch[4]
                val colUpB = sampleScratch[5]
                val weightUp = sampleScratch[6]

                val idx = rowOff + c
                val wFine = fine.weight[idx]
                val ratio = wFine / (weightUp + WEIGHT_EPSILON)
                val invWFine = 1f / (wFine + WEIGHT_EPSILON)

                val lapR = (fine.colorR[idx] - colUpR * ratio) * invWFine
                val lapG = (fine.colorG[idx] - colUpG * ratio) * invWFine
                val lapB = (fine.colorB[idx] - colUpB * ratio) * invWFine

                outR[idx] = accUpR + lapR
                outG[idx] = accUpG + lapG
                outB[idx] = accUpB + lapB
            }
        }
        return ReconstructedRGB(fw, fh, outR, outG, outB)
    }

    private fun reconstructFineBandToPixels(
        bandTop: Int,
        bandRows: Int,
        canvasWidth: Int,
        canvasHeight: Int,
        bandColorR: FloatArray,
        bandColorG: FloatArray,
        bandColorB: FloatArray,
        bandWeight: FloatArray,
        coarser: PyramidLevel,
        coarseAcc: ReconstructedRGB,
        wrapLongitude: Boolean,
        outPixels: IntArray,
        rowHasCoverage: BooleanArray,
    ) {
        val cw = coarser.width
        val ch = coarser.height
        val sampleScratch = FloatArray(7)

        for (localRow in 0 until bandRows) {
            val globalRow = bandTop + localRow
            val cy = (globalRow + 0.5f) * 0.5f - 0.5f
            val bandRowOff = localRow * canvasWidth
            val dstRowOff = globalRow * canvasWidth
            var anyCovered = false

            for (col in 0 until canvasWidth) {
                val idx = bandRowOff + col
                val wFine = bandWeight[idx]
                if (wFine <= 1e-6f) {
                    outPixels[dstRowOff + col] = 0
                    continue
                }

                val cx = (col + 0.5f) * 0.5f - 0.5f
                sampleCoarseBilinear(
                    coarser = coarser,
                    coarseAcc = coarseAcc,
                    cw = cw,
                    ch = ch,
                    cx = cx,
                    cy = cy,
                    wrapLongitude = wrapLongitude,
                    out = sampleScratch,
                )
                val accUpR = sampleScratch[0]
                val accUpG = sampleScratch[1]
                val accUpB = sampleScratch[2]
                val colUpR = sampleScratch[3]
                val colUpG = sampleScratch[4]
                val colUpB = sampleScratch[5]
                val weightUp = sampleScratch[6]

                val ratio = wFine / (weightUp + WEIGHT_EPSILON)
                val invWFine = 1f / (wFine + WEIGHT_EPSILON)

                val finalR = accUpR + (bandColorR[idx] - colUpR * ratio) * invWFine
                val finalG = accUpG + (bandColorG[idx] - colUpG * ratio) * invWFine
                val finalB = accUpB + (bandColorB[idx] - colUpB * ratio) * invWFine

                val ir = finalR.roundToInt().coerceIn(0, 255)
                val ig = finalG.roundToInt().coerceIn(0, 255)
                val ib = finalB.roundToInt().coerceIn(0, 255)
                outPixels[dstRowOff + col] = (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
                anyCovered = true
            }
            rowHasCoverage[globalRow] = anyCovered
        }
    }

    private fun sampleCoarseBilinear(
        coarser: PyramidLevel,
        coarseAcc: ReconstructedRGB,
        cw: Int,
        ch: Int,
        cx: Float,
        cy: Float,
        wrapLongitude: Boolean,
        out: FloatArray,
    ) {
        val y0 = kotlin.math.floor(cy.toDouble()).toInt()
        val fy = cy - y0
        val omFy = 1f - fy
        val r0 = y0.coerceIn(0, ch - 1) * cw
        val r1 = (y0 + 1).coerceIn(0, ch - 1) * cw

        val xFloor = kotlin.math.floor(cx.toDouble()).toInt()
        val fx = cx - xFloor
        val omFx = 1f - fx

        val c0: Int
        val c1: Int
        if (wrapLongitude) {
            c0 = ((xFloor % cw) + cw) % cw
            c1 = (((xFloor + 1) % cw) + cw) % cw
        } else {
            c0 = xFloor.coerceIn(0, cw - 1)
            c1 = (xFloor + 1).coerceIn(0, cw - 1)
        }

        val i00 = r0 + c0
        val i10 = r0 + c1
        val i01 = r1 + c0
        val i11 = r1 + c1

        val w00 = omFx * omFy
        val w10 = fx * omFy
        val w01 = omFx * fy
        val w11 = fx * fy

        out[0] = coarseAcc.r[i00] * w00 + coarseAcc.r[i10] * w10 + coarseAcc.r[i01] * w01 + coarseAcc.r[i11] * w11
        out[1] = coarseAcc.g[i00] * w00 + coarseAcc.g[i10] * w10 + coarseAcc.g[i01] * w01 + coarseAcc.g[i11] * w11
        out[2] = coarseAcc.b[i00] * w00 + coarseAcc.b[i10] * w10 + coarseAcc.b[i01] * w01 + coarseAcc.b[i11] * w11

        out[3] = coarser.colorR[i00] * w00 + coarser.colorR[i10] * w10 + coarser.colorR[i01] * w01 + coarser.colorR[i11] * w11
        out[4] = coarser.colorG[i00] * w00 + coarser.colorG[i10] * w10 + coarser.colorG[i01] * w01 + coarser.colorG[i11] * w11
        out[5] = coarser.colorB[i00] * w00 + coarser.colorB[i10] * w10 + coarser.colorB[i01] * w01 + coarser.colorB[i11] * w11

        out[6] = coarser.weight[i00] * w00 + coarser.weight[i10] * w10 + coarser.weight[i01] * w01 + coarser.weight[i11] * w11
    }

    private fun scaleFootprint(footprint: CanvasFootprint, scale: Int): CanvasFootprint {
        if (scale <= 1) return footprint
        val startColumn = Math.floorDiv(footprint.startColumn, scale) - 1
        val endColumn = Math.floorDiv(footprint.startColumn + footprint.columnSpan - 1, scale) + 2
        val startRow = Math.floorDiv(footprint.startRow, scale) - 1
        val endRow = Math.floorDiv(footprint.startRow + footprint.rowSpan - 1, scale) + 2
        return CanvasFootprint(
            startColumn = startColumn,
            columnSpan = endColumn - startColumn,
            startRow = startRow,
            rowSpan = endRow - startRow,
        )
    }

    private inline fun forEachColumnRun(
        footprint: CanvasFootprint,
        canvasWidth: Int,
        longitudeSpanDegrees: Float,
        block: (canvasColumn: Int, columnSpan: Int) -> Unit,
    ) {
        val span = min(footprint.columnSpan, canvasWidth)
        if (span <= 0) return
        if (longitudeSpanDegrees >= 360f) {
            val first = wrapColumn(footprint.startColumn, canvasWidth)
            val firstSpan = min(span, canvasWidth - first)
            block(first, firstSpan)
            val remainder = span - firstSpan
            if (remainder > 0) block(0, remainder)
        } else {
            val first = max(footprint.startColumn, 0)
            val last = min(footprint.startColumn + span, canvasWidth)
            if (last > first) block(first, last - first)
        }
    }
}
