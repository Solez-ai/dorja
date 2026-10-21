package com.example.scanner.photosphere

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

private val ReticleRadius = 36.dp
private val ReticleStroke = 2.5.dp
private val ProgressStroke = 4.dp
private val ActiveDotRadius = 14.dp
private val PendingDotRadius = 7.dp
private val CapturedDotRadius = 6.dp
private val EdgeMargin = 40.dp
private val ArrowSize = 14.dp

private val ActiveColor = Color(0xFF4FC3F7)
private val AlignedColor = Color(0xFF81C784)
private val CapturedColor = Color(0xFF81C784)

@Composable
fun TargetOverlay(
    orientation: OrientationData,
    sessionState: SphereCaptureSessionState,
    modifier: Modifier = Modifier,
    fieldOfView: FieldOfView = FieldOfView.TYPICAL_PORTRAIT,
    showFrameFootprints: Boolean = false,
) {
    val pulse by rememberInfiniteTransition(label = "target-pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "active-dot-scale",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val focalLengthPx = SphereProjection.focalLengthPx(size.width, size.height, fieldOfView)
        if (focalLengthPx <= 0f) return@Canvas

        if (orientation.hasFix) {
            if (showFrameFootprints) {
                sessionState.capturedFrames.forEach { frame ->
                    drawFrameFootprint(
                        orientation = orientation,
                        frameOrientation = frame.orientation,
                        fieldOfView = fieldOfView,
                        center = center,
                        focalLengthPx = focalLengthPx,
                    )
                }
            }
            val active = sessionState.activeTarget
            sessionState.targets.forEach { target ->
                if (target.id == active?.id) return@forEach
                drawInactiveTarget(
                    orientation = orientation,
                    target = target,
                    isCaptured = sessionState.isCaptured(target),
                    center = center,
                    focalLengthPx = focalLengthPx,
                )
            }
            if (active != null) {
                drawActiveTarget(
                    orientation = orientation,
                    target = active,
                    alignment = sessionState.alignment,
                    center = center,
                    focalLengthPx = focalLengthPx,
                    pulseScale = pulse,
                )
            }
        }

        drawReticle(
            center = center,
            alignment = sessionState.alignment,
        )
    }
}

private const val FOOTPRINT_CORNER_OVERLAP = 0.82f

private fun DrawScope.drawFrameFootprint(
    orientation: OrientationData,
    frameOrientation: OrientationData,
    fieldOfView: FieldOfView,
    center: Offset,
    focalLengthPx: Float,
) {
    val centreTarget = SphereTarget(
        id = -1,
        ringIndex = 0,
        yawDegrees = frameOrientation.yawDegrees,
        elevationDegrees = -frameOrientation.pitchDegrees,
    )
    if (!SphereProjection.project(orientation, centreTarget).isInFront) return

    val halfH = Math.toRadians((fieldOfView.horizontalDegrees * FOOTPRINT_CORNER_OVERLAP) / 2.0)
    val halfV = Math.toRadians((fieldOfView.verticalDegrees * FOOTPRINT_CORNER_OVERLAP) / 2.0)
    val tanH = tan(halfH)
    val tanV = tan(halfV)
    val basis = CameraBasis.of(
        CameraPose(
            yawDegrees = frameOrientation.yawDegrees,
            pitchDegrees = frameOrientation.pitchDegrees,
            rollDegrees = frameOrientation.rollDegrees,
            matrix = frameOrientation.cameraBasis?.let { b -> DoubleArray(b.size) { b[it].toDouble() } },
        )
    )
    val corners = arrayOf(
        doubleArrayOf(-tanH, tanV),
        doubleArrayOf(tanH, tanV),
        doubleArrayOf(tanH, -tanV),
        doubleArrayOf(-tanH, -tanV),
    ).map { (cx, cy) ->
        val world = basis.toWorld(cx, cy, 1.0)
        val len = kotlin.math.sqrt(world[0] * world[0] + world[1] * world[1] + world[2] * world[2])
        val wx = world[0] / len
        val wy = world[1] / len
        val wz = world[2] / len
        val yaw = Math.toDegrees(atan2(wx, wy)).toFloat()
        val elevation = Math.toDegrees(kotlin.math.asin(wz.coerceIn(-1.0, 1.0))).toFloat()
        val projected = SphereProjection.project(
            orientation,
            SphereTarget(id = -1, ringIndex = 0, yawDegrees = yaw, elevationDegrees = elevation),
        )
        if (!projected.isInFront) return
        Offset(
            x = center.x + (projected.x / projected.z) * focalLengthPx,
            y = center.y - (projected.y / projected.z) * focalLengthPx,
        )
    }

    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
        close()
    }
    drawPath(path = path, color = CapturedColor.copy(alpha = 0.14f))
    drawPath(
        path = path,
        color = CapturedColor.copy(alpha = 0.45f),
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

private fun DrawScope.drawInactiveTarget(
    orientation: OrientationData,
    target: SphereTarget,
    isCaptured: Boolean,
    center: Offset,
    focalLengthPx: Float,
) {
    val projected = SphereProjection.project(orientation, target)
    if (!projected.isInFront) return

    val position = Offset(
        x = center.x + (projected.x / projected.z) * focalLengthPx,
        y = center.y - (projected.y / projected.z) * focalLengthPx,
    )
    if (!isInsideViewport(position)) return

    if (isCaptured) {
        drawCircle(
            color = CapturedColor.copy(alpha = 0.7f),
            radius = CapturedDotRadius.toPx(),
            center = position,
        )
    } else {
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = PendingDotRadius.toPx(),
            center = position,
        )
    }
}

