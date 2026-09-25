package com.example.ui.transition

import android.app.ActivityInfo
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

/**
 * Global switchboards for the fold rotation transition.
 *
 * [suspended] lets screens that own the orientation themselves (the panorama
 * viewer forces landscape) opt out of sensor-driven folding while composed.
 */
object FoldRotation {
    var suspended by mutableStateOf(false)
}

enum class FoldOrientation { PORTRAIT, LANDSCAPE_CW, LANDSCAPE_CCW }

/** One in-flight transition: the portrait frame captured before the commit and the committed frame captured after. */
private class FoldFrame(
    val old: ImageBitmap,
    val new: ImageBitmap?,
)

/** Tunables for the fold feel. */
private object FoldSpec {
    const val TRIGGER_DEGREES = 45f        // tilt past this arms a transition
    const val RELEASE_DEGREES = 18f        // tilt back under this disarms
    const val MIN_GRAVITY = 4f             // below this the device is flat — no signal
    const val FILTER_ALPHA = 0.25f         // low-pass coefficient for the tilt angle
    const val DURATION_MS = 430            // fold animation length
    const val CAMERA = 32f                 // perspective depth for the folding half
    const val MAX_BLUR = 15f               // px blur at 90° fold
}

/**
 * If the landscape landing looks upside down on a given device, flip this —
 * sensor frame conventions differ per vendor.
 */
private const val ANGLE_POSITIVE_IS_CW = true

/**
 * Wraps the whole app and gives every orientation change a physical "book
 * fold" transition: the portrait UI folds around its vertical spine (with
 * progressive blur, dimming and crease shading) while the freshly laid-out
 * landscape UI is revealed underneath.
 *
 * The activity holds the window portrait-locked while this host is active so
 * the fold timing belongs to the sensor pipeline instead of the system's own
 * rotation schedule; the content is laid out at swapped dimensions and rotated
 * at the root, which is what the commit step flips.
 */
@Composable
fun FoldRotationHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val oldLayer = rememberGraphicsLayer()
    val newLayer = rememberGraphicsLayer()

    var orientation by remember { mutableStateOf(FoldOrientation.PORTRAIT) }
    var captureOld by remember { mutableStateOf(false) }
    var captureNew by remember { mutableStateOf(false) }
    var frame by remember { mutableStateOf<FoldFrame?>(null) }
    val progress = remember { Animatable(1f) }

    // ── Own the system orientation while the host is active ──
    // Only when a gravity sensor exists to drive the fold; otherwise the
    // system's own rotation stays in charge (the adoption effect below keeps
    // the layout in sync).
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        val original = activity?.requestedOrientation
        val hasSensor =
            (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                .getDefaultSensor(Sensor.TYPE_GRAVITY) != null
        if (hasSensor) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
        onDispose {
            if (hasSensor) {
                activity?.requestedOrientation = original
                    ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Screens that force orientation themselves (panorama) suspend the fold
    // engine; when the window orientation changes underneath us (their doing),
    // adopt it silently — no animation, no fight with the system.
    val windowOrientation = configuration.orientation
    LaunchedEffect(windowOrientation, FoldRotation.suspended) {
        if (frame == null) {
            orientation = if (windowOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (orientation == FoldOrientation.PORTRAIT) FoldOrientation.LANDSCAPE_CW else orientation
            } else {
                FoldOrientation.PORTRAIT
            }
        }
    }

    // ── Gravity sensor → filtered tilt → transition trigger ──
    DisposableEffect(lifecycleOwner) {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        var filtered = 0f
        var armed = false
        var confirmations = 0
        var running = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val g = event.values
                val planar = sqrt(g[0] * g[0] + g[1] * g[1])
                if (planar < FoldSpec.MIN_GRAVITY) return // device flat — unstable, ignore
                val angle =
                    Math.toDegrees(atan2(g[0].toDouble(), -g[1].toDouble())).toFloat()
                filtered += FoldSpec.FILTER_ALPHA * (angle - filtered)

                if (running || FoldRotation.suspended) return

                if (!armed) {
                    if (kotlin.math.abs(filtered) > FoldSpec.TRIGGER_DEGREES) {
                        confirmations++
                        // Two consecutive filtered samples past the trigger
                        // keeps sensor spikes from starting a fold.
                        if (confirmations >= 2) {
                            armed = true
                            confirmations = 0
                            val target =
                                if (ANGLE_POSITIVE_IS_CW == (filtered > 0f)) {
                                    FoldOrientation.LANDSCAPE_CW
                                } else {
                                    FoldOrientation.LANDSCAPE_CCW
                                }
                            if (target != orientation) {
                                running = true
                                scope.launch {
                                    foldTo(
                                        target = target,
                                        orientationSetter = { orientation = it },
                                        captureOldSetter = { captureOld = it },
                                        captureNewSetter = { captureNew = it },
                                        oldLayer = oldLayer,
                                        newLayer = newLayer,
                                        frameSetter = { frame = it },
                                        progress = progress,
                                    )
                                    running = false
                                    armed = false
                                }
                            } else {
                                armed = false
                            }
                        }
                    } else {
                        confirmations = 0
                    }
                } else if (kotlin.math.abs(filtered) < FoldSpec.RELEASE_DEGREES) {
                    armed = false
                }
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    if (sensor != null) {
                        sensorManager.registerListener(
                            listener,
                            sensor,
                            SensorManager.SENSOR_DELAY_GAME,
                        )
                    }
                Lifecycle.Event.ON_PAUSE -> sensorManager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Root: portrait-locked window, content laid out at swapped dims and rotated ──
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    // Frame capture for the fold textures happens here, before
                    // the regular draw, whenever the transition asks for it.
                    if (captureOld) {
                        captureOld = false
                        oldLayer.record { this@drawWithContent.drawContent() }
                    }
                    if (captureNew) {
                        captureNew = false
                        newLayer.record { this@drawWithContent.drawContent() }
                    }
                    drawContent()
                },
        ) {
            val windowLandscape =
                windowOrientation == Configuration.ORIENTATION_LANDSCAPE
            val rotated = !windowLandscape && orientation != FoldOrientation.PORTRAIT
            Layout(content = { content() }) { measurables, constraints ->
                val contentWidth = if (rotated) constraints.maxHeight else constraints.maxWidth
                val contentHeight = if (rotated) constraints.maxWidth else constraints.maxHeight
                val placeable = measurables.first().measure(
                    Constraints.fixed(contentWidth, contentHeight),
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(
                        x = (constraints.maxWidth - contentWidth) / 2,
                        y = (constraints.maxHeight - contentHeight) / 2,
                    ) {
                        if (rotated) {
                            rotationZ = if (orientation == FoldOrientation.LANDSCAPE_CW) 90f else -90f
                            transformOrigin = TransformOrigin.Center
                        }
                    }
                }
            }
        }

        // ── The fold overlay (window space, above the rotated content) ──
        frame?.let { f ->
            FoldOverlay(frame = f, progress = progress.value)
        }
    }
}

