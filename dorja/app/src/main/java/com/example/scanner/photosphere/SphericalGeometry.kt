package com.example.scanner.photosphere

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class CameraPose(
    val yawDegrees: Float,
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val matrix: DoubleArray? = null,
) {
    val elevationDegrees: Float get() = -pitchDegrees
}

class CameraBasis private constructor(
    val forwardX: Double,
    val forwardY: Double,
    val forwardZ: Double,
    val rightX: Double,
    val rightY: Double,
    val rightZ: Double,
    val upX: Double,
    val upY: Double,
    val upZ: Double,
) {
    fun depthOf(x: Double, y: Double, z: Double): Double =
        x * forwardX + y * forwardY + z * forwardZ

    fun lateralOf(x: Double, y: Double, z: Double): Double =
        x * rightX + y * rightY + z * rightZ

    fun verticalOf(x: Double, y: Double, z: Double): Double =
        x * upX + y * upY + z * upZ

    fun toWorld(cameraX: Double, cameraY: Double, cameraZ: Double): DoubleArray = doubleArrayOf(
        cameraX * rightX + cameraY * upX + cameraZ * forwardX,
        cameraX * rightY + cameraY * upY + cameraZ * forwardY,
        cameraX * rightZ + cameraY * upZ + cameraZ * forwardZ,
    )

    fun toRotationMatrix(): DoubleArray = doubleArrayOf(
        rightX, -upX, forwardX,
        rightY, -upY, forwardY,
        rightZ, -upZ, forwardZ,
    )

    fun toPose(): CameraPose {
        val yaw = Math.toDegrees(atan2(forwardX, forwardY)).toFloat()
        val elevation = Math.toDegrees(asin(forwardZ.coerceIn(-1.0, 1.0))).toFloat()
        val yawR = Math.toRadians(yaw.toDouble())
        val elevationR = Math.toRadians(elevation.toDouble())
        val sinYaw = sin(yawR)
        val cosYaw = cos(yawR)
        val sinElevation = sin(elevationR)
        val up0X = -sinYaw * sinElevation
        val up0Y = -cosYaw * sinElevation
        val up0Z = cos(elevationR)
        val roll = Math.toDegrees(
            atan2(
                -(rightX * up0X + rightY * up0Y + rightZ * up0Z),
                upX * up0X + upY * up0Y + upZ * up0Z,
            )
        ).toFloat()
        return CameraPose(yawDegrees = yaw, pitchDegrees = -elevation, rollDegrees = roll)
    }

    companion object {
        fun of(pose: CameraPose): CameraBasis {
            pose.matrix?.let { return fromRotationMatrix(it) }

            val yaw = Math.toRadians(pose.yawDegrees.toDouble())
            val elevation = Math.toRadians(pose.elevationDegrees.toDouble())
            val roll = Math.toRadians(pose.rollDegrees.toDouble())

            val sinYaw = sin(yaw)
            val cosYaw = cos(yaw)
            val cosElevation = cos(elevation)

            val forwardX = sinYaw * cosElevation
            val forwardY = cosYaw * cosElevation
            val forwardZ = sin(elevation)

            val right0X = cosYaw
            val right0Y = -sinYaw
            val up0X = right0Y * forwardZ
            val up0Y = -right0X * forwardZ
            val up0Z = right0X * forwardY - right0Y * forwardX

            val cosRoll = cos(roll)
            val sinRoll = sin(roll)
            return CameraBasis(
                forwardX = forwardX,
                forwardY = forwardY,
                forwardZ = forwardZ,
                rightX = right0X * cosRoll - up0X * sinRoll,
                rightY = right0Y * cosRoll - up0Y * sinRoll,
                rightZ = -up0Z * sinRoll,
                upX = up0X * cosRoll + right0X * sinRoll,
                upY = up0Y * cosRoll + right0Y * sinRoll,
                upZ = up0Z * cosRoll,
            )
        }

        fun fromRotationMatrix(matrix: DoubleArray): CameraBasis = CameraBasis(
            rightX = matrix[0],
            rightY = matrix[3],
            rightZ = matrix[6],
            upX = -matrix[1],
            upY = -matrix[4],
            upZ = -matrix[7],
            forwardX = matrix[2],
            forwardY = matrix[5],
            forwardZ = matrix[8],
        )
    }
}

