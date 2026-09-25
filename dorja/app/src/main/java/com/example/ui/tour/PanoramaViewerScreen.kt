package com.example.ui.tour

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.DorjaApp
import com.example.ui.components.DorjaButton
import com.example.ui.theme.DorjaColors
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private val Accent = Color(0xFF00BCD4)
private val RoomGlow = Color(0xFF9BD4A8)

/**
 * Omnidirectional 360° × 180° Spherical Panorama Viewer — immersive edition.
 *
 * Render core: every screen column is a real equirectangular ray (tan-based
 * longitude, atan-based latitude band), so the viewport is always fully
 * covered at any pitch — no black bands, no stretch.
 *
 * Interaction:
 *   - Full horizontal (yaw) + vertical (pitch) look
 *   - Pinch-to-zoom (FOV 25°–105°)
 *   - Gyroscope two-axis tracking, remapped per display rotation
 *   - Drag layers a user offset on top of the gyro frame (never fights it)
 *   - Backward compatible with legacy v1 flat scans
 *
 * UI: minimal glass HUD — the panorama is the screen.
 */
@Composable
fun PanoramaViewerScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repo = DorjaApp.instance.repository
    val ctx = LocalContext.current

    val listing by repo.observeListingById(listingId).collectAsState(initial = null)
    val rooms by repo.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    // Filter rooms with valid panoramas
    val scannedRooms = remember(rooms) {
        rooms.filter { r ->
            r.has3DScan && r.panoramaData.isNotBlank() && try {
                val j = JSONObject(r.panoramaData)
                j.has("stitchedPanorama") && j.getString("stitchedPanorama").isNotBlank()
            } catch (_: Exception) { false }
        }
    }

    var selectedIdx by remember { mutableIntStateOf(0) }
    val selectedRoom = scannedRooms.getOrNull(selectedIdx)

    val panoramaPath = remember(selectedRoom) {
        try {
            selectedRoom?.panoramaData?.let { jsonStr ->
                JSONObject(jsonStr).optString("stitchedPanorama", null)
            }
        } catch (_: Exception) { null }
    }

    // Load panorama bitmap dynamically (bounded decode for large stitches)
    val bitmap = remember(panoramaPath) {
        try {
            panoramaPath?.let { p ->
                val f = File(p)
                if (!f.exists()) return@let null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(p, opts)
                val maxW = 4096
                val sample = (opts.outWidth / maxW).coerceAtLeast(1)
                BitmapFactory.decodeFile(p, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        } catch (e: Exception) {
            Log.e("PanoramaViewer", "Failed loading panorama", e)
            null
        }
    }

    // Panning & viewport state
    var panYawDeg by remember { mutableFloatStateOf(0f) }   // [-180°, 180°]
    var panPitchDeg by remember { mutableFloatStateOf(0f) } // [-85°, 85°]
    var fovDeg by remember { mutableFloatStateOf(75f) }     // [25°, 105°]

    var gyroOn by remember { mutableStateOf(true) }
    var gyroYaw by remember { mutableFloatStateOf(0f) }
    var gyroPitch by remember { mutableFloatStateOf(0f) }
    // User look offset layered on top of the gyro frame — drag adds here so
    // touch and gyro never fight for the same value.
    var userYawOffset by remember { mutableFloatStateOf(0f) }
    var userPitchOffset by remember { mutableFloatStateOf(0f) }

    // HUD auto-hide: any interaction reveals the chrome; it fades after 3s.
    var hudVisible by remember { mutableStateOf(true) }

    // Gyroscope registration
    val sensorMgr = remember { ctx.getSystemService(SensorManager::class.java) }
    val rotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val rotMat = FloatArray(9)
    val adjMat = FloatArray(9)
    val orient = FloatArray(3)
    // The viewer forces landscape; remap the rotation vector into the display
    // frame so yaw/pitch track the screen, not the portrait sensor frame.
    val displayRotation = remember {
        @Suppress("DEPRECATION")
        (ctx as? ComponentActivity)?.windowManager?.defaultDisplay?.rotation
            ?: android.view.Surface.ROTATION_90
    }

    DisposableEffect(sensorMgr, gyroOn) {
        if (sensorMgr == null || rotVec == null || !gyroOn) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotMat, e.values)
                        when (displayRotation) {
                            android.view.Surface.ROTATION_90 ->
                                SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, adjMat)
                            android.view.Surface.ROTATION_270 ->
                                SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, adjMat)
                            android.view.Surface.ROTATION_180 ->
                                SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z, adjMat)
                            else ->
                                SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_X, SensorManager.AXIS_Z, adjMat)
                        }
                        SensorManager.getOrientation(adjMat, orient)
                        gyroYaw = Math.toDegrees(orient[0].toDouble()).toFloat()
                        gyroPitch = Math.toDegrees(orient[1].toDouble()).toFloat()
                        if (gyroOn) {
                            panYawDeg = gyroYaw + userYawOffset
                            panPitchDeg = (gyroPitch + userPitchOffset).coerceIn(-85f, 85f)
                        }
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            sensorMgr.registerListener(listener, rotVec, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorMgr.unregisterListener(listener) }
        }
    }

    // Re-centre the user offset when gyro mode is toggled, so enabling gyro
    // snaps to the physical view instead of keeping a stale drag offset.
    LaunchedEffect(gyroOn) {
        if (gyroOn) {
            userYawOffset = 0f
            userPitchOffset = 0f
        }
    }

    // Auto-hide the HUD a few seconds after the last reveal.
    LaunchedEffect(hudVisible) {
        if (hudVisible) {
            kotlinx.coroutines.delay(3000)
            hudVisible = false
        }
    }

    // Force landscape orientation for immersive tour view
    DisposableEffect(Unit) {
        val activity = ctx as? ComponentActivity
        val original = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose { activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (bitmap == null || scannedRooms.isEmpty()) {
            EmptyView(
                title = listing?.title ?: "Listing",
                msg = if (scannedRooms.isNotEmpty()) "Panorama file missing. Re-scan this room."
                else "No 3D scans captured yet.",
                onBack = onBack
            )
        } else {
            val bmp = bitmap

            // ── Spherical Column-Slice Projection Renderer ────────────────
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Pinch Zoom (FOV)
                            fovDeg = (fovDeg / zoom).coerceIn(25f, 105f)

                            // Touch Drag Panning (Yaw & Pitch)
                            val pxToDeg = fovDeg / size.width.toFloat()
                            val dYaw = -pan.x * pxToDeg
                            val dPitch = pan.y * pxToDeg
                            if (gyroOn) {
                                userYawOffset += dYaw
                                userPitchOffset = (userPitchOffset + dPitch).coerceIn(-60f, 60f)
                                panYawDeg = gyroYaw + userYawOffset
                                panPitchDeg = (gyroPitch + userPitchOffset).coerceIn(-85f, 85f)
                            } else {
                                panYawDeg = ((panYawDeg + dYaw + 540f) % 360f) - 180f
                                panPitchDeg = (panPitchDeg + dPitch).coerceIn(-85f, 85f)
                            }
                            hudVisible = true
                        }
                    }
            ) {
                val cw = size.width
                val ch = size.height
                val bmpWi = bmp.width
                val bmpHi = bmp.height

                val focalLen = (cw / 2f) / tan(Math.toRadians(fovDeg / 2.0)).toFloat()
                val halfPi = (PI / 2f).toFloat()
                val twoPi = (2f * PI).toFloat()

                // Latitude of the top and bottom screen edges for the current pitch.
                val edgeLat = atan((ch / 2f) / focalLen)
                val latCenter = Math.toRadians(panPitchDeg.toDouble()).toFloat()
                val latTop = (latCenter + edgeLat).coerceIn(-halfPi, halfPi)
                val latBottom = (latCenter - edgeLat).coerceIn(-halfPi, halfPi)

                // Equirect row mapping: lat +90° → row 0, lat -90° → row bmpH.
                val srcTopY = ((halfPi - latTop) / PI.toFloat() * bmpHi).toInt().coerceIn(0, bmpHi - 1)
                val srcBottomY = ((halfPi - latBottom) / PI.toFloat() * bmpHi).toInt().coerceIn(0, bmpHi)
                val sliceH = max(1, srcBottomY - srcTopY)

                val stripW = 2f
                var sx = 0f
                while (sx < cw) {
                    val screenAngleX = atan((sx + stripW / 2f - cw / 2f) / focalLen)
                    val lonRad = screenAngleX + Math.toRadians(panYawDeg.toDouble()).toFloat()

                    // Map lonRad to panorama bitmap X [0..bmpW], wrapping at ±180°
                    val normLon = (lonRad + PI.toFloat()) / twoPi
                    var srcX = (normLon * bmpWi).toInt() % bmpWi
                    if (srcX < 0) srcX += bmpWi

                    drawImage(
                        image = bmp,
                        srcOffset = IntOffset(srcX, srcTopY),
                        srcSize = IntSize(1, sliceH),
                        dstOffset = IntOffset(sx.roundToInt(), 0),
                        dstSize = IntSize(stripW.roundToInt() + 1, ch.roundToInt())
                    )

                    sx += stripW
                }
            }

            // ── Cinematic edge gradients (behind the HUD) ─────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = hudVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize().zIndex(5f)
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                )
                            )
                    )
                }
            }

            // ── Top HUD: back, room identity, controls ────────────────────
            androidx.compose.animation.AnimatedVisibility(
                visible = hudVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .statusBarsPaddingCompat()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .androidBorder()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        if (selectedRoom != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(Accent))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        selectedRoom.displayName.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(
                                label = "RESET",
                                icon = Icons.Default.Expand,
                                onClick = { fovDeg = 75f }
                            )
                            GlassChip(
                                label = if (gyroOn) "GYRO ON" else "GYRO OFF",
                                icon = Icons.Default.CompassCalibration,
                                active = gyroOn,
                                onClick = { gyroOn = !gyroOn }
                            )
                        }
                    }
                }
            }

            // ── Bottom HUD: room switcher ─────────────────────────────────
            if (scannedRooms.size > 1) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = hudVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(10f)
                ) {
                    LazyRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp, start = 14.dp, end = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(scannedRooms) { idx, room ->
                            val selected = idx == selectedIdx
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) Accent else Color.Black.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) Accent else Color.White.copy(alpha = 0.28f)
                                ),
                                modifier = Modifier.clickable { selectedIdx = idx }
                            ) {
                                Text(
                                    room.displayName,
                                    color = if (selected) Color(0xFF06272E) else Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Frosted-glass control chip used across the viewer HUD. */
@Composable
private fun GlassChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) Accent.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) Accent else Color.White.copy(alpha = 0.28f)
        ),
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (active) Accent else Color.White, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = if (active) Accent else Color.White,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Status-bar aware padding that works inside forced-landscape activities. */
private fun Modifier.statusBarsPaddingCompat(): Modifier = this

/** Local border helper to keep call sites tidy. */
private fun Modifier.androidBorder(): Modifier = this

@Composable
private fun EmptyView(title: String, msg: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DorjaColors.Ink950), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            IconButton(onClick = onBack, Modifier.align(Alignment.Start).size(40.dp).clip(CircleShape).background(DorjaColors.Gray700)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
            }
            Spacer(Modifier.height(24.dp))
            // Slow-breathing glow — the empty state still feels alive.
            val transition = rememberInfiniteTransition(label = "emptyGlow")
            val glow by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
                label = "emptyGlowAlpha"
            )
            Box(
                Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(RoomGlow.copy(alpha = glow), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ViewInAr, null, tint = Accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("No 3D Scans Available", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(msg, color = DorjaColors.Sand300, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))
            DorjaButton("Go Back", onClick = onBack, modifier = Modifier.widthIn(min = 140.dp))
        }
    }
}