private fun DrawScope.drawActiveTarget(
    orientation: OrientationData,
    target: SphereTarget,
    alignment: AlignmentState,
    center: Offset,
    focalLengthPx: Float,
    pulseScale: Float,
) {
    val projected = SphereProjection.project(orientation, target)
    val dotColor = if (alignment.isWithinThreshold) AlignedColor else ActiveColor
    val reticleRadiusPx = ReticleRadius.toPx()

    if (projected.isInFront) {
        val position = Offset(
            x = center.x + (projected.x / projected.z) * focalLengthPx,
            y = center.y - (projected.y / projected.z) * focalLengthPx,
        )
        if (isInsideViewport(position)) {
            val offsetFromCenter = position - center
            val distancePx = hypot(offsetFromCenter.x, offsetFromCenter.y)

            if (distancePx > reticleRadiusPx * 1.25f) {
                val unit = offsetFromCenter / distancePx
                drawLine(
                    color = dotColor.copy(alpha = 0.55f),
                    start = center + unit * reticleRadiusPx,
                    end = position - unit * ActiveDotRadius.toPx(),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                )
            }

            val radiusPx = ActiveDotRadius.toPx() *
                if (alignment.isWithinThreshold) 1f else pulseScale
            drawCircle(
                color = dotColor.copy(alpha = 0.25f),
                radius = radiusPx * 1.45f,
                center = position,
            )
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = position,
            )
            return
        }
    }

    drawEdgeIndicator(
        directionX = projected.x,
        directionY = -projected.y,
        center = center,
        color = dotColor,
    )
}

private fun DrawScope.drawEdgeIndicator(
    directionX: Float,
    directionY: Float,
    center: Offset,
    color: Color,
) {
    val norm = hypot(directionX, directionY)
    val ux = if (norm > 1e-4f) directionX / norm else 1f
    val uy = if (norm > 1e-4f) directionY / norm else 0f

    val orbitRadius = min(size.width, size.height) / 2f - EdgeMargin.toPx()
    if (orbitRadius <= 0f) return

    val tip = Offset(center.x + ux * orbitRadius, center.y + uy * orbitRadius)
    val arrowSizePx = ArrowSize.toPx()
    val angle = atan2(uy, ux)
    val wingAngle = 2.5f

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(
            tip.x + cos(angle + wingAngle) * arrowSizePx,
            tip.y + sin(angle + wingAngle) * arrowSizePx,
        )
        moveTo(tip.x, tip.y)
        lineTo(
            tip.x + cos(angle - wingAngle) * arrowSizePx,
            tip.y + sin(angle - wingAngle) * arrowSizePx,
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawCircle(
        color = color,
        radius = 4.dp.toPx(),
        center = Offset(tip.x - ux * (arrowSizePx * 1.2f), tip.y - uy * (arrowSizePx * 1.2f)),
    )
}

private fun DrawScope.drawReticle(
    center: Offset,
    alignment: AlignmentState,
) {
    val radiusPx = ReticleRadius.toPx()
    val ringColor = if (alignment.isWithinThreshold) {
        AlignedColor
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    drawCircle(
        color = ringColor,
        radius = radiusPx,
        center = center,
        style = Stroke(width = ReticleStroke.toPx()),
    )

    if (alignment.dwellProgress > 0f) {
        val diameter = radiusPx * 2f
        drawArc(
            color = AlignedColor,
            startAngle = -90f,
            sweepAngle = 360f * alignment.dwellProgress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
            size = Size(diameter, diameter),
            style = Stroke(width = ProgressStroke.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.isInsideViewport(point: Offset): Boolean =
    point.x in 0f..size.width && point.y in 0f..size.height
