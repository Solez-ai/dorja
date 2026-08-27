package com.example.ui.tour

import android.opengl.GLSurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.gl.Room3DRenderer
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun TourViewerScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val roomsState by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())
    val listingState by repository.observeListingById(listingId).collectAsState(initial = null)

    val activeRooms = roomsState
    var currentRoomIndex by remember { mutableIntStateOf(0) }

    if (activeRooms.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.Ink950)
                .padding(24.dp)
                .testTag("tour_viewer_empty_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.Gray700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = DorjaColors.Sand300,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No 3D Rooms Scanned Yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This property does not have any 360° reality scanned rooms yet. Hosts can add rooms and capture 3D multi-axis panorama scans.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Sand300,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                DorjaButton(
                    text = "Go Back",
                    onClick = onBack,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
        return
    }

    val currentRoom = activeRooms.getOrNull(currentRoomIndex) ?: activeRooms.first()

    // Camera angles
    var panAngle by remember { mutableFloatStateOf(0f) }
    var tiltAngle by remember { mutableFloatStateOf(0f) }

    // Joystick thumb offsets
    var joystickOffsetX by remember { mutableFloatStateOf(0f) }
    var joystickOffsetY by remember { mutableFloatStateOf(0f) }

    // Transition animatables
    val transitionAlpha = remember { Animatable(1f) }
    val transitionScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var isTransitioning by remember { mutableStateOf(false) }

    // Orientation recommendation banner
    var showOrientationBanner by remember { mutableStateOf(true) }

    // Panorama frame data from scan
    var panoramaFrameUris by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(currentRoom.id) {
        val data = currentRoom.panoramaData
        if (data.isNotBlank() && data != "{}") {
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
    var rendererRef by remember { mutableStateOf<Room3DRenderer?>(null) }
    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }

    fun switchRoom(newIndex: Int) {
        if (newIndex == currentRoomIndex || isTransitioning || newIndex !in activeRooms.indices) return
        scope.launch {
            isTransitioning = true
            transitionAlpha.animateTo(0.1f, animationSpec = tween(200))
            transitionScale.animateTo(1.08f, animationSpec = tween(200))
            currentRoomIndex = newIndex
            rendererRef?.roomType = activeRooms[newIndex].roomType
            rendererRef?.panAngle = 0f
            rendererRef?.tiltAngle = 0f
            panAngle = 0f
            tiltAngle = 0f
            glSurfaceViewRef?.requestRender()
            transitionScale.animateTo(1f, animationSpec = tween(300))
            transitionAlpha.animateTo(1f, animationSpec = tween(300))
            isTransitioning = false
        }
    }

    LaunchedEffect(currentRoom) {
        rendererRef?.roomType = currentRoom.roomType
        glSurfaceViewRef?.requestRender()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950)
            .testTag("tour_viewer_screen")
    ) {
        // 1. Panorama Viewer — shows real captured camera frames as cylindrical 360° panorama
        if (panoramaFrameUris.isNotEmpty()) {
            // REAL 360° PANORAMA VIEWER
            // Gyroscope auto-pan
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val sensorManager = ctx.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            var gyroYaw by remember { mutableFloatStateOf(0f) }
            DisposableEffect(Unit) {
                val listener = object : android.hardware.SensorEventListener {
                    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                        if (event?.sensor?.type == android.hardware.Sensor.TYPE_ROTATION_VECTOR) {
                            val rot = FloatArray(9)
                            android.hardware.SensorManager.getRotationMatrixFromVector(rot, event.values)
                            val orient = FloatArray(3)
                            android.hardware.SensorManager.getOrientation(rot, orient)
                            gyroYaw = Math.toDegrees(orient[0].toDouble()).toFloat()
                        }
                    }
                    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
                }
                val sensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
                sensor?.let { sensorManager.registerListener(listener, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
                onDispose { sensorManager?.unregisterListener(listener) }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(transitionAlpha.value)
                    .scale(transitionScale.value)
            ) {
                // Cylindrical panorama: each frame fills screen height, total width = frameCount * screenWidth
                // The pan offset shifts the strip, and frames wrap around for seamless 360°
                val totalWidthDp = (panoramaFrameUris.size * 400f).dp
                val scrollState = rememberScrollState()

                // Auto-pan based on gyroscope
                LaunchedEffect(gyroYaw) {
                    val scrollTarget = ((gyroYaw / 360f) * (panoramaFrameUris.size * 400f)).toInt()
                    scrollState.animateScrollTo(scrollTarget.coerceAtLeast(0))
                }

                // Drag-to-pan overrides gyro
                var dragAccum by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                dragAccum += dragAmount.x
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
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(400.dp)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Panorama Frame ${index % panoramaFrameUris.size + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Top gradient for text contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(DorjaColors.Ink950.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .align(Alignment.TopCenter)
                    )
                    // Bottom gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, DorjaColors.Ink950.copy(alpha = 0.8f))
                                )
                            )
                            .align(Alignment.BottomCenter)
                    )
                }

                // Panorama badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DorjaColors.Ink950.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompassCalibration,
                            contentDescription = null,
                            tint = DorjaColors.Jol600,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "360° REAL PANORAMA \u2022 ${panoramaFrameUris.size} FRAMES STITCHED",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                // Drag hint
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DorjaColors.Ink950.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                ) {
                    Text(
                        text = "DRAG OR ROTATE PHONE TO LOOK AROUND",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Sand300,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // FALLBACK: OpenGL ES 3D renderer (no panorama data)
            AndroidView(
                factory = { context ->
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(2)
                        val renderer = Room3DRenderer(context).also {
                            it.roomType = currentRoom.roomType
                        }
                        rendererRef = renderer
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        glSurfaceViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(transitionAlpha.value)
                    .scale(transitionScale.value)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panAngle += dragAmount.x * 0.35f
                            tiltAngle = (tiltAngle - dragAmount.y * 0.25f).coerceIn(-35f, 35f)
                            rendererRef?.panAngle = panAngle
                            rendererRef?.tiltAngle = tiltAngle
                        }
                    }
            )
        }

        // 2. Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DorjaColors.Ink950.copy(alpha = 0.8f))
                    .testTag("tour_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close Tour",
                    tint = DorjaColors.White
                )
            }

            // Room Title Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DorjaColors.Ink950.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = DorjaColors.Jol600,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ROOM ${currentRoomIndex + 1}/${activeRooms.size}: ${currentRoom.displayName.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Reality Passport Badge
            DorjaBadge(
                text = "PASSPORT VERIFIED",
                backgroundColor = DorjaColors.Jol600,
                textColor = DorjaColors.White
            )
        }

        // 3. Orientation Recommendation Banner
        AnimatedVisibility(
            visible = showOrientationBanner,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 104.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DorjaColors.Ink950.copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DorjaColors.Jol600),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = DorjaColors.Jol600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Rotate to Landscape",
                                style = MaterialTheme.typography.labelMedium,
                                color = DorjaColors.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "For the best 3D viewing experience",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Sand300,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { showOrientationBanner = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = DorjaColors.Sand300, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 4. Compass / Orientation Indicator (Top Right Below Bar)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DorjaColors.Ink950.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 156.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = DorjaColors.Sand300,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${((panAngle % 360 + 360) % 360).toInt()}° PAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Sand300
                )
            }
        }

        // 4. Interactive Doorway Hotspot (Walk to Next Room)
        val nextRoomIndex = (currentRoomIndex + 1) % activeRooms.size
        val nextRoom = activeRooms[nextRoomIndex]
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DorjaColors.Jol600.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DorjaColors.White),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { switchRoom(nextRoomIndex) }
                    .testTag("doorway_hotspot_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = DorjaColors.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "WALK TO",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Teal100
                        )
                        Text(
                            text = nextRoom.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = DorjaColors.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = DorjaColors.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 5. Bottom Controls (Virtual Joystick + Room Switcher Tabs)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            // Joystick + Guidance Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Virtual Joystick (120dp circle)
                val joystickMaxRadius = 45f
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.Ink950.copy(alpha = 0.85f))
                        .border(1.5.dp, DorjaColors.Sand300.copy(alpha = 0.6f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    joystickOffsetX = 0f
                                    joystickOffsetY = 0f
                                },
                                onDragCancel = {
                                    joystickOffsetX = 0f
                                    joystickOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = joystickOffsetX + dragAmount.x
                                    val newY = joystickOffsetY + dragAmount.y
                                    val distance = sqrt((newX * newX + newY * newY).toDouble()).toFloat()
                                    if (distance <= joystickMaxRadius) {
                                        joystickOffsetX = newX
                                        joystickOffsetY = newY
                                    } else {
                                        val factor = joystickMaxRadius / distance
                                        joystickOffsetX = newX * factor
                                        joystickOffsetY = newY * factor
                                    }

                                    // Rotate camera proportionally
                                    panAngle += (joystickOffsetX / joystickMaxRadius) * 2.5f
                                    tiltAngle = (tiltAngle - (joystickOffsetY / joystickMaxRadius) * 1.5f).coerceIn(-35f, 35f)
                                    rendererRef?.panAngle = panAngle
                                    rendererRef?.tiltAngle = tiltAngle
                                }
                            )
                        }
                        .testTag("virtual_joystick"),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer guide ring
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .border(1.dp, DorjaColors.Sand300.copy(alpha = 0.2f), CircleShape)
                    )

                    // Draggable Knob
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(joystickOffsetX.roundToInt(), joystickOffsetY.roundToInt()) }
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DorjaColors.Jol600)
                            .border(1.5.dp, DorjaColors.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOOK",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Reality Passport Spec Indicator
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DorjaColors.Ink950.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "60 FPS 3D ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Success,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drag joystick or swipe screen to pan",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Sand300,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Room Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activeRooms.forEachIndexed { index, room ->
                    val isSelected = index == currentRoomIndex
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) DorjaColors.Jol600 else DorjaColors.Ink950.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) DorjaColors.White else DorjaColors.Sand300.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { switchRoom(index) }
                            .testTag("tour_room_tab_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = if (isSelected) DorjaColors.White else DorjaColors.Sand300,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = room.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) DorjaColors.White else DorjaColors.Sand300,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
