package com.example.ui.tour

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun PanoramaViewerScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val listing by repository.observeListingById(listingId).collectAsState(initial = null)
    val rooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())

    // Filter rooms that have 3D scans
    val scannedRooms = remember(rooms) { rooms.filter { it.has3DScan && it.panoramaData.isNotBlank() } }
    var selectedRoomIndex by remember { mutableIntStateOf(0) }
    val selectedRoom = scannedRooms.getOrNull(selectedRoomIndex)

    // Parse panorama JSON
    val panoramaFrames = remember(selectedRoom) {
        try {
            if (selectedRoom?.panoramaData.isNullOrBlank()) emptyList()
            else {
                val json = JSONObject(selectedRoom!!.panoramaData)
                val frames = json.getJSONArray("frames")
                (0 until frames.length()).map { frames.getString(it) }
            }
        } catch (e: Exception) { emptyList() }
    }

    // Pan state
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    // Gyroscope pan
    var gyroYaw by remember { mutableFloatStateOf(0f) }
    var gyroEnabled by remember { mutableStateOf(true) }
    var lastGyroTime by remember { mutableStateOf(0L) }

    // Sensor
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
    LaunchedEffect(gyroYaw, gyroEnabled, panoramaFrames.size) {
        if (!gyroEnabled || panoramaFrames.isEmpty()) return@LaunchedEffect
        // Map yaw to scroll position
        // Each frame covers ~30 degrees (360/12), duplicated frames for wrap
        val degreesPerFrame = 360f / panoramaFrames.size.coerceAtLeast(1)
        val frameWidthPx = 1080f // approximate width per frame in pixels
        val totalWidth = panoramaFrames.size * frameWidthPx
        // Convert yaw to pixel offset
        val targetOffset = ((gyroYaw / 360f) * totalWidth) % totalWidth
        scrollOffset = targetOffset
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (panoramaFrames.isEmpty()) {
            // No frames
            EmptyPanoramaView(listingTitle = listing?.title ?: "Listing", onBack = onBack)
        } else {
            // Panorama strip
            Box(modifier = Modifier.fillMaxSize()) {
                // Horizontal scrollable panorama
                val duplicatedFrames = panoramaFrames + panoramaFrames // duplicate for seamless wrap

                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                scrollOffset -= dragAmount.x
                            }
                        },
                    userScrollEnabled = !gyroEnabled
                ) {
                    items(duplicatedFrames.size) { index ->
                        val framePath = duplicatedFrames[index]
                        Box(
                            modifier = Modifier
                                .width(1080.dp)
                                .height(800.dp)
                        ) {
                            AsyncImage(
                                model = framePath,
                                contentDescription = "Panorama frame ${(index % panoramaFrames.size) + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Gyro-driven scroll override
                if (gyroEnabled) {
                    LaunchedEffect(scrollOffset) {
                        // Convert pixel offset to approximate scroll index
                        val frameWidth = 1080
                        val targetIndex = ((scrollOffset / frameWidth).roundToInt()).coerceIn(0, duplicatedFrames.size - 1)
                        if (targetIndex in 0 until duplicatedFrames.size) {
                            listState.animateScrollToItem(targetIndex)
                        }
                    }
                }

                // Top gradient overlay (depth effect)
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

                // Bottom gradient overlay (depth effect)
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
                            text = "360° REAL PANORAMA • ${panoramaFrames.size} FRAMES STITCHED",
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
                        .graphicsLayer { }
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
                                modifier = Modifier.graphicsLayer { }
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
            }
        }
    }
}

@Composable
private fun EmptyPanoramaView(listingTitle: String, onBack: () -> Unit) {
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
                text = "No 3D Scans Available",
                style = MaterialTheme.typography.titleMedium,
                color = DorjaColors.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "The host hasn't captured a 3D panorama for this listing yet. Check back soon!",
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

private val CyanAccent = Color(0xFF00BCD4)
