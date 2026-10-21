package com.example.ui.tour

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ThreeSixty
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.DorjaApp
import com.example.ui.components.DorjaButton
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.DorjaFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private const val TAG = "PanoramaViewerScreen"
private val Accent = Color(0xFF00BCD4)
private val FisheyeAccent = Color(0xFFFFB300)
private val RoomGlow = Color(0xFF9BD4A8)

private data class PanoramaMeta(
    val filePath: String,
    val longitudeSpanDegrees: Float,
    val latitudeSpanDegrees: Float,
    val centerLongitudeDegrees: Float,
    val centerLatitudeDegrees: Float,
)

/**
 * 360° Spherical Panorama Viewer with Photo-Sphere-Viewer Normal + Fisheye OpenGL ES 2.0
 * ray-casting shaders, gyroscope + touch look-around, and a polite Landscape Tilt Prompt.
 */
@Composable
fun PanoramaViewerScreen(
    listingId: String,
    onBack: () -> Unit,
) {
    val repo = DorjaApp.instance.repository
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val listing by repo.observeListingById(listingId).collectAsState(initial = null)
    val rooms by repo.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    val scannedRooms = remember(rooms) {
        rooms.filter { r ->
            r.has3DScan && r.panoramaData.isNotBlank() && try {
                val j = JSONObject(r.panoramaData)
                j.has("stitchedPanorama") && j.getString("stitchedPanorama").isNotBlank()
            } catch (_: Exception) {
                false
            }
        }
    }

    var selectedIdx by remember { mutableIntStateOf(0) }
    val selectedRoom = scannedRooms.getOrNull(selectedIdx)

    val panoramaMeta = remember(selectedRoom) {
        selectedRoom?.panoramaData?.let { jsonStr ->
            runCatching {
                val j = JSONObject(jsonStr)
                val path = j.optString("stitchedPanorama", "")
                if (path.isBlank()) null
                else {
                    PanoramaMeta(
                        filePath = path,
                        longitudeSpanDegrees = j.optDouble("longitudeSpanDegrees", 360.0).toFloat()
                            .coerceIn(30f, 360f),
                        latitudeSpanDegrees = j.optDouble("latitudeSpanDegrees", Double.NaN).toFloat(),
                        centerLongitudeDegrees = j.optDouble("centerLongitudeDegrees", 0.0).toFloat(),
                        centerLatitudeDegrees = j.optDouble("centerLatitudeDegrees", 0.0).toFloat(),
                    )
                }
            }.getOrNull()
        }
    }

    // Track physical phone tilt via accelerometer so we can politely invite the user to tilt to
    // landscape when holding the phone upright, and auto-rotate to landscape as soon as they tilt it!
    var isPhoneHeldPortrait by remember {
        mutableStateOf(configuration.orientation != Configuration.ORIENTATION_LANDSCAPE)
    }
    var landscapePromptDismissed by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val activity = context.findActivity()
        val prevOrientation = activity?.requestedOrientation
        // Allow sensor-driven rotation between portrait and landscape so tilting the phone rotates naturally
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gx = event.values[0]
                val gy = event.values[1]
                // When |gx| > |gy| * 1.15, the phone is physically tilted into landscape
                if (abs(gx) > abs(gy) * 1.18f && abs(gx) > 3.5f) {
                    isPhoneHeldPortrait = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else if (abs(gy) > abs(gx) * 1.25f && abs(gy) > 4.5f) {
                    isPhoneHeldPortrait = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
            if (prevOrientation != null) {
                activity.requestedOrientation = prevOrientation
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(false) }
    var effectiveLatSpanDeg by remember { mutableFloatStateOf(180f) }
    var effectiveCenterLatDeg by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(panoramaMeta) {
        val meta = panoramaMeta
        if (meta == null) {
            bitmap = null
            return@LaunchedEffect
        }
        isLoadingBitmap = true
        val loaded = withContext(Dispatchers.IO) {
            val f = File(meta.filePath)
            if (!f.exists()) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            var sample = 1
            while (max(bounds.outWidth / sample, bounds.outHeight / sample) > 4096) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(f.absolutePath, opts)
        }
        if (loaded != null) {
            val inferredSpan = if (!meta.latitudeSpanDegrees.isNaN() && meta.latitudeSpanDegrees > 5f) {
                meta.latitudeSpanDegrees.coerceIn(30f, 180f)
            } else {
                // Infer spherical latitude band from aspect ratio (square-pixel equirectangular rule)
                ((loaded.height.toFloat() / loaded.width.toFloat()) * 360f).coerceIn(40f, 180f)
            }
            effectiveLatSpanDeg = inferredSpan
            effectiveCenterLatDeg = meta.centerLatitudeDegrees.coerceIn(-45f, 45f)
        }
        bitmap = loaded
        isLoadingBitmap = false
    }

    // Camera view state: Normal vs Fisheye (Photo-Sphere-Viewer style), Gyro, Yaw/Pitch/FOV
    var isFisheyeEnabled by remember { mutableStateOf(false) }
    val fisheyeStrength by animateFloatAsState(
        targetValue = if (isFisheyeEnabled) 1.55f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "fisheyeStrength",
    )

    var manualYawDeg by remember { mutableFloatStateOf(0f) }
    var manualPitchDeg by remember { mutableFloatStateOf(0f) }
    var vFovDeg by remember { mutableFloatStateOf(65f) }

    var gyroEnabled by remember { mutableStateOf(true) }
    var gyroYawDeg by remember { mutableFloatStateOf(0f) }
    var gyroPitchDeg by remember { mutableFloatStateOf(0f) }
    var gyroBaseYaw by remember { mutableStateOf<Float?>(null) }
    var gyroBasePitch by remember { mutableStateOf<Float?>(null) }

    // Gyroscope tracking (TYPE_GAME_ROTATION_VECTOR preferred)
    DisposableEffect(gyroEnabled) {
        if (!gyroEnabled) return@DisposableEffect onDispose {}
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotSensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sm == null || rotSensor == null) return@DisposableEffect onDispose {}

        val R = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = if (event.values.size > 4) event.values.copyOfRange(0, 4) else event.values
                runCatching { SensorManager.getRotationMatrixFromVector(R, values) }.onFailure { return }

                // Forward direction of back camera (-Z_device in world)
                val fx = -R[2].toDouble()
                val fy = -R[5].toDouble()
                val fz = -R[8].toDouble()

                val rawYaw = Math.toDegrees(atan2(fx, fy)).toFloat()
                val rawPitch = Math.toDegrees(asin(fz.coerceIn(-1.0, 1.0))).toFloat()

                if (gyroBaseYaw == null) gyroBaseYaw = rawYaw
                if (gyroBasePitch == null) gyroBasePitch = rawPitch

                val targetYaw = shortestAngleDelta(gyroBaseYaw ?: rawYaw, rawYaw)
                val targetPitch = (rawPitch - (gyroBasePitch ?: rawPitch)).coerceIn(-80f, 80f)

                gyroYawDeg += shortestAngleDelta(gyroYawDeg, targetYaw) * 0.22f
                gyroPitchDeg += (targetPitch - gyroPitchDeg) * 0.22f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sm.registerListener(listener, rotSensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    // Clamp pitch so the view stays comfortably inside the captured latitude band in Normal mode
    // while allowing full sphere exploration in Fisheye mode
    val maxPitchDeg = remember(effectiveLatSpanDeg, isFisheyeEnabled) {
        if (isFisheyeEnabled) {
            75f
        } else {
            max(18f, effectiveLatSpanDeg * 0.5f - 8f).coerceAtMost(85f)
        }
    }

    val totalYawDeg = ((manualYawDeg + if (gyroEnabled) gyroYawDeg else 0f) % 360f + 360f) % 360f
    val totalPitchDeg = (manualPitchDeg + if (gyroEnabled) gyroPitchDeg * 0.65f else 0f)
        .coerceIn(-maxPitchDeg, maxPitchDeg)

    val isLandscapeConfig = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val showLandscapePrompt = (!isLandscapeConfig || isPhoneHeldPortrait) &&
        !landscapePromptDismissed &&
        scannedRooms.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090D))
    ) {
        if (scannedRooms.isEmpty()) {
            EmptyScanState(onBack = onBack)
        } else {
            val currentBitmap = bitmap
            if (currentBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(selectedIdx, maxPitchDeg, isFisheyeEnabled) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (zoom != 1f && zoom > 0f) {
                                    vFovDeg = (vFovDeg / zoom).coerceIn(30f, 100f)
                                }
                                val widthPx = size.width.coerceAtLeast(1).toFloat()
                                val heightPx = size.height.coerceAtLeast(1).toFloat()
                                val effectiveFov = vFovDeg + fisheyeStrength * 18f
                                val degPerPxX = effectiveFov / widthPx * 1.45f
                                val degPerPxY = effectiveFov / heightPx * 1.25f
                                manualYawDeg = (manualYawDeg - pan.x * degPerPxX) % 360f
                                manualPitchDeg = (manualPitchDeg + pan.y * degPerPxY)
                                    .coerceIn(-maxPitchDeg, maxPitchDeg)
                            }
                        }
                ) {
                    PhotoSphereGlSurface(
                        bitmap = currentBitmap,
                        yawDegrees = totalYawDeg,
                        pitchDegrees = totalPitchDeg,
                        vFovDegrees = vFovDeg,
                        fisheyeAmount = fisheyeStrength,
                        latitudeSpanDegrees = effectiveLatSpanDeg,
                        centerLatitudeDegrees = effectiveCenterLatDeg,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else if (isLoadingBitmap) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Accent)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading 360° Sphere…",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // ── TOP HUD: Back Button + Room Title + FISHEYE / GYRO / RESET Controls ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(2f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = selectedRoom?.displayName ?: "360° Room Scan",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = listing?.title ?: if (isFisheyeEnabled) "360° Fisheye Sphere" else "360° Horizon Panorama",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                        )
                    }
                }

                // Right HUD Controls: FISHEYE Toggle + GYRO Toggle + RESET
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Prominent FISHEYE Effect Button (Photo-Sphere-Viewer style)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isFisheyeEnabled) {
                            FisheyeAccent.copy(alpha = 0.25f)
                        } else {
                            Color.Black.copy(alpha = 0.58f)
                        },
                        border = BorderStroke(
                            1.2.dp,
                            if (isFisheyeEnabled) FisheyeAccent else Color.White.copy(alpha = 0.28f),
                        ),
                        modifier = Modifier
                            .testTag("fisheye_toggle_button")
                            .clickable { isFisheyeEnabled = !isFisheyeEnabled },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PanoramaFishEye,
                                contentDescription = "Toggle Fisheye 360° Effect",
                                tint = if (isFisheyeEnabled) FisheyeAccent else Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = if (isFisheyeEnabled) "FISHEYE ON" else "FISHEYE",
                                color = if (isFisheyeEnabled) FisheyeAccent else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DorjaFontFamily,
                            )
                        }
                    }

                    // GYRO Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (gyroEnabled) {
                            Accent.copy(alpha = 0.22f)
                        } else {
                            Color.Black.copy(alpha = 0.58f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (gyroEnabled) Accent else Color.White.copy(alpha = 0.22f),
                        ),
                        modifier = Modifier.clickable {
                            gyroEnabled = !gyroEnabled
                            gyroBaseYaw = null
                            gyroBasePitch = null
                            gyroYawDeg = 0f
                            gyroPitchDeg = 0f
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = "Toggle Gyro",
                                tint = if (gyroEnabled) Accent else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = if (gyroEnabled) "GYRO ON" else "GYRO OFF",
                                color = if (gyroEnabled) Accent else Color.White.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DorjaFontFamily,
                            )
                        }
                    }

                    // RESET View Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.58f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier.clickable {
                            manualYawDeg = 0f
                            manualPitchDeg = 0f
                            vFovDeg = 65f
                            gyroBaseYaw = null
                            gyroBasePitch = null
                            gyroYawDeg = 0f
                            gyroPitchDeg = 0f
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset View",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "RESET",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = DorjaFontFamily,
                            )
                        }
                    }
                }
            }

            // ── POLITE LANDSCAPE TILT PROMPT ──
            AnimatedVisibility(
                visible = showLandscapePrompt,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(4f)
                    .padding(horizontal = 24.dp),
            ) {
                PoliteLandscapeTiltCard(
                    onSwitchToLandscape = {
                        context.findActivity()?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        landscapePromptDismissed = true
                    },
                    onDismiss = { landscapePromptDismissed = true },
                )
            }

            // ── BOTTOM HUD: Compass Heading + Room Switcher Strip ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val headingInt = totalYawDeg.roundToInt() % 360
                    val cardinal = when (headingInt) {
                        in 338..360, in 0..22 -> "N"
                        in 23..67 -> "NE"
                        in 68..112 -> "E"
                        in 113..157 -> "SE"
                        in 158..202 -> "S"
                        in 203..247 -> "SW"
                        in 248..292 -> "W"
                        else -> "NW"
                    }
                    Text(
                        text = "$headingInt° $cardinal  •  ${if (isFisheyeEnabled) "FISHEYE 360°" else "NORMAL 360°"}  •  DRAG OR TILT TO EXPLORE",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        fontFamily = DorjaFontFamily,
                        letterSpacing = 1.1.sp,
                    )
                }

                if (scannedRooms.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(scannedRooms) { idx, room ->
                            val selected = idx == selectedIdx
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) RoomGlow else Color.Black.copy(alpha = 0.55f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) RoomGlow else Color.White.copy(alpha = 0.2f),
                                ),
                                modifier = Modifier.clickable {
                                    selectedIdx = idx
                                    manualYawDeg = 0f
                                    manualPitchDeg = 0f
                                    gyroBaseYaw = null
                                    gyroBasePitch = null
                                },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThreeSixty,
                                        contentDescription = null,
                                        tint = if (selected) Color(0xFF0A1410) else Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = room.displayName,
                                        color = if (selected) Color(0xFF0A1410) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Polite, friendly card asking the user to tilt their device to landscape mode for the best
 * 360° panorama viewing experience.
 */