data class FrameIntrinsics(
    val widthPx: Int,
    val heightPx: Int,
    val focalXPx: Double,
    val focalYPx: Double,
    val radial: DoubleArray? = null,
) {
    init {
        require(widthPx > 0 && heightPx > 0) { "frame must have positive extent" }
        require(focalXPx > 0.0 && focalYPx > 0.0) { "focal length must be positive" }
    }

    val centerXPx: Double get() = widthPx / 2.0
    val centerYPx: Double get() = heightPx / 2.0
    val focalPx: Double get() = sqrt(focalXPx * focalYPx)

    fun scaledBy(focalScale: Double): FrameIntrinsics {
        if (focalScale <= 0.0 || abs(focalScale - 1.0) < 1e-6) return this
        val scaledRadial = radial?.let { coefficients ->
            var divisor = focalScale * focalScale
            val out = DoubleArray(coefficients.size)
            for (index in coefficients.indices) {
                out[index] = coefficients[index] / divisor
                divisor *= focalScale * focalScale
            }
            out
        }
        return copy(
            focalXPx = focalXPx * focalScale,
            focalYPx = focalYPx * focalScale,
            radial = scaledRadial,
        )
    }

    companion object {
        fun fromFieldOfView(
            widthPx: Int,
            heightPx: Int,
            horizontalFovDegrees: Float,
            verticalFovDegrees: Float,
            radial: DoubleArray? = null,
        ): FrameIntrinsics {
            require(horizontalFovDegrees in 1f..179f) { "horizontal fov: $horizontalFovDegrees" }
            require(verticalFovDegrees in 1f..179f) { "vertical fov: $verticalFovDegrees" }
            return FrameIntrinsics(
                widthPx = widthPx,
                heightPx = heightPx,
                focalXPx = (widthPx / 2.0) / tanHalf(horizontalFovDegrees),
                focalYPx = (heightPx / 2.0) / tanHalf(verticalFovDegrees),
                radial = radial,
            )
        }

        fun forLens(
            widthPx: Int,
            heightPx: Int,
            horizontalFovDegrees: Float,
            verticalFovDegrees: Float,
            distortion: RadialDistortion?,
        ): FrameIntrinsics {
            val pinhole = fromFieldOfView(
                widthPx = widthPx,
                heightPx = heightPx,
                horizontalFovDegrees = horizontalFovDegrees,
                verticalFovDegrees = verticalFovDegrees,
            )
            val radial = distortion?.pixelCoefficientsFor(pinhole.focalPx) ?: return pinhole
            return pinhole.copy(radial = radial)
        }

        private fun tanHalf(degrees: Float): Double = tan(Math.toRadians(degrees / 2.0))
    }
}

internal fun featherWeight(
    idealColumn: Double,
    idealRow: Double,
    centreX: Double,
    centreY: Double,
): Double {
    val across = 1.0 - abs(idealColumn - centreX) / centreX
    if (across <= 0.0) return 0.0
    val down = 1.0 - abs(idealRow - centreY) / centreY
    if (down <= 0.0) return 0.0
    val linear = across * down
    return linear * linear * linear
}

object Equirectangular {

    fun longitudeDegrees(
        col: Int,
        width: Int,
        longitudeSpanDegrees: Float = 360f,
        centerLongitudeDegrees: Float = 0f,
    ): Double = centerLongitudeDegrees +
        (col + 0.5) / width * longitudeSpanDegrees - longitudeSpanDegrees / 2

    fun latitudeDegrees(
        row: Int,
        height: Int,
        latitudeSpanDegrees: Float = 180f,
        centerLatitudeDegrees: Float = 0f,
    ): Double = centerLatitudeDegrees + latitudeSpanDegrees / 2 -
        (row + 0.5) / height * latitudeSpanDegrees

    fun columnFor(
        longitudeDegrees: Double,
        width: Int,
        longitudeSpanDegrees: Float = 360f,
        centerLongitudeDegrees: Float = 0f,
    ): Double =
        (longitudeDegrees - centerLongitudeDegrees + longitudeSpanDegrees / 2) /
            longitudeSpanDegrees * width - 0.5

    fun rowFor(
        latitudeDegrees: Double,
        height: Int,
        latitudeSpanDegrees: Float = 180f,
        centerLatitudeDegrees: Float = 0f,
    ): Double =
        (centerLatitudeDegrees + latitudeSpanDegrees / 2 - latitudeDegrees) /
            latitudeSpanDegrees * height - 0.5

    fun direction(longitudeDegrees: Double, latitudeDegrees: Double): DoubleArray {
        val lon = Math.toRadians(longitudeDegrees)
        val lat = Math.toRadians(latitudeDegrees)
        val cosLat = cos(lat)
        return doubleArrayOf(sin(lon) * cosLat, cos(lon) * cosLat, sin(lat))
    }

    fun longitudeOf(x: Double, y: Double): Double = Math.toDegrees(atan2(x, y))

