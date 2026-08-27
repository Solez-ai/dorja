package com.example.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.DorjaApp
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private enum class ScannerPhase {
    ROOM_SELECT,
    PRE_CAPTURE,
    SCANNING,
    RESULT
}

@Composable
fun RoomScannerScreen(
    initialListingId: String? = null,
    onBack: () -> Unit,
    onScanSaved: (String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasCameraPermission = isGranted }

    // Listing & rooms
    val listingsState by repository.getAllListings().collectAsState(initial = emptyList())
    var currentListingId by remember(initialListingId, listingsState) {
        mutableStateOf(initialListingId ?: listingsState.firstOrNull()?.id ?: "l1")
    }
    val activeListing = listingsState.find { it.id == currentListingId } ?: listingsState.firstOrNull()
    val roomsState by repository.getRoomsByListing(currentListingId).collectAsState(initial = emptyList())

    // Phase
    var phase by remember { mutableStateOf(ScannerPhase.ROOM_SELECT) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    val activeRoom = roomsState.find { it.id == selectedRoomId }

    // Scanner state
    var scanProgress by remember { mutableIntStateOf(0) }
    val totalTargets = 12
    var isAligned by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomDimensions by remember { mutableStateOf("14 x 12 ft") }

    // Real gyroscope
    var realYaw by remember { mutableFloatStateOf(0f) }
    var realPitch by remember { mutableFloatStateOf(0f) }
    val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager
    val gyroListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rot = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rot, event.values)
                    val orient = FloatArray(3)
                    SensorManager.getOrientation(rot, orient)
                    realYaw = Math.toDegrees(orient[0].toDouble()).toFloat()
                    realPitch = Math.toDegrees(orient[1].toDouble()).toFloat()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    // Target angles for 12 scan points: 8 horizontal + 2 ceiling + 2 floor
    val targetAngles = remember {
        listOf(
            0f, 30f, 60f, 90f, 120f, 150f, 180f, 210f, 240f, 270f, 300f, 330f
        )
    }

    // Help dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = DorjaColors.Jol600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3D Panorama Scan Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Hold your phone upright at chest height.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                    Text("2. Press the green shutter to start scanning.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                    Text("3. Slowly rotate 360° — the scanner auto-captures at each angle.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                    Text("4. Press Stop when all 12 angles are captured.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                }
            },
            confirmButton = {
                DorjaButton(text = "Got it", onClick = { showHelpDialog = false }, modifier = Modifier.width(100.dp))
            }
        )
    }

    // Add Room Dialog
    if (showAddRoomDialog) {
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text("Add Room", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Name") },
                        placeholder = { Text("e.g. Master Bedroom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.Sand300
                        )
                    )
                    OutlinedTextField(
                        value = newRoomDimensions,
                        onValueChange = { newRoomDimensions = it },
                        label = { Text("Dimensions") },
                        placeholder = { Text("e.g. 16 x 14 ft") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.Sand300
                        )
                    )
                }
            },
            confirmButton = {
                DorjaButton(text = "Add", onClick = {
                    if (newRoomName.isNotBlank()) {
                        scope.launch {
                            repository.addRoom(
                                listingId = currentListingId,
                                roomType = "BEDROOM",
                                displayName = newRoomName,
                                dimensions = newRoomDimensions,
                                description = ""
                            )
                            newRoomName = ""
                            showAddRoomDialog = false
                        }
                    }
                }, modifier = Modifier.width(100.dp))
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) { Text("Cancel", color = DorjaColors.Gray700) }
            }
        )
    }

    // Register/unregister gyroscope
    DisposableEffect(phase) {
        if (phase == ScannerPhase.SCANNING) {
            val rotSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            rotSensor?.let { sensorManager.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_UI) }
        }
        onDispose { sensorManager?.unregisterListener(gyroListener) }
    }

    // Auto-capture logic during scanning
    LaunchedEffect(phase, scanProgress) {
        if (phase != ScannerPhase.SCANNING || scanProgress >= totalTargets) return@LaunchedEffect

        val targetYaw = targetAngles[scanProgress]
        val tolerance = 20f
        var alignedSince = 0L

        while (scanProgress < totalTargets) {
            delay(80)
            val yawDiff = abs(normalizeAngle(realYaw) - normalizeAngle(targetYaw))
            val yawAligned = yawDiff < tolerance || yawDiff > (360f - tolerance)

            if (yawAligned) {
                if (alignedSince == 0L) alignedSince = System.currentTimeMillis()
                isAligned = true
                if (System.currentTimeMillis() - alignedSince > 350) {
                    scanProgress++
                    alignedSince = 0L
                    isAligned = false
                    delay(200)
                }
            } else {
                alignedSince = 0L
                isAligned = false
            }
        }
        // All angles captured
        delay(300)
        phase = ScannerPhase.RESULT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950)
            .testTag("room_scanner_screen")
    ) {
        when (phase) {
            ScannerPhase.ROOM_SELECT -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(DorjaColors.Gray700)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = DorjaColors.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("3D House Scanner", style = MaterialTheme.typography.titleLarge, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                                    Text("Room-by-Room Panorama Builder", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300)
                                }
                            }
                            IconButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(DorjaColors.Gray700)
                            ) {
                                Icon(Icons.Default.HelpOutline, "Help", tint = DorjaColors.Sand300)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Listing card
                        if (activeListing != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp), color = DorjaColors.Gray700,
                                border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(DorjaColors.Jol600.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Home, null, tint = DorjaColors.Jol600, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(activeListing.title, style = MaterialTheme.typography.titleSmall, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                                        Text("${activeListing.publicArea} • ${roomsState.size} Rooms", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Room list header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("SELECT ROOM TO SCAN", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Sand300, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showAddRoomDialog = true }) {
                                Icon(Icons.Default.Add, null, tint = DorjaColors.Jol600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Room", color = DorjaColors.Jol600, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Room list
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f, fill = false)) {
                            if (roomsState.isEmpty()) {
                                item {
                                    Surface(shape = RoundedCornerShape(12.dp), color = DorjaColors.Gray700.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("No rooms configured.", color = DorjaColors.Sand300)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            DorjaButton(text = "+ Add First Room", onClick = { showAddRoomDialog = true })
                                        }
                                    }
                                }
                            } else {
                                items(roomsState) { room ->
                                    val isSelected = selectedRoomId == room.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) DorjaColors.Gray700 else DorjaColors.Ink950,
                                        border = BorderStroke(1.5.dp, if (isSelected) DorjaColors.Jol600 else DorjaColors.Sand300.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedRoomId = room.id }
                                            .testTag("room_scan_item_${room.id}")
                                    ) {
                                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                                    .background(if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.2f) else DorjaColors.Gray700),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom, null,
                                                    tint = if (room.has3DScan) DorjaColors.Success else DorjaColors.Sand300,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(room.displayName, style = MaterialTheme.typography.titleSmall, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                                                    if (room.dimensions.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("• ${room.dimensions}", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300, fontSize = 11.sp)
                                                    }
                                                }
                                                Text(
                                                    if (room.has3DScan) "Panorama Scanned ✓" else "Ready to scan",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (room.has3DScan) DorjaColors.Success else DorjaColors.Sand300,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            DorjaBadge(
                                                text = if (room.has3DScan) "DONE" else "SCAN",
                                                backgroundColor = if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.2f) else DorjaColors.Jol600.copy(alpha = 0.2f),
                                                textColor = if (room.has3DScan) DorjaColors.Success else DorjaColors.Jol600
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom CTA
                    DorjaButton(
                        text = if (activeRoom?.has3DScan == true) "Re-Scan ${activeRoom.displayName}" else "Scan 3D: ${activeRoom?.displayName ?: "Select a room"}",
                        onClick = {
                            if (activeRoom != null) {
                                selectedRoomId = activeRoom.id
                                scanProgress = 0
                                isAligned = false
                                phase = ScannerPhase.PRE_CAPTURE
                                if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        icon = Icons.Default.ViewInAr,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "launch_scanner_button"
                    )
                }
            }

            ScannerPhase.PRE_CAPTURE -> {
                // Full-screen camera + instruction overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    // Camera preview
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val pv = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    try {
                                        val cp = future.get()
                                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                                        cp.unbindAll()
                                        cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }, ContextCompat.getMainExecutor(ctx))
                                pv
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Cylindrical dot-grid overlay
                    CylindricalOverlay()

                    // Top bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { phase = ScannerPhase.ROOM_SELECT }, modifier = Modifier.size(40.dp).clip(CircleShape).background(DorjaColors.Ink950.copy(alpha = 0.7f))) {
                            Icon(Icons.Default.Close, "Close", tint = DorjaColors.White)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = DorjaColors.Ink950.copy(alpha = 0.7f), border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f))) {
                            Text(activeRoom?.displayName ?: "ROOM", color = DorjaColors.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                        IconButton(onClick = { showHelpDialog = true }, modifier = Modifier.size(40.dp).clip(CircleShape).background(DorjaColors.Ink950.copy(alpha = 0.7f))) {
                            Icon(Icons.Default.HelpOutline, "Help", tint = DorjaColors.White)
                        }
                    }

                    // Center instruction card
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Phone icon with checkmark badge
                        Box(
                            modifier = Modifier.size(88.dp).clip(CircleShape)
                                .background(DorjaColors.Jol600.copy(alpha = 0.15f)).border(2.dp, DorjaColors.Jol600, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Smartphone, null, tint = DorjaColors.Jol600, modifier = Modifier.size(48.dp))
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape)
                                    .background(DorjaColors.Success).border(2.dp, DorjaColors.Ink950, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, "Ready", tint = DorjaColors.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Hold your phone upright in portrait mode", style = MaterialTheme.typography.titleMedium, color = DorjaColors.White, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stand in the center of ${activeRoom?.displayName ?: "the room"} and rotate slowly.", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))

                        // "PRESS TO START" + green shutter
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PRESS TO START", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = DorjaColors.Jol600, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape)
                                    .background(DorjaColors.White.copy(alpha = 0.2f)).border(3.dp, DorjaColors.White, CircleShape)
                                    .clickable {
                                        scanProgress = 0
                                        isAligned = false
                                        phase = ScannerPhase.SCANNING
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(DorjaColors.Jol600))
                            }
                        }
                    }

                    // Bottom bar: gallery left, gyro right
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(12.dp), color = DorjaColors.Ink950.copy(alpha = 0.7f), border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f))) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, "Layers", tint = DorjaColors.Sand300, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LAYERS", color = DorjaColors.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = DorjaColors.Ink950.copy(alpha = 0.7f), border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f))) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Explore, null, tint = DorjaColors.Success, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GYRO ON", color = DorjaColors.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            ScannerPhase.SCANNING -> {
                // Full-screen active panorama scanner
                Box(modifier = Modifier.fillMaxSize()) {
                    // Camera preview
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val pv = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    try {
                                        val cp = future.get()
                                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                                        cp.unbindAll()
                                        cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }, ContextCompat.getMainExecutor(ctx))
                                pv
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Gyro-driven reticle canvas
                    GyroReticleCanvas(realYaw, realPitch, isAligned, scanProgress, totalTargets)

                    // Top bar with progress
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 16.dp, end = 16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp), color = DorjaColors.Ink950.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text((activeRoom?.displayName ?: "ROOM").uppercase(), style = MaterialTheme.typography.labelMedium, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                                    Text("Angle ${scanProgress + 1} of $totalTargets", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Jol600, fontFamily = FontFamily.Monospace)
                                }
                                Text("$scanProgress/$totalTargets", style = MaterialTheme.typography.titleMedium, color = DorjaColors.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    // Alignment status banner
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = DorjaColors.Ink950.copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, if (isAligned) DorjaColors.Success else DorjaColors.Jol600),
                        modifier = Modifier.align(Alignment.Center).offset(y = (-120).dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isAligned) "ALIGNED — CAPTURING" else "Rotate to angle ${scanProgress + 1} of $totalTargets",
                                style = MaterialTheme.typography.labelLarge, color = DorjaColors.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Bottom: progress strip + stop button
                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress strip
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = DorjaColors.Ink950.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("SCAN PROGRESS", color = DorjaColors.Sand300, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("${((scanProgress.toFloat() / totalTargets) * 100).toInt()}%", color = DorjaColors.Jol600, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    repeat(totalTargets) { index ->
                                        Box(
                                            modifier = Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    when {
                                                        index < scanProgress -> DorjaColors.Success
                                                        index == scanProgress -> DorjaColors.Jol600
                                                        else -> DorjaColors.Gray700
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // Stop button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { phase = ScannerPhase.RESULT },
                                modifier = Modifier.size(68.dp).clip(CircleShape).background(DorjaColors.Error).testTag("stop_scan_button")
                            ) {
                                Icon(Icons.Default.Stop, "Stop", tint = DorjaColors.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("PRESS STOP WHEN DONE", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Sand300, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            ScannerPhase.RESULT -> {
                // Scan complete - save result
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = DorjaColors.Success, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("3D Scan Complete", style = MaterialTheme.typography.titleLarge, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                                    Text(activeRoom?.displayName ?: "Room", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Sand300)
                                }
                            }
                            DorjaBadge(text = "VERIFIED", backgroundColor = DorjaColors.Success.copy(alpha = 0.2f), textColor = DorjaColors.Success)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Scan summary
                        Surface(shape = RoundedCornerShape(16.dp), color = DorjaColors.Gray700, border = BorderStroke(1.5.dp, DorjaColors.Jol600), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("PANORAMA SCAN SUMMARY", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Sand300, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column { Text("COVERAGE", color = DorjaColors.Sand300, fontSize = 10.sp); Text("360° Full", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                                    Column { Text("ANGLES", color = DorjaColors.Sand300, fontSize = 10.sp); Text("$scanProgress Captured", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                                    Column { Text("ROOM", color = DorjaColors.Sand300, fontSize = 10.sp); Text(activeRoom?.displayName ?: "N/A", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                                }
                            }
                        }
                    }

                    // Action buttons
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DorjaButton(
                            text = "Save 3D Scan to ${activeRoom?.displayName ?: "Room"}",
                            onClick = {
                                scope.launch {
                                    if (activeRoom != null) {
                                        repository.updateRoom3DScan(activeRoom.id, "panorama_${System.currentTimeMillis()}")
                                    }
                                    withContext(Dispatchers.Main) { onScanSaved(activeRoom?.id ?: "") }
                                }
                            },
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "save_scan_button"
                        )

                        val unScanned = roomsState.filter { !it.has3DScan && it.id != selectedRoomId }
                        if (unScanned.isNotEmpty()) {
                            DorjaOutlinedButton(
                                text = "Scan Next: ${unScanned.first().displayName}",
                                onClick = {
                                    selectedRoomId = unScanned.first().id
                                    scanProgress = 0
                                    isAligned = false
                                    phase = ScannerPhase.PRE_CAPTURE
                                    if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                icon = Icons.Default.ViewInAr,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        DorjaOutlinedButton(
                            text = "Back to Room List",
                            onClick = { phase = ScannerPhase.ROOM_SELECT },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CylindricalOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dotColor = Color(0xFF00BCD4).copy(alpha = 0.55f)
        val r = 2.dp.toPx()

        // Top border — curved inward
        for (i in 0..24) {
            val frac = i / 24f
            val x = w * frac
            val curve = 18f * (1f - 4f * (frac - 0.5f) * (frac - 0.5f))
            drawCircle(dotColor, r, Offset(x, 8.dp.toPx() + curve.coerceAtLeast(0f)))
        }
        // Bottom border
        for (i in 0..24) {
            val frac = i / 24f
            val x = w * frac
            val curve = 18f * (1f - 4f * (frac - 0.5f) * (frac - 0.5f))
            drawCircle(dotColor, r, Offset(x, h - 8.dp.toPx() - curve.coerceAtLeast(0f)))
        }
        // Left edge
        for (i in 0..14) {
            val frac = i / 14f
            val y = h * frac
            val curve = 12f * (1f - 4f * (frac - 0.5f) * (frac - 0.5f))
            drawCircle(dotColor, r, Offset(8.dp.toPx() + curve.coerceAtLeast(0f), y))
        }
        // Right edge
        for (i in 0..14) {
            val frac = i / 14f
            val y = h * frac
            val curve = 12f * (1f - 4f * (frac - 0.5f) * (frac - 0.5f))
            drawCircle(dotColor, r, Offset(w - 8.dp.toPx() - curve.coerceAtLeast(0f), y))
        }
    }
}

@Composable
private fun GyroReticleCanvas(
    yaw: Float,
    pitch: Float,
    isAligned: Boolean,
    progress: Int,
    total: Int
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(1f, 1.3f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = 100.dp.toPx()

        // Feature point cloud
        val rng = java.util.Random(42)
        for (i in 0..50) {
            drawCircle(Color.White.copy(alpha = 0.3f), 2f, Offset(rng.nextFloat() * w, rng.nextFloat() * h))
        }

        // Compass ring
        drawCircle(Color(0xFF007C78).copy(alpha = 0.3f), radius, Offset(cx, cy), style = Stroke(1.5.dp.toPx(), PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))

        // Progress nodes around ring
        for (i in 0 until total) {
            val angle = (360f / total) * i
            val rad = Math.toRadians(angle.toDouble() - 90.0)
            val nx = cx + (radius * cos(rad)).toFloat()
            val ny = cy + (radius * sin(rad)).toFloat()
            val captured = i < progress
            val current = i == progress

            drawCircle(
                when {
                    captured -> Color(0xFF2E7D32)
                    current -> Color(0xFF007C78)
                    else -> Color.White.copy(alpha = 0.4f)
                },
                if (current) 10.dp.toPx() * pulse else 6.dp.toPx(),
                Offset(nx, ny)
            )
            drawCircle(Color.White, if (current) 4.dp.toPx() else 2.5.dp.toPx(), Offset(nx, ny))
        }

        // Connecting lines between captured nodes
        for (i in 0 until total - 1) {
            if (i < progress && i + 1 <= progress) {
                val a1 = Math.toRadians(((360f / total) * i).toDouble() - 90.0)
                val a2 = Math.toRadians(((360f / total) * (i + 1)).toDouble() - 90.0)
                drawLine(
                    Color(0xFF2E7D32).copy(alpha = 0.7f),
                    Offset(cx + (radius * cos(a1)).toFloat(), cy + (radius * sin(a1)).toFloat()),
                    Offset(cx + (radius * cos(a2)).toFloat(), cy + (radius * sin(a2)).toFloat()),
                    2.dp.toPx()
                )
            }
        }

        // Center reticle
        drawCircle(if (isAligned) Color(0xFF2E7D32) else Color.White.copy(alpha = 0.85f), 28.dp.toPx(), Offset(cx, cy), style = Stroke(2.dp.toPx()))
        drawLine(if (isAligned) Color(0xFF2E7D32) else Color(0xFF007C78), Offset(cx, cy - 16.dp.toPx()), Offset(cx, cy + 16.dp.toPx()), 1.5.dp.toPx())
        drawLine(if (isAligned) Color(0xFF2E7D32) else Color(0xFF007C78), Offset(cx - 16.dp.toPx(), cy), Offset(cx + 16.dp.toPx(), cy), 1.5.dp.toPx())

        // Gyro-driven crosshair offset
        val gyroOffsetX = (yaw / 90f * 60f).dp.toPx()
        val gyroOffsetY = (pitch / 45f * 40f).dp.toPx()
        drawCircle(Color(0xFF007C78).copy(alpha = 0.5f), 16.dp.toPx() * pulse, Offset(cx + gyroOffsetX, cy + gyroOffsetY))
        drawCircle(Color.White, 6.dp.toPx(), Offset(cx + gyroOffsetX, cy + gyroOffsetY))

        // Guide line from center to current target
        if (progress < total) {
            val targetAngle = Math.toRadians(((360f / total) * progress).toDouble() - 90.0)
            drawLine(
                Color(0xFF007C78),
                Offset(cx, cy),
                Offset(cx + (radius * cos(targetAngle)).toFloat(), cy + (radius * sin(targetAngle)).toFloat()),
                2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }
    }
}

private fun normalizeAngle(angle: Float): Float {
    var a = angle % 360f
    if (a < 0f) a += 360f
    return a
}
