package com.example.ui.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.Listing
import com.example.data.model.RoomItem
import com.example.ui.components.BentoCard
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaInput
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class PanoramaScanPhase {
    HOUSE_ROOM_SELECT,
    ORIENTATION_PROMPT,
    ACTIVE_PANORAMA,
    STITCHING_PROCESSING,
    RESULT_PASSPORT
}

data class PanoramaSliceTarget(
    val id: Int,
    val name: String,
    val directionText: String,
    val angleYaw: Float,
    val anglePitch: Float,
    val axisType: String // "SIDEWAYS", "CORNER", "UP_CEILING", "DOWN_FLOOR"
)

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

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val listingsState by repository.getAllListings().collectAsState(initial = emptyList())
    var currentListingId by remember(initialListingId, listingsState) {
        mutableStateOf(initialListingId ?: listingsState.firstOrNull()?.id ?: "l1")
    }

    val activeListing = listingsState.find { it.id == currentListingId } ?: listingsState.firstOrNull()
    val roomsState by repository.getRoomsByListing(currentListingId).collectAsState(initial = emptyList())

    // Currently targeted room in the house
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    val activeRoom = roomsState.find { it.id == selectedRoomId } ?: roomsState.firstOrNull()

    var scanPhase by remember { mutableStateOf(PanoramaScanPhase.HOUSE_ROOM_SELECT) }
    var hdModeEnabled by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    // New Room input states
    var newRoomName by remember { mutableStateOf("") }
    var newRoomType by remember { mutableStateOf("BEDROOM") }
    var newRoomDimensions by remember { mutableStateOf("14 x 12 ft") }

    // Panorama Slices Targets (Multi-Axis with sideways, corners, up/ceiling, down/floor)
    val panoramaTargets = remember {
        listOf(
            PanoramaSliceTarget(0, "Center Front", "Align Front 0°", 0f, 0f, "SIDEWAYS"),
            PanoramaSliceTarget(1, "Front-Right Corner", "Turn Right 45° ↗", 45f, 0f, "CORNER"),
            PanoramaSliceTarget(2, "Right Wall", "Pan Right 90° ►", 90f, 0f, "SIDEWAYS"),
            PanoramaSliceTarget(3, "Rear-Right Corner", "Turn Right 135° ↘", 135f, 0f, "CORNER"),
            PanoramaSliceTarget(4, "Rear Wall", "Turn 180° Back", 180f, 0f, "SIDEWAYS"),
            PanoramaSliceTarget(5, "Rear-Left Corner", "Turn Left 225° ↙", 225f, 0f, "CORNER"),
            PanoramaSliceTarget(6, "Left Wall", "Pan Left 270° ◄", 270f, 0f, "SIDEWAYS"),
            PanoramaSliceTarget(7, "Front-Left Corner", "Turn Left 315° ↖", 315f, 0f, "CORNER"),
            PanoramaSliceTarget(8, "Ceiling Zenith", "Tilt Up Ceiling +45° ▲", 0f, -45f, "UP_CEILING"),
            PanoramaSliceTarget(9, "Ceiling Right Corner", "Tilt Up Corner +45° ↗", 90f, -45f, "UP_CEILING"),
            PanoramaSliceTarget(10, "Floor Nadir", "Tilt Down Floor -45° ▼", 0f, 45f, "DOWN_FLOOR"),
            PanoramaSliceTarget(11, "Floor Left Corner", "Tilt Down Corner -45° ↙", 270f, 45f, "DOWN_FLOOR")
        )
    }

    var currentTargetIndex by remember { mutableIntStateOf(0) }
    val capturedSlices = remember { mutableStateListOf<Int>() }

    // Sensor / Targeting Simulation
    var simulatedYaw by remember { mutableFloatStateOf(0f) }
    var simulatedPitch by remember { mutableFloatStateOf(0f) }
    var isReticleAligned by remember { mutableStateOf(false) }

    // Live Metrics
    var pointCloudPoints by remember { mutableIntStateOf(0) }
    var planesDetected by remember { mutableIntStateOf(0) }
    var scanDurationMs by remember { mutableFloatStateOf(0f) }

    // Help Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = DorjaColors.Jol600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3D Panorama Scanning Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Hold your phone upright in portrait mode at chest height.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                    Text("2. Turn slowly in a circle (360°) to capture sideways and corner angles.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                    Text("3. Tilt up to capture ceiling corners and tilt down for floor surfaces.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                    Text("4. Align the center circle over the cyan target point until it clicks.", style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Gray700)
                }
            },
            confirmButton = {
                DorjaButton(text = "Got it", onClick = { showHelpDialog = false }, modifier = Modifier.width(100.dp))
            }
        )
    }

    // Add Room to House Dialog
    if (showAddRoomDialog) {
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = {
                Text("Add Room to House", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Display Name") },
                        placeholder = { Text("e.g. Master Bedroom, Dining Lounge") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
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
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Add Room",
                    onClick = {
                        if (newRoomName.isNotBlank()) {
                            scope.launch {
                                repository.addRoom(
                                    listingId = currentListingId,
                                    roomType = newRoomType,
                                    displayName = newRoomName,
                                    dimensions = newRoomDimensions,
                                    description = "Newly added house room ready for 3D reality scan."
                                )
                                newRoomName = ""
                                showAddRoomDialog = false
                            }
                        }
                    },
                    modifier = Modifier.width(120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Ink950)
            .testTag("room_scanner_screen")
    ) {
        when (scanPhase) {
            PanoramaScanPhase.HOUSE_ROOM_SELECT -> {
                // Screen 1: Scan Entire House Room-by-Room Hub
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Top Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(DorjaColors.Gray700)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = DorjaColors.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "3D House Scanner",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = DorjaColors.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Room-by-Room 360° Panorama Builder",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Sand300
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.Gray700)
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = DorjaColors.Sand300)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Active House Card
                        if (activeListing != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DorjaColors.Gray700,
                                border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DorjaColors.Jol600.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activeListing.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DorjaColors.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${activeListing.publicArea} • ${roomsState.size} Rooms",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DorjaColors.Sand300
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Room List Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT ROOM TO SCAN 3D",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Sand300,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showAddRoomDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Room", color = DorjaColors.Jol600, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rooms List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            if (roomsState.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DorjaColors.Gray700.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("No rooms configured in this house.", color = DorjaColors.Sand300)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            DorjaButton(text = "+ Add First Room", onClick = { showAddRoomDialog = true })
                                        }
                                    }
                                }
                            } else {
                                items(roomsState) { room ->
                                    val isSelected = (selectedRoomId == room.id) || (selectedRoomId == null && room == roomsState.first())
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) DorjaColors.Gray700 else DorjaColors.Ink950,
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isSelected) DorjaColors.Jol600 else DorjaColors.Sand300.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedRoomId = room.id }
                                            .testTag("room_scan_item_${room.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.2f) else DorjaColors.Gray700),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom,
                                                    contentDescription = null,
                                                    tint = if (room.has3DScan) DorjaColors.Success else DorjaColors.Sand300,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = room.displayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = DorjaColors.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (room.dimensions.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "• ${room.dimensions}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = DorjaColors.Sand300,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (room.has3DScan) "360° Panorama Scanned & Verified" else "Ready for 3D Multi-Axis Scan",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (room.has3DScan) DorjaColors.Success else DorjaColors.Sand300,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            DorjaBadge(
                                                text = if (room.has3DScan) "SCANNED" else "SCAN 3D",
                                                backgroundColor = if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.2f) else DorjaColors.Jol600.copy(alpha = 0.2f),
                                                textColor = if (room.has3DScan) DorjaColors.Success else DorjaColors.Jol600
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Launch Scan CTA
                    Column {
                        DorjaButton(
                            text = if (activeRoom?.has3DScan == true) "Re-Scan ${activeRoom.displayName}" else "Scan 3D: ${activeRoom?.displayName ?: "Room"}",
                            onClick = {
                                if (activeRoom != null) {
                                    selectedRoomId = activeRoom.id
                                    scanPhase = PanoramaScanPhase.ORIENTATION_PROMPT
                                }
                            },
                            icon = Icons.Default.ViewInAr,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "launch_panorama_scanner_button"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            PanoramaScanPhase.ORIENTATION_PROMPT -> {
                // Screen 2: Viewfinder Framing + Orientation Prompt
                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Framing Viewfinder Overlay (Dot Matrix / Corner guides)
                    ViewfinderFramingOverlay()

                    // Top Bar (Help, HD Toggle, Close)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DorjaColors.Ink950.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = DorjaColors.White)
                        }

                        // HD Switch
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DorjaColors.Ink950.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("HD", color = if (hdModeEnabled) DorjaColors.Jol600 else DorjaColors.Sand300, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = hdModeEnabled,
                                    onCheckedChange = { hdModeEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DorjaColors.White,
                                        checkedTrackColor = DorjaColors.Jol600,
                                        uncheckedThumbColor = DorjaColors.Sand300,
                                        uncheckedTrackColor = DorjaColors.Gray700
                                    ),
                                    modifier = Modifier.size(32.dp, 20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { scanPhase = PanoramaScanPhase.HOUSE_ROOM_SELECT },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DorjaColors.Ink950.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = DorjaColors.White)
                        }
                    }

                    // Center Guidance: Phone Upright in Portrait Prompt
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(DorjaColors.Jol600.copy(alpha = 0.15f))
                                .border(2.dp, DorjaColors.Jol600, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = DorjaColors.Jol600,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Hold your phone upright in portrait mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = DorjaColors.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Stand in the center of ${activeRoom?.displayName ?: "the room"} and rotate 360° to capture all corners and surfaces.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Sand300,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Green Pill Action Button: PRESS TO START
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = DorjaColors.Jol600,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .clickable {
                                    currentTargetIndex = 0
                                    capturedSlices.clear()
                                    scanPhase = PanoramaScanPhase.ACTIVE_PANORAMA
                                }
                                .testTag("press_to_start_panorama_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PRESS TO START",
                                    color = DorjaColors.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = DorjaColors.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Bottom Bar (Gallery, Center Shutter, Gyro Status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp, start = 32.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DorjaColors.Gray700)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = DorjaColors.Sand300)
                        }

                        // Center Shutter Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DorjaColors.White.copy(alpha = 0.2f))
                                .border(3.dp, DorjaColors.White, CircleShape)
                                .clickable {
                                    currentTargetIndex = 0
                                    capturedSlices.clear()
                                    scanPhase = PanoramaScanPhase.ACTIVE_PANORAMA
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.Jol600)
                            )
                        }

                        // Sensor Status
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DorjaColors.Ink950.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Explore, contentDescription = null, tint = DorjaColors.Success, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GYRO ON", color = DorjaColors.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            PanoramaScanPhase.ACTIVE_PANORAMA -> {
                // Screen 3: Full-Screen Multi-Axis Panorama Construction Scanner
                val currentTarget = panoramaTargets.getOrElse(currentTargetIndex) { panoramaTargets.last() }

                // Simulation loop to guide alignment
                LaunchedEffect(currentTargetIndex) {
                    isReticleAligned = false
                    // Simulate phone turning to target
                    val startYaw = simulatedYaw
                    val startPitch = simulatedPitch
                    val endYaw = currentTarget.angleYaw
                    val endPitch = currentTarget.anglePitch

                    val steps = 15
                    for (i in 1..steps) {
                        delay(50)
                        simulatedYaw = startYaw + (endYaw - startYaw) * (i.toFloat() / steps)
                        simulatedPitch = startPitch + (endPitch - startPitch) * (i.toFloat() / steps)
                        pointCloudPoints += 12
                        planesDetected = (capturedSlices.size / 2) + 1
                    }

                    // Auto-align lock
                    isReticleAligned = true
                    delay(400) // Lock confirmation delay

                    // Auto-capture slice
                    if (!capturedSlices.contains(currentTargetIndex)) {
                        capturedSlices.add(currentTargetIndex)
                    }

                    if (currentTargetIndex < panoramaTargets.size - 1) {
                        delay(250)
                        currentTargetIndex++
                    } else {
                        // All 12 slices captured! Advance to stitching
                        delay(500)
                        scanPhase = PanoramaScanPhase.STITCHING_PROCESSING
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Full Screen AR / Panorama Viewport Canvas
                    ActivePanoramaCanvas(
                        currentTarget = currentTarget,
                        isAligned = isReticleAligned,
                        capturedCount = capturedSlices.size,
                        totalTargets = panoramaTargets.size
                    )

                    // Top Bar: Room Name & Slice Progress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DorjaColors.Ink950.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = (activeRoom?.displayName ?: "ROOM").uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DorjaColors.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${currentTarget.name} • ${currentTarget.axisType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Jol600,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${capturedSlices.size}/${panoramaTargets.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DorjaColors.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Center Directional Guidance Banner
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = DorjaColors.Ink950.copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, if (isReticleAligned) DorjaColors.Success else DorjaColors.Jol600),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-110).dp)
                            .padding(horizontal = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (currentTarget.axisType) {
                                    "UP_CEILING" -> Icons.Default.KeyboardArrowUp
                                    "DOWN_FLOOR" -> Icons.Default.KeyboardArrowDown
                                    else -> Icons.Default.KeyboardArrowRight
                                },
                                contentDescription = null,
                                tint = if (isReticleAligned) DorjaColors.Success else DorjaColors.Jol600,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isReticleAligned) "ALIGNMENT LOCKED (CAPTURING)" else currentTarget.directionText,
                                style = MaterialTheme.typography.labelLarge,
                                color = DorjaColors.White,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Bottom Multi-Slice Progress Strip + Stop Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Live Panorama Thumbnail Stitch Strip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DorjaColors.Ink950.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("PANORAMA STITCH PROGRESS", color = DorjaColors.Sand300, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("${((capturedSlices.size.toFloat() / panoramaTargets.size) * 100).toInt()}%", color = DorjaColors.Jol600, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    panoramaTargets.forEachIndexed { index, target ->
                                        val isCaptured = capturedSlices.contains(index)
                                        val isCurrent = currentTargetIndex == index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(
                                                    when {
                                                        isCaptured -> DorjaColors.Success
                                                        isCurrent -> DorjaColors.Jol600
                                                        else -> DorjaColors.Gray700
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Red Stop Button ("PRESS STOP WHEN DONE")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (capturedSlices.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        if (capturedSlices.isNotEmpty()) {
                                            capturedSlices.removeAt(capturedSlices.lastIndex)
                                            if (currentTargetIndex > 0) currentTargetIndex--
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(DorjaColors.Gray700)
                                ) {
                                    Icon(Icons.Default.Undo, contentDescription = "Undo Slice", tint = DorjaColors.Sand300)
                                }
                                Spacer(modifier = Modifier.width(24.dp))
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { scanPhase = PanoramaScanPhase.STITCHING_PROCESSING },
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(DorjaColors.Error)
                                        .testTag("stop_scan_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Scan",
                                        tint = DorjaColors.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "PRESS STOP WHEN DONE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Sand300,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            PanoramaScanPhase.STITCHING_PROCESSING -> {
                // Screen 4: Panorama Stitching & 3D Construction
                LaunchedEffect(Unit) {
                    delay(1600)
                    scanPhase = PanoramaScanPhase.RESULT_PASSPORT
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = DorjaColors.Jol600,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Stitching 360° Multi-Axis Panorama...",
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stitching ${capturedSlices.size.coerceAtLeast(10)} angular slices with corner and zenith alignment",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Sand300,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            PanoramaScanPhase.RESULT_PASSPORT -> {
                // Screen 5: 3D Panorama Reality Passport Preview & Actions
                var previewPan by remember { mutableFloatStateOf(0f) }
                var previewTilt by remember { mutableFloatStateOf(0f) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DorjaColors.Success, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "3D Reality Passport Ready",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = DorjaColors.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = activeRoom?.displayName ?: "Room Scan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Sand300
                                    )
                                }
                            }
                            DorjaBadge(
                                text = "VERIFIED 3D",
                                backgroundColor = DorjaColors.Success.copy(alpha = 0.2f),
                                textColor = DorjaColors.Success
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive 3D Panorama Viewport Preview Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DorjaColors.Gray700,
                            border = BorderStroke(1.5.dp, DorjaColors.Jol600),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        previewPan += dragAmount.x * 0.5f
                                        previewTilt = (previewTilt - dragAmount.y * 0.3f).coerceIn(-45f, 45f)
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height

                                    // Cylindrical background panorama simulation
                                    val panOffset = (previewPan % w)
                                    drawRect(color = Color(0xFF071B2C))

                                    // Room walls & grid
                                    drawRect(
                                        color = Color(0xFF0F304E),
                                        topLeft = Offset(panOffset, h * 0.2f),
                                        size = Size(w * 0.8f, h * 0.6f)
                                    )
                                    drawRect(
                                        color = Color(0xFF007C78).copy(alpha = 0.3f),
                                        topLeft = Offset(panOffset, h * 0.7f),
                                        size = Size(w * 0.8f, h * 0.3f)
                                    )

                                    // Pan/Tilt Horizon Line
                                    drawLine(
                                        color = Color(0xFF007C78),
                                        start = Offset(0f, (h / 2f) + previewTilt),
                                        end = Offset(w, (h / 2f) + previewTilt),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }

                                Text(
                                    text = "DRAG TO EXPLORE 360°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Sand300,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Telemetry Grid Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DorjaColors.Gray700,
                            border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SPATIAL PANORAMA TELEMETRY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Sand300,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("COVERAGE", color = DorjaColors.Sand300, fontSize = 10.sp)
                                        Text("360° x 180° Full", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Column {
                                        Text("STITCHED SLICES", color = DorjaColors.Sand300, fontSize = 10.sp)
                                        Text("${capturedSlices.size} Angles", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                    Column {
                                        Text("GEOMETRY PLANES", color = DorjaColors.Sand300, fontSize = 10.sp)
                                        Text("8 Detected", color = DorjaColors.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                            }
                        }
                    }

                    // Action Buttons: Save to Room & Scan Next
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DorjaButton(
                            text = "Save 3D Scan to ${activeRoom?.displayName ?: "Room"}",
                            onClick = {
                                scope.launch {
                                    if (activeRoom != null) {
                                        repository.updateRoom3DScan(activeRoom.id, "panorama_360_data_${System.currentTimeMillis()}")
                                    }
                                    onScanSaved(activeRoom?.id ?: "r1")
                                }
                            },
                            icon = Icons.Default.Shield,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "save_room_scan_button"
                        )

                        val unScannedRooms = roomsState.filter { !it.has3DScan && it.id != selectedRoomId }
                        if (unScannedRooms.isNotEmpty()) {
                            DorjaOutlinedButton(
                                text = "Scan Next Room: ${unScannedRooms.first().displayName}",
                                onClick = {
                                    scope.launch {
                                        if (activeRoom != null) {
                                            repository.updateRoom3DScan(activeRoom.id, "panorama_360_data_${System.currentTimeMillis()}")
                                        }
                                        selectedRoomId = unScannedRooms.first().id
                                        scanPhase = PanoramaScanPhase.ORIENTATION_PROMPT
                                    }
                                },
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewfinderFramingOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val margin = 24.dp.toPx()
        val cornerLen = 32.dp.toPx()
        val strokeW = 2.dp.toPx()
        val color = Color(0xFF007C78).copy(alpha = 0.7f)

        // Top-Left Corner
        drawLine(color, Offset(margin, margin), Offset(margin + cornerLen, margin), strokeW)
        drawLine(color, Offset(margin, margin), Offset(margin, margin + cornerLen), strokeW)

        // Top-Right Corner
        drawLine(color, Offset(w - margin, margin), Offset(w - margin - cornerLen, margin), strokeW)
        drawLine(color, Offset(w - margin, margin), Offset(w - margin, margin + cornerLen), strokeW)

        // Bottom-Left Corner
        drawLine(color, Offset(margin, h - margin), Offset(margin + cornerLen, h - margin), strokeW)
        drawLine(color, Offset(margin, h - margin), Offset(margin, h - margin - cornerLen), strokeW)

        // Bottom-Right Corner
        drawLine(color, Offset(w - margin, h - margin), Offset(w - margin - cornerLen, h - margin), strokeW)
        drawLine(color, Offset(w - margin, h - margin), Offset(w - margin, h - margin - cornerLen), strokeW)
    }
}

@Composable
private fun ActivePanoramaCanvas(
    currentTarget: PanoramaSliceTarget,
    isAligned: Boolean,
    capturedCount: Int,
    totalTargets: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reticle_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = 110.dp.toPx()

        // 1. Feature Points cloud (simulated visual spatial tracking)
        val rand = java.util.Random(123)
        for (i in 0..40) {
            val px = rand.nextFloat() * w
            val py = rand.nextFloat() * h
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = 2f,
                center = Offset(px, py)
            )
        }

        // 2. Fixed Compass Orientation Guide Ring
        drawCircle(
            color = Color(0xFF007C78).copy(alpha = 0.3f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
        )

        // 3. Center Viewport Reticle
        drawCircle(
            color = if (isAligned) Color(0xFF2E7D32) else Color.White.copy(alpha = 0.85f),
            radius = 28.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = if (isAligned) Color(0xFF2E7D32) else Color(0xFF007C78),
            start = Offset(cx, cy - 16.dp.toPx()),
            end = Offset(cx, cy + 16.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = if (isAligned) Color(0xFF2E7D32) else Color(0xFF007C78),
            start = Offset(cx - 16.dp.toPx(), cy),
            end = Offset(cx + 16.dp.toPx(), cy),
            strokeWidth = 1.5.dp.toPx()
        )

        // 4. Guided Dots Constellation on Compass Circle (8 Angular Directions)
        val compassAngles = listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0)
        val dotPoints = mutableListOf<Offset>()

        compassAngles.forEach { deg ->
            val rad = Math.toRadians(deg - 90.0)
            val dx = cx + (radius * kotlin.math.cos(rad)).toFloat()
            val dy = cy + (radius * kotlin.math.sin(rad)).toFloat()
            val pt = Offset(dx, dy)
            dotPoints.add(pt)

            drawCircle(
                color = Color(0xFF007C78).copy(alpha = 0.6f),
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = pt
            )
        }

        // Connecting lines between compass dots
        for (i in 0 until dotPoints.size) {
            val nextIdx = (i + 1) % dotPoints.size
            drawLine(
                color = Color(0xFF007C78).copy(alpha = 0.25f),
                start = dotPoints[i],
                end = dotPoints[nextIdx],
                strokeWidth = 1.dp.toPx()
            )
        }

        // 5. Dynamic Active Target Node Offset based on current slice target
        val targetOffset = when (currentTarget.axisType) {
            "UP_CEILING" -> Offset(cx, cy - (radius * 0.8f))
            "DOWN_FLOOR" -> Offset(cx, cy + (radius * 0.8f))
            "CORNER" -> Offset(cx + (radius * 0.7f), cy - (radius * 0.4f))
            else -> Offset(cx + radius, cy)
        }

        val activeNodePos = if (isAligned) Offset(cx, cy) else targetOffset

        // Target Cyan/Green Circle Node with pulsing animation
        drawCircle(
            color = if (isAligned) Color(0xFF2E7D32) else Color(0xFF007C78),
            radius = 16.dp.toPx() * (if (isAligned) pulseAnim else 1f),
            center = activeNodePos
        )
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = activeNodePos
        )

        // 6. Directional Guiding Arrow indicator connecting center to target
        if (!isAligned) {
            drawLine(
                color = Color(0xFF007C78),
                start = Offset(cx, cy),
                end = activeNodePos,
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )
        }
    }
}
