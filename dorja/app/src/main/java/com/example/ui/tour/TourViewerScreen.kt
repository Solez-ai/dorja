package com.example.ui.tour

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.MeetingRoom
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch

@Composable
fun TourViewerScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val roomsState by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())
    val activeRooms = roomsState
    var currentRoomIndex by remember { mutableIntStateOf(0) }

    // Parse panorama frame data from scan
    var panoramaFrameUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val currentRoom = activeRooms.getOrNull(currentRoomIndex) ?: activeRooms.firstOrNull()

    LaunchedEffect(currentRoom?.id) {
        val data = currentRoom?.panoramaData
        if (!data.isNullOrBlank() && data != "{}") {
            try {
                val json = org.json.JSONObject(data)
                val frames = json.getJSONArray("frames")
                val uris = mutableListOf<String>()
                for (i in 0 until frames.length()) {
                    uris.add(frames.getString(i))
                }
                panoramaFrameUris = uris
            } catch (e: Exception) {
                panoramaFrameUris = emptyList()
            }
        } else {
            panoramaFrameUris = emptyList()
        }
    }

    // Transition animations
    val transitionAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val transitionScale = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = rememberCoroutineScope()
    var isTransitioning by remember { mutableStateOf(false) }

    fun switchRoom(newIndex: Int) {
        if (newIndex == currentRoomIndex || isTransitioning || newIndex !in activeRooms.indices) return
        scope.launch {
            isTransitioning = true
            transitionAlpha.animateTo(0.1f, animationSpec = androidx.compose.animation.core.tween(200))
            transitionScale.animateTo(1.08f, animationSpec = androidx.compose.animation.core.tween(200))
            currentRoomIndex = newIndex
            transitionScale.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(300))
            transitionAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(300))
            isTransitioning = false
        }
    }

    // Empty state — no rooms scanned at all
    if (activeRooms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.Ink950)
                .padding(24.dp)
                .testTag("tour_viewer_empty_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(DorjaColors.Gray700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MeetingRoom, null, tint = DorjaColors.Sand300, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("No 3D Rooms Scanned Yet", style = MaterialTheme.typography.titleMedium, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "This property has no 360° panorama scans. Hosts can scan rooms from the property management screen.",
                    style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                DorjaButton(text = "Go Back", onClick = onBack, modifier = Modifier.width(140.dp))
            }
        }
        return
    }

    // Has rooms but no panorama frames for current room
    if (panoramaFrameUris.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.Ink950)
                .testTag("tour_viewer_screen"),
        ) {
            // Room selector strip at top
            Column(modifier = Modifier.fillMaxSize()) {
                // Top nav bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(DorjaColors.Ink950.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = DorjaColors.White)
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DorjaColors.Ink950.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CompassCalibration, null, tint = DorjaColors.Jol600, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "NO PANORAMA DATA",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Sand300,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Message
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(DorjaColors.Gray700),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MeetingRoom, null, tint = DorjaColors.Sand300, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "\"${currentRoom?.displayName ?: "Room"}\" has no panorama scan data.",
                            style = MaterialTheme.typography.bodyMedium, color = DorjaColors.White, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Host: Scan this room from the 3D Scanner to capture a 360° panorama.",
                            style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300
                        )
                    }
                }
            }
        }
        return
    }

    // === REAL 360° PANORAMA VIEWER ===
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950)
            .testTag("tour_viewer_screen")
    ) {
        // Gyroscope auto-pan
        val ctx = LocalContext.current
        val sensorManager = ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
        var gyroYaw by remember { mutableFloatStateOf(0f) }
        DisposableEffect(Unit) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                        val rot = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(rot, event.values)
                        val orient = FloatArray(3)
                        SensorManager.getOrientation(rot, orient)
                        gyroYaw = Math.toDegrees(orient[0].toDouble()).toFloat()
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            sensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
            onDispose { sensorManager?.unregisterListener(listener) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(transitionAlpha.value)
                .scale(transitionScale.value)
        ) {
            val totalWidthDp = (panoramaFrameUris.size * 400f).dp
            val scrollState = rememberScrollState()

            // Auto-pan from gyroscope
            LaunchedEffect(gyroYaw) {
                val scrollTarget = ((gyroYaw / 360f) * (panoramaFrameUris.size * 400f)).toInt()
                if (scrollTarget in 0..scrollState.maxValue) {
                    scrollState.animateScrollTo(scrollTarget)
                }
            }

            // Panoramic strip
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newScroll = (scrollState.value - dragAmount.x * 2).toInt().coerceIn(0, scrollState.maxValue)
                            scope.launch { scrollState.animateScrollTo(newScroll) }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(totalWidthDp)
                        .horizontalScroll(scrollState)
                ) {
                    // Duplicate frames for seamless wrap-around
                    val extendedFrames = panoramaFrameUris + panoramaFrameUris
                    extendedFrames.forEachIndexed { index, uri ->
                        Box(
                            modifier = Modifier.fillMaxHeight().width(400.dp)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Panorama Frame ${index % panoramaFrameUris.size + 1}",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(80.dp)
                        .background(Brush.verticalGradient(listOf(DorjaColors.Ink950.copy(alpha = 0.8f), Color.Transparent)))
                        .align(Alignment.TopCenter)
                )
                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(80.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, DorjaColors.Ink950.copy(alpha = 0.8f))))
                        .align(Alignment.BottomCenter)
                )
            }

            // Top navigation bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(DorjaColors.Ink950.copy(alpha = 0.8f)).testTag("tour_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close Tour", tint = DorjaColors.White)
                }

                // Room selector tabs
                if (activeRooms.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeRooms.forEachIndexed { index, room ->
                            val isSelected = index == currentRoomIndex
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) DorjaColors.Jol600 else DorjaColors.Ink950.copy(alpha = 0.7f),
                                border = if (isSelected) null else BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f)),
                                modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { switchRoom(index) }
                            ) {
                                Text(
                                    text = room.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DorjaColors.Ink950.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CompassCalibration, null, tint = DorjaColors.Jol600, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${currentRoom?.displayName ?: "ROOM"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Panorama badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DorjaColors.Ink950.copy(alpha = 0.75f),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CompassCalibration, null, tint = DorjaColors.Jol600, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "360° REAL PANORAMA • ${panoramaFrameUris.size} FRAMES STITCHED",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Drag hint
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DorjaColors.Ink950.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
            ) {
                Text(
                    "DRAG OR ROTATE PHONE TO LOOK AROUND",
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Sand300,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