/**
 * Drives one transition: capture the portrait frame → commit the orientation
 * (content re-lays-out rotated) → capture the landscape frame → animate the
 * fold overlay from 0 to 1 → drop the overlay so the live UI takes over.
 */
private suspend fun foldTo(
    target: FoldOrientation,
    orientationSetter: (FoldOrientation) -> Unit,
    captureOldSetter: (Boolean) -> Unit,
    captureNewSetter: (Boolean) -> Unit,
    oldLayer: GraphicsLayer,
    newLayer: GraphicsLayer,
    frameSetter: (FoldFrame?) -> Unit,
    progress: Animatable<Float, *>,
) {
    // Two frame waits per capture: the first lets the invalidation reach the
    // draw phase (where record happens), the second guarantees it landed.
    captureOldSetter(true)
    withFrameNanos { }
    withFrameNanos { }
    val oldBitmap = oldLayer.toImageBitmap()

    orientationSetter(target)
    captureNewSetter(true)
    withFrameNanos { }
    withFrameNanos { }
    val newBitmap = try {
        newLayer.toImageBitmap()
    } catch (_: IllegalStateException) {
        null
    }

    frameSetter(FoldFrame(oldBitmap, newBitmap))
    progress.snapTo(0f)
    try {
        progress.animateTo(1f, tween(FoldSpec.DURATION_MS, easing = FastOutSlowInEasing))
    } finally {
        frameSetter(null)
    }
}

/**
 * The book-fold overlay. The old portrait frame is split at its vertical
 * spine: the left half holds and fades, the right half rotates toward the
 * viewer around the spine with progressive blur, dimming and edge shading,
 * while the committed landscape frame scales in underneath.
 */
@Composable
private fun FoldOverlay(frame: FoldFrame, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * sin(progress * Math.PI).toFloat())),
    ) {
        // Committed landscape frame scaling in beneath the fold.
        frame.new?.let { new ->
            val reveal = ((progress - 0.45f) / 0.5f).coerceIn(0f, 1f)
            Image(
                bitmap = new,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = reveal
                        scaleX = 0.965f + 0.035f * reveal
                        scaleY = scaleX
                        transformOrigin = TransformOrigin.Center
                    },
            )
        }

        // Crease shadow at the spine, strongest mid-fold.
        val crease = 0.4f * sin(progress * Math.PI).toFloat()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Black.copy(alpha = crease),
                        1f to Color.Transparent,
                    ),
                ),
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val halfWidth = maxWidth / 2

            // ── Folding page: right half of the old portrait frame ──
            // Progressive blur lives on an unrotated full-screen wrapper so
            // the blur is uniform in screen space as the page swings.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= 31) {
                            val a = progress * 90f
                            val r = FoldSpec.MAX_BLUR * (a / 90f) * (a / 90f)
                            if (r > 0.5f) {
                                renderEffect = android.graphics.RenderEffect
                                    .createBlurEffect(r, r, android.graphics.Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            }
                        }
                    },
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(halfWidth)
                        .clipToBounds()
                        .graphicsLayer {
                            rotationY = progress * 90f
                            cameraDistance = FoldSpec.CAMERA
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        },
                ) {
                    // The full window-sized frame, shifted left by half a
                    // window so this clipped half shows the frame's right half.
                    Image(
                        bitmap = frame.old,
                        contentDescription = null,
                        modifier = Modifier
                            .width(maxWidth)
                            .fillMaxHeight()
                            .graphicsLayer { translationX = -maxWidth.toPx() / 2f },
                    )
                    // Edge shading: dark near the spine, growing with the fold.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.Black.copy(alpha = 0.45f * sin(progress * Math.PI).toFloat()),
                                    0.35f to Color.Black.copy(alpha = 0.1f * sin(progress * Math.PI).toFloat()),
                                    1f to Color.Transparent,
                                ),
                            ),
                    )
                }
            }

            // ── Holding page: left half of the old portrait frame ──
            val hold = ((0.85f - progress) / 0.5f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(halfWidth)
                    .clipToBounds()
                    .graphicsLayer { alpha = hold },
            ) {
                Image(
                    bitmap = frame.old,
                    contentDescription = null,
                    modifier = Modifier
                        .width(maxWidth)
                        .fillMaxHeight(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f * sin(progress * Math.PI).toFloat())),
                )
            }
        }
    }
}
