package com.example.ui.tour

import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity

private val Accent = Color(0xFF00BCD4)

/**
 * 360° equirectangular panorama viewer.
 *
 * The panorama bitmap is an equirectangular projection where:
 *   - horizontal axis = longitude (0° to 360°)
 *   - vertical axis = latitude (-90° to +90°)
 *   - aspect ratio = 2:1
 *
 * For each column on screen, we compute the corresponding longitude angle,
 * then sample from the panorama using cylindrical projection.
 *
 * Supports:
 *   - Drag-to-pan (touch)
 *   - Gyroscope auto-rotation
 *   - Multiple room tabs
 *   - Seamless horizontal wrap-around
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

    // Rooms with stitched panoramas
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
            selectedRoom?.panoramaData?.let { JSONObject(it).optString("stitchedPanorama", null) }
        } catch (_: Exception) { null }
    }

    // Load bitmap — scaled down to prevent OOM
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
            Log.e("PanoramaViewer", "Failed to load panorama", e)
            null
        }
    }

    // Pan state — panX in panorama-pixel units
    var panX by remember { mutableFloatStateOf(0f) }
    var gyroOn by remember { mutableStateOf(true) }
    var gyroYaw by remember { mutableFloatStateOf(0f) }

    // Gyroscope
    val sensorMgr = remember { ctx.getSystemService(SensorManager::class.java) }
    val rotVec = remember { sensorMgr?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val rotMat = FloatArray(9)
    val orient = FloatArray(3)

    DisposableEffect(sensorMgr, gyroOn) {
        if (sensorMgr == null || rotVec == null || !gyroOn) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent?) {
                    if (e?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                        SensorManager.getRotationMatrixFromVector(rotMat, e.values)
                        SensorManager.getOrientation(rotMat, orient)
                        gyroYaw = Math.toDegrees(orient[0].toDouble()).toFloat()
                    }
                }
                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            sensorMgr.registerListener(listener, rotVec, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorMgr.unregisterListener(listener) }
        }
    }

    // Map gyro yaw to pan offset
    // yaw = 0° → look at the center of the panorama (panoX = bmpW/2 → lon = 0)
    LaunchedEffect(gyroYaw, gyroOn, bitmap) {
        if (!gyroOn || bitmap == null) return@LaunchedEffect
        val yawRad = Math.toRadians(gyroYaw.toDouble()).toFloat()
        val f = bitmap.width.toFloat() / (2f * PI.toFloat())
        panX = yawRad * f
    }

    // Force landscape orientation while viewing panorama
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
                msg = if (scannedRooms.isNotEmpty()) "Panorama not available. Re-scan this room."
                else "No 3D scans captured yet.",
                onBack = onBack
            )
        } else {
            val bmp = bitmap

            // ── Cylindrical projection viewer ──────────────
            // Each screen column maps to a longitude angle on the sphere.
            // We sample the panorama bitmap at the corresponding position.
            //
            // Focal length f = bmpW / (2π) — maps radians to panorama pixels
            // Screen column sx → angle = atan((sx - cw/2) / f) relative to center
            // Lon = angle + panX/f (panX is in panorama-pixel units)
            // Source X = f × lon + bmpW/2

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            panX -= drag.x // drag left = look left (natural feel)
                        }
                    }
            ) {
                val cw = size.width
                val ch = size.height
                val bmpW = bmp.width.toFloat()
                val bmpH = bmp.height.toFloat()

                // Focal length: bmpW pixels = 2π radians (360°)
                val f = bmpW / (2f * PI.toFloat())

                // Draw vertical strips with cylindrical projection
                val stripW = 2f
                var sx = 0f
                while (sx < cw) {
                    // Screen column → longitude angle
                    val screenAngle = atan((sx - cw / 2f) / f)
                    val lon = screenAngle + panX / f

                    // Source X in panorama bitmap
                    // lon = 0 → center of bitmap (bmpW/2)
                    // lon = -π → left edge (0)
                    // lon = +π → right edge (bmpW)
                    val srcX = (f * lon + bmpW / 2f).toInt()
                    // Wrap around for seamless panorama
                    val srcXWrapped = ((srcX % bmpW.toInt()) + bmpW.toInt()) % bmpW.toInt()

                    // Source Y: sample the full height of the panorama
                    // For a 2:1 equirectangular, the vertical axis is latitude
                    val srcY = 0
                    val srcH = bmpH.toInt()

                    if (srcXWrapped in 0 until bmpW.toInt() && srcH > 0) {
                        drawImage(
                            image = bmp,
                            srcOffset = IntOffset(srcXWrapped, srcY),
                            srcSize = IntSize(1, srcH),
                            dstOffset = IntOffset(sx.roundToInt(), 0),
                            dstSize = IntSize(stripW.roundToInt() + 1, ch.roundToInt())
                        )
                    }

                    sx += stripW
                }
            }

            // ── Overlays ────────────────────────────────────────
            // Top gradient
            Box(Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent))).zIndex(10f).align(Alignment.TopCenter))
            // Bottom gradient
            Box(Modifier.fillMaxWidth().height(80.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))).zIndex(10f).align(Alignment.BottomCenter))

            // Badge
            Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.65f), border = androidx.compose.foundation.BorderStroke(1.dp, Accent.copy(alpha = 0.4f)),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 50.dp).zIndex(11f)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Accent))
                    Spacer(Modifier.width(8.dp))
                    Text("360° PANORAMA • DRAG TO LOOK AROUND", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            // Back button
            IconButton(onClick = onBack, Modifier.padding(top = 44.dp, start = 12.dp).size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).zIndex(11f).align(Alignment.TopStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Gyro toggle
            Surface(shape = RoundedCornerShape(20.dp), color = if (gyroOn) Accent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (gyroOn) Accent else Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.padding(top = 44.dp, end = 12.dp).zIndex(11f).align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(20.dp)).clickable { gyroOn = !gyroOn }) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (gyroOn) Accent else Color.Gray))
                    Spacer(Modifier.width(4.dp))
                    Text(if (gyroOn) "GYRO ON" else "GYRO OFF", color = if (gyroOn) Accent else Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Room tabs
            if (scannedRooms.size > 1) {
                LazyRow(Modifier.fillMaxWidth().padding(bottom = 16.dp, start = 12.dp, end = 12.dp).zIndex(11f).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(scannedRooms) { idx, room ->
                        Surface(shape = RoundedCornerShape(20.dp), color = if (idx == selectedIdx) Accent else Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (idx == selectedIdx) Accent else Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { selectedIdx = idx }) {
                            Text(room.displayName, color = if (idx == selectedIdx) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyView(title: String, msg: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(DorjaColors.Ink950), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            IconButton(onClick = onBack, Modifier.align(Alignment.Start).size(40.dp).clip(CircleShape).background(DorjaColors.Gray700)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(64.dp).clip(CircleShape).background(DorjaColors.Gray700), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ViewInAr, null, tint = Accent, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("No 3D Scans Available", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(msg, color = DorjaColors.Sand300, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))
            DorjaButton("Go Back", onClick = onBack, modifier = Modifier.width(140.dp))
        }
    }
}