@Composable
private fun PoliteLandscapeTiltCard(
    onSwitchToLandscape: () -> Unit,
    onDismiss: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "phoneTilt")
    val phoneRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tiltAngle",
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF101622).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Accent.copy(alpha = 0.45f)),
        shadowElevation = 16.dp,
        modifier = Modifier
            .widthIn(max = 380.dp)
            .testTag("landscape_tilt_prompt"),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ScreenRotation,
                    contentDescription = "Tilt phone to landscape",
                    tint = Accent,
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(phoneRotation),
                )
            }

            Text(
                text = "Tilt Your Device to Landscape",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Please gently tilt your phone sideways into landscape mode for the widest, most immersive 360° room view!",
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Stay Here", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSwitchToLandscape,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Color(0xFF061014),
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1.3f),
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tilt Landscape", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyScanState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.ViewInAr,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No 3D Scans Available",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No rooms in this listing have been scanned yet. Use the 360° Horizon Room Scanner to capture panoramic views.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        DorjaButton(
            text = "Go Back",
            onClick = onBack,
            backgroundColor = Accent,
            modifier = Modifier.widthIn(min = 160.dp),
        )
    }
}

/**
 * OpenGL ES 2.0 Spherical + Fisheye Panorama View matching `Photo-Sphere-Viewer`'s
 * `Renderer.ts` and `equirectangular.fragment.glsl`.
 */