    fun latitudeOf(z: Double): Double = Math.toDegrees(asin(z.coerceIn(-1.0, 1.0)))
}

data class CanvasFootprint(
    val startColumn: Int,
    val columnSpan: Int,
    val startRow: Int,
    val rowSpan: Int,
) {
    val isEmpty: Boolean get() = columnSpan <= 0 || rowSpan <= 0

    fun wrapsSeam(canvasWidth: Int): Boolean =
        startColumn < 0 || startColumn + columnSpan > canvasWidth
}

object FrameFootprint {

    private const val SAMPLES_PER_EDGE = 24

    fun compute(
        basis: CameraBasis,
        intrinsics: FrameIntrinsics,
        canvasWidth: Int,
        canvasHeight: Int,
        marginPx: Int = 2,
        pivotRatio: Double = 0.0,
        longitudeSpanDegrees: Float = 360f,
        centerLongitudeDegrees: Float = 0f,
        latitudeSpanDegrees: Float = 180f,
        centerLatitudeDegrees: Float = 0f,
    ): CanvasFootprint {
        val centreLongitude = Equirectangular.longitudeOf(basis.forwardX, basis.forwardY)

        var minLongitude = Double.MAX_VALUE
        var maxLongitude = -Double.MAX_VALUE
        var minLatitude = Double.MAX_VALUE
        var maxLatitude = -Double.MAX_VALUE

        forEachBorderDirection(intrinsics) { cameraX, cameraY ->
            val ray = basis.toWorld(cameraX, cameraY, 1.0)
            val length = length(ray)
            val world = pivotDirection(
                basis = basis,
                pivotRatio = pivotRatio,
                x = ray[0] / length,
                y = ray[1] / length,
                z = ray[2] / length,
            )
            val latitude = Equirectangular.latitudeOf(world[2])
            val longitude = unwrapNear(
                Equirectangular.longitudeOf(world[0], world[1]),
                centreLongitude,
            )

            minLongitude = min(minLongitude, longitude)
            maxLongitude = max(maxLongitude, longitude)
            minLatitude = min(minLatitude, latitude)
            maxLatitude = max(maxLatitude, latitude)
        }

        val coversNorthPole = containsDirection(basis, intrinsics, 0.0, 0.0, 1.0, pivotRatio)
        val coversSouthPole = containsDirection(basis, intrinsics, 0.0, 0.0, -1.0, pivotRatio)
        if (coversNorthPole) maxLatitude = 90.0
        if (coversSouthPole) minLatitude = -90.0

        val startRow = floorToInt(
            Equirectangular.rowFor(
                maxLatitude, canvasHeight, latitudeSpanDegrees, centerLatitudeDegrees,
            )
        ) - marginPx
        val endRow = ceilToInt(
            Equirectangular.rowFor(
                minLatitude, canvasHeight, latitudeSpanDegrees, centerLatitudeDegrees,
            )
        ) + marginPx
        val clampedStartRow = startRow.coerceIn(0, canvasHeight)
        val clampedEndRow = endRow.coerceIn(0, canvasHeight)

        if (coversNorthPole || coversSouthPole) {
            return CanvasFootprint(
                startColumn = 0,
                columnSpan = canvasWidth,
                startRow = clampedStartRow,
                rowSpan = clampedEndRow - clampedStartRow,
            )
        }

        val startColumn = floorToInt(
            Equirectangular.columnFor(
                minLongitude, canvasWidth, longitudeSpanDegrees, centerLongitudeDegrees,
            )
        ) - marginPx
        val endColumn = ceilToInt(
            Equirectangular.columnFor(
                maxLongitude, canvasWidth, longitudeSpanDegrees, centerLongitudeDegrees,
            )
        ) + marginPx
        val columnSpan = min(endColumn - startColumn, canvasWidth)

        return CanvasFootprint(
            startColumn = startColumn,
            columnSpan = columnSpan,
            startRow = clampedStartRow,
            rowSpan = clampedEndRow - clampedStartRow,
        )
    }

    private fun containsDirection(
        basis: CameraBasis,
        intrinsics: FrameIntrinsics,
        x: Double,
        y: Double,
        z: Double,
        pivotRatio: Double,
    ): Boolean = projectDirection(basis, intrinsics, x, y, z, pivotRatio) != null

