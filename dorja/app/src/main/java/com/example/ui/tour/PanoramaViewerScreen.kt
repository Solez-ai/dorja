package com.example.ui.tour

import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.roundToInt

private val CyanAccent = Color(0xFF00BCD4)

@Composable
fun PanoramaViewerScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val context = LocalContext.current

    val listing by repository.observeListingById(listingId).collectAsState(initial = null)
    val rooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    // Filter rooms that have 3D scans with stitched panorama
    val scannedRooms = remember(rooms) {
        rooms.filter { room ->
            room.has3DScan && room.panoramaData.isNotBlank() && try {
                val json = JSONObject(room.panoramaData)
                json.has("stitchedPanorama") && json.getString("stitchedPanorama").isNotBlank()
            } catch (e: Exception) { false }
        }
    }

    var selectedRoomIndex by remember { mutableIntStateOf(0) }
    val selectedRoom = scannedRooms.getOrNull(selectedRoomIndex)

    // Get stitched panorama path
    val panoramaPath = remember(selectedRoom) {
        try {
            if (selectedRoom?.panoramaData.isNullOrBlank()) null
            else {
                val json = JSONObject(selectedRoom!!.panoramaData)
                json.optString("stitchedPanorama", null)
            }
        } catch (e: Exception) { null }
    }

    // Load bitmap
    val panoramaBitmap = remember(panoramaPath) {
        try {
            if (panoramaPath == null) null
            else {
                val file = File(panoramaPath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(panoramaPath)?.asImageBitmap()
                } else {
                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(Uri.parse(panoramaPath))
                    )?.asImageBitmap()
                }
            }
        } catch (e: Exception) { null }
    }

    // Pan offset (horizontal scroll in pixels, wraps around)
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var gyroEnabled by remember { mutableStateOf(true) }

    // Gyroscope state
    var gyroYaw by remember { mutableFloatStateOf(0f) }
    var lastGyroTime by remember { mutableStateOf(0L) }

    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val rotationVector = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    DisposableEffect(sensorManager, gyroEnabled) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR && gyroEnabled) {
                    val now = System.currentTimeMillis()
                    if (now - lastGyroTime > 33) { // ~30fps throttle
                        lastGyroTime = now
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        gyroYaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager?.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    // Auto-pan with gyroscope
    LaunchedEffect(gyroYaw, gyroEnabled, panoramaBitmap) {
        if (!gyroEnabled || panoramaBitmap == null) return@LaunchedEffect
        val bmpWidth = panoramaBitmap.width.toFloat()
        if (bmpWidth <= 0f) return@LaunchedEffect
        // Map yaw (0-360) to pan offset (0 to bmpWidth)
        // The panorama repeats every 360°, so we map yaw directly
        val targetOffset = (gyroYaw / 360f) * bmpWidth
        panOffsetX = targetOffset
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (panoramaBitmap == null || scannedRooms.isEmpty()) {
            EmptyPanoramaView(
                listingTitle = listing?.title ?: "Listing",
                onBack = onBack,
                hasScannedRooms = scannedRooms.isNotEmpty()
            )
        } else {
            // Panorama canvas with gyroscope + drag panning
            val imageBitmap = panoramaBitmap

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Drag to pan: negative drag = move image right
                            panOffsetX -= dragAmount.x
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bmpW = imageBitmap.width.toFloat()
                val bmpH = imageBitmap.height.toFloat()

                // Scale: fit height, tile width
                val scale = canvasHeight / bmpH
                val scaledWidth = bmpW * scale

                // Normalize panOffsetX to [0, scaledWidth) for seamless wrapping
                var offsetX = ((panOffsetX % scaledWidth) + scaledWidth) % scaledWidth

                // Draw enough copies to fill the screen width
                // We need at least ceil(canvasWidth / scaledWidth) + 2 copies
                val copiesNeeded = ((canvasWidth / scaledWidth).toInt()) + 2

                for (i in 0..copiesNeeded) {
                    val drawX = (i * scaledWidth) - offsetX
                    // Only draw if visible
                    if (drawX + scaledWidth > 0 && drawX < canvasWidth) {
                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset(0, 0),
                            srcSize = IntSize(imageBitmap.width, imageBitmap.height),
                            dstOffset = IntOffset(drawX.roundToInt(), 0),
                            dstSize = IntSize(scaledWidth.roundToInt(), canvasHeight.roundToInt())
                        )
                    }
                }
            }

            // Top gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .zIndex(10f)
                    .align(Alignment.TopCenter)
            )

            // Bottom gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .zIndex(10f)
                    .align(Alignment.BottomCenter)
            )

            // 360° Badge (top center)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
                    .zIndex(11f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanAccent))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "360° PANORAMA • INTERACTIVE VIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Back button (top left)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 44.dp, start = 12.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(11f)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Gyro toggle button (top right)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (gyroEnabled) CyanAccent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (gyroEnabled) CyanAccent else Color.White.copy(alpha = 0.3f)),
                modifier = Modifier
                    .padding(top = 44.dp, end = 12.dp)
                    .zIndex(11f)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { gyroEnabled = !gyroEnabled }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (gyroEnabled) CyanAccent else Color.Gray))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (gyroEnabled) "GYRO ON" else "GYRO OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (gyroEnabled) CyanAccent else Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }

            // Room selector tabs (bottom)
            if (scannedRooms.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                        .zIndex(11f)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(scannedRooms) { index, room ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (index == selectedRoomIndex) CyanAccent else Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (index == selectedRoomIndex) CyanAccent else Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.clickable { selectedRoomIndex = index }
                        ) {
                            Text(
                                text = room.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (index == selectedRoomIndex) Color.White else Color.White.copy(alpha = 0.7f),
                                fontWeight = if (index == selectedRoomIndex) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Hint text (bottom)
            Text(
                text = "DRAG OR ROTATE PHONE TO LOOK AROUND",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (scannedRooms.size > 1) 60.dp else 24.dp)
                    .zIndex(11f)
            )
        }
    }
}

@Composable
private fun EmptyPanoramaView(listingTitle: String, onBack: () -> Unit, hasScannedRooms: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DorjaColors.Gray700)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DorjaColors.Gray700),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasScannedRooms) "Panorama Not Available" else "No 3D Scans Available",
                style = MaterialTheme.typography.titleMedium,
                color = DorjaColors.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasScannedRooms) {
                    "This room's panorama was captured before the stitching update. Re-scan to get the full 360° experience."
                } else {
                    "The host hasn't captured a 3D panorama for this listing yet. Check back soon!"
                },
                style = MaterialTheme.typography.bodySmall,
                color = DorjaColors.Sand300,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            DorjaButton(
                text = "Go Back",
                onClick = onBack,
                modifier = Modifier.width(140.dp)
            )
        }
    }
}