@Composable
private fun PhotoSphereGlSurface(
    bitmap: Bitmap,
    yawDegrees: Float,
    pitchDegrees: Float,
    vFovDegrees: Float,
    fisheyeAmount: Float,
    latitudeSpanDegrees: Float,
    centerLatitudeDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val renderer = remember { PhotoSphereGlRenderer() }
    val glView = remember(context) {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(2)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    LaunchedEffect(bitmap) {
        renderer.setBitmap(bitmap)
        glView.requestRender()
    }

    LaunchedEffect(
        yawDegrees,
        pitchDegrees,
        vFovDegrees,
        fisheyeAmount,
        latitudeSpanDegrees,
        centerLatitudeDegrees,
    ) {
        renderer.updateCamera(
            yawDeg = yawDegrees,
            pitchDeg = pitchDegrees,
            vFovDeg = vFovDegrees,
            fisheye = fisheyeAmount,
            latSpanDeg = latitudeSpanDegrees,
            centerLatDeg = centerLatitudeDegrees,
        )
        glView.requestRender()
    }

    DisposableEffect(lifecycleOwner, glView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier,
    )
}

/**
 * OpenGL ES 2.0 ray-casting renderer implementing `Photo-Sphere-Viewer`'s exact spherical
 * projection for both **Normal** (`fisheye == 0`) and **Fisheye** (`fisheye > 0`) modes.
 *
 * In `Photo-Sphere-Viewer` (`packages/core/src/services/Renderer.ts`):
 * - The panorama texture lives on a sphere of radius `R = 1.0`.
 * - In Normal mode (`fisheye = 0`), the virtual camera sits at `(0, 0, 0)` looking along `uForward`.
 * - In Fisheye mode (`fisheye > 0`), the virtual camera is pulled backward along `-uForward`
 *   by `distance = (fisheye / 2.0) * R` and vertical FOV widens, producing the signature
 *   stereographic / fisheye curvature across the 360° sphere.
 */
private class PhotoSphereGlRenderer : GLSurfaceView.Renderer {

    @Volatile private var pendingBitmap: Bitmap? = null
    @Volatile private var bitmapDirty: Boolean = false

    @Volatile private var yawDeg: Float = 0f
    @Volatile private var pitchDeg: Float = 0f
    @Volatile private var vFovDeg: Float = 65f
    @Volatile private var fisheye: Float = 0f
    @Volatile private var latSpanDeg: Float = 180f
    @Volatile private var centerLatDeg: Float = 0f

    private var viewportWidth: Int = 1
    private var viewportHeight: Int = 1

    private var programId: Int = 0
    private var textureId: Int = 0
    private var hasTexture: Boolean = false

    private var aPositionLoc: Int = -1
    private var uRightLoc: Int = -1
    private var uUpLoc: Int = -1
    private var uForwardLoc: Int = -1
    private var uLocalCamLoc: Int = -1
    private var uTanHalfVFovLoc: Int = -1
    private var uAspectLoc: Int = -1
    private var uLatSpanRadLoc: Int = -1
    private var uCenterLatRadLoc: Int = -1
    private var uTextureLoc: Int = -1

    private val quadBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(
                floatArrayOf(
                    -1f, -1f,
                    1f, -1f,
                    -1f, 1f,
                    1f, 1f,
                )
            )
            position(0)
        }

    fun setBitmap(bitmap: Bitmap) {
        pendingBitmap = bitmap
        bitmapDirty = true
    }

    fun updateCamera(
        yawDeg: Float,
        pitchDeg: Float,
        vFovDeg: Float,
        fisheye: Float,
        latSpanDeg: Float,
        centerLatDeg: Float,
    ) {
        this.yawDeg = yawDeg
        this.pitchDeg = pitchDeg
        this.vFovDeg = vFovDeg
        this.fisheye = fisheye
        this.latSpanDeg = latSpanDeg
        this.centerLatDeg = centerLatDeg
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.05f, 0.08f, 1.0f)
        programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPositionLoc = GLES20.glGetAttribLocation(programId, "aPosition")
        uRightLoc = GLES20.glGetUniformLocation(programId, "uRight")
        uUpLoc = GLES20.glGetUniformLocation(programId, "uUp")
        uForwardLoc = GLES20.glGetUniformLocation(programId, "uForward")
        uLocalCamLoc = GLES20.glGetUniformLocation(programId, "uLocalCam")
        uTanHalfVFovLoc = GLES20.glGetUniformLocation(programId, "uTanHalfVFov")
        uAspectLoc = GLES20.glGetUniformLocation(programId, "uAspect")
        uLatSpanRadLoc = GLES20.glGetUniformLocation(programId, "uLatSpanRad")
        uCenterLatRadLoc = GLES20.glGetUniformLocation(programId, "uCenterLatRad")
        uTextureLoc = GLES20.glGetUniformLocation(programId, "uTexture")

        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        textureId = texIds[0]
        hasTexture = false
        bitmapDirty = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (programId == 0) return

        val bmp = pendingBitmap
        if (bitmapDirty && bmp != null && !bmp.isRecycled) {
            bitmapDirty = false
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            hasTexture = true
        }

        if (!hasTexture) return

        GLES20.glUseProgram(programId)

        val yawRad = Math.toRadians(yawDeg.toDouble())
        val pitchRad = Math.toRadians(pitchDeg.toDouble())
        val cosPitch = cos(pitchRad)
        val sinPitch = sin(pitchRad)
        val sinYaw = sin(yawRad)
        val cosYaw = cos(yawRad)

        // Camera orthonormal basis in world space (Y up, Z north, X east)
        val fx = (sinYaw * cosPitch).toFloat()
        val fy = sinPitch.toFloat()
        val fz = (cosYaw * cosPitch).toFloat()

        val rx = cosYaw.toFloat()
        val ry = 0f
        val rz = (-sinYaw).toFloat()

        val ux = (-sinYaw * sinPitch).toFloat()
        val uy = cosPitch.toFloat()
        val uz = (-cosYaw * sinPitch).toFloat()

        // Photo-Sphere-Viewer fisheye offset: pull camera backward by (fisheye * 0.5) along -forward
        val fAmount = fisheye.coerceIn(0f, 3.0f)
        val camOffset = (fAmount * 0.48f).coerceIn(0f, 0.92f)
        val localCamX = -fx * camOffset
        val localCamY = -fy * camOffset
        val localCamZ = -fz * camOffset

        // Effective vertical FOV widens smoothly in Fisheye mode (matching Photo-Sphere-Viewer)
        val effectiveVFov = (vFovDeg + fAmount * 22f).coerceIn(25f, 138f)
        val tanHalfVFov = tan(Math.toRadians(effectiveVFov / 2.0)).toFloat()
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()

        val latSpanRad = Math.toRadians(latSpanDeg.coerceIn(25f, 180f).toDouble()).toFloat()
        val centerLatRad = Math.toRadians(centerLatDeg.coerceIn(-60f, 60f).toDouble()).toFloat()

        GLES20.glUniform3f(uRightLoc, rx, ry, rz)
        GLES20.glUniform3f(uUpLoc, ux, uy, uz)
        GLES20.glUniform3f(uForwardLoc, fx, fy, fz)
        GLES20.glUniform3f(uLocalCamLoc, localCamX, localCamY, localCamZ)
        GLES20.glUniform1f(uTanHalfVFovLoc, tanHalfVFov)
        GLES20.glUniform1f(uAspectLoc, aspect)
        GLES20.glUniform1f(uLatSpanRadLoc, latSpanRad)
        GLES20.glUniform1f(uCenterLatRadLoc, centerLatRad)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, quadBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPositionLoc)
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            varying vec2 vNdc;
            void main() {
                vNdc = aPosition;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision highp float;
            varying vec2 vNdc;
            uniform vec3 uRight;
            uniform vec3 uUp;
            uniform vec3 uForward;
            uniform vec3 uLocalCam;
            uniform float uTanHalfVFov;
            uniform float uAspect;
            uniform float uLatSpanRad;
            uniform float uCenterLatRad;
            uniform sampler2D uTexture;

            const float PI = 3.141592653589793;

            void main() {
                vec3 rayDir = normalize(
                    vNdc.x * uTanHalfVFov * uAspect * uRight +
                    vNdc.y * uTanHalfVFov * uUp +
                    uForward
                );

                // Intersect ray (uLocalCam + t * rayDir) with unit sphere |P|^2 = 1.0
                float a = dot(rayDir, rayDir);
                float b = dot(uLocalCam, rayDir);
                float c = dot(uLocalCam, uLocalCam) - 1.0;
                float disc = max(b * b - a * c, 0.0);
                float t = (-b + sqrt(disc)) / a;
                vec3 dir = normalize(uLocalCam + t * rayDir);

                float lon = atan(dir.x, dir.z);
                float lat = asin(clamp(dir.y, -1.0, 1.0));

                float u = fract(lon / (2.0 * PI) + 0.5);
                float v = 0.5 - (lat - uCenterLatRad) / uLatSpanRad;

                if (v >= 0.0 && v <= 1.0) {
                    vec4 texColor = texture2D(uTexture, vec2(u, clamp(v, 0.001, 0.999)));
                    // Subtle feather at horizon band boundaries when viewing in wide Fisheye mode
                    float edgeDist = min(v, 1.0 - v);
                    float edgeFade = smoothstep(0.0, 0.025, edgeDist);
                    vec3 ambient = mix(vec3(0.05, 0.07, 0.10), texColor.rgb * 0.35, 0.5);
                    gl_FragColor = vec4(mix(ambient, texColor.rgb, edgeFade), 1.0);
                } else {
                    float over = v < 0.0 ? -v : (v - 1.0);
                    float clampedV = clamp(v, 0.002, 0.998);
                    vec3 edgeSample = texture2D(uTexture, vec2(u, clampedV)).rgb;
                    float fade = exp(-over * 7.5);
                    vec3 studioCap = v < 0.0 ? vec3(0.07, 0.09, 0.13) : vec3(0.04, 0.05, 0.08);
                    gl_FragColor = vec4(mix(studioCap, edgeSample * 0.45, fade), 1.0);
                }
            }
        """

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                Log.e(TAG, "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
                GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            if (vs == 0 || fs == 0) return 0
            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vs)
            GLES20.glAttachShader(program, fs)
            GLES20.glLinkProgram(program)
            val status = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                Log.e(TAG, "Program link error: ${GLES20.glGetProgramInfoLog(program)}")
                GLES20.glDeleteProgram(program)
                return 0
            }
            return program
        }
    }
}

private fun shortestAngleDelta(fromDeg: Float, toDeg: Float): Float {
    var d = (toDeg - fromDeg) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