    private fun forEachBorderDirection(
        intrinsics: FrameIntrinsics,
        block: (cameraX: Double, cameraY: Double) -> Unit,
    ) {
        val lastColumn = intrinsics.widthPx - 1.0
        val lastRow = intrinsics.heightPx - 1.0
        val radial = intrinsics.radial

        val border = { column: Double, row: Double ->
            val ideal = if (radial != null) {
                undistortPixel(column, row, intrinsics.centerXPx, intrinsics.centerYPx, radial)
            } else {
                doubleArrayOf(column, row)
            }
            block(cameraXOf(ideal[0], intrinsics), cameraYOf(ideal[1], intrinsics))
        }

        for (step in 0..SAMPLES_PER_EDGE) {
            val fraction = step.toDouble() / SAMPLES_PER_EDGE
            val alongWidth = fraction * lastColumn
            val alongHeight = fraction * lastRow
            border(alongWidth, 0.0)
            border(alongWidth, lastRow)
            border(0.0, alongHeight)
            border(lastColumn, alongHeight)
        }
    }

    private fun cameraXOf(column: Double, intrinsics: FrameIntrinsics): Double =
        (column - intrinsics.centerXPx) / intrinsics.focalXPx

    private fun cameraYOf(row: Double, intrinsics: FrameIntrinsics): Double =
        -(row - intrinsics.centerYPx) / intrinsics.focalYPx

    private fun length(vector: DoubleArray): Double =
        sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2])

    private fun floorToInt(value: Double): Int = kotlin.math.floor(value).toInt()

    private fun ceilToInt(value: Double): Int = kotlin.math.ceil(value).toInt()
}

internal const val MIN_DEPTH = 1e-6

internal fun pivotDirection(
    basis: CameraBasis,
    pivotRatio: Double,
    x: Double,
    y: Double,
    z: Double,
): DoubleArray {
    if (pivotRatio <= 0.0) return doubleArrayOf(x, y, z)

    val alongAxis = basis.depthOf(x, y, z)
    val half = pivotRatio * alongAxis
    val distance = -half + sqrt(max(half * half - pivotRatio * pivotRatio + 1.0, 0.0))

    val px = pivotRatio * basis.forwardX + distance * x
    val py = pivotRatio * basis.forwardY + distance * y
    val pz = pivotRatio * basis.forwardZ + distance * z
    val length = sqrt(px * px + py * py + pz * pz)
    if (length <= 0.0) return doubleArrayOf(x, y, z)
    return doubleArrayOf(px / length, py / length, pz / length)
}

internal fun projectDirection(
    basis: CameraBasis,
    intrinsics: FrameIntrinsics,
    x: Double,
    y: Double,
    z: Double,
    pivotRatio: Double = 0.0,
): DoubleArray? {
    val depth = basis.depthOf(x, y, z) - pivotRatio
    if (depth <= MIN_DEPTH) return null
    val centreX = intrinsics.centerXPx
    val centreY = intrinsics.centerYPx
    val idealColumn = centreX + intrinsics.focalXPx * basis.lateralOf(x, y, z) / depth
    val idealRow = centreY - intrinsics.focalYPx * basis.verticalOf(x, y, z) / depth

    val radial = intrinsics.radial
    val column: Double
    val row: Double
    if (radial != null) {
        val distorted = distortPixel(idealColumn, idealRow, centreX, centreY, radial)
        column = distorted[0]
        row = distorted[1]
    } else {
        column = idealColumn
        row = idealRow
    }
    if (column < 0.0 || column > intrinsics.widthPx - 1.0) return null
    if (row < 0.0 || row > intrinsics.heightPx - 1.0) return null
    return doubleArrayOf(column, row)
}

internal fun unwrapNear(degrees: Double, reference: Double): Double {
    var result = degrees
    while (result - reference > 180.0) result -= 360.0
    while (result - reference < -180.0) result += 360.0
    return result
}

internal fun wrapColumn(column: Int, width: Int): Int {
    val remainder = column % width
    return if (remainder < 0) remainder + width else remainder
}

internal fun angularOverlap(
    a: CameraBasis,
    b: CameraBasis,
    horizontalFovDegrees: Float,
    verticalFovDegrees: Float,
): Double? {
    val depth = a.depthOf(b.forwardX, b.forwardY, b.forwardZ)
    if (depth <= MIN_DEPTH) return null
    val horizontalSeparation = abs(a.lateralOf(b.forwardX, b.forwardY, b.forwardZ) / depth)
    val verticalSeparation = abs(a.verticalOf(b.forwardX, b.forwardY, b.forwardZ) / depth)
    if (horizontalSeparation >= overlapBound(horizontalFovDegrees)) return null
    if (verticalSeparation >= overlapBound(verticalFovDegrees)) return null
    return horizontalSeparation * horizontalSeparation + verticalSeparation * verticalSeparation
}

private fun overlapBound(degrees: Float): Double =
    if (degrees >= 90f) Double.MAX_VALUE else 2.0 * tan(Math.toRadians(degrees / 2.0))
