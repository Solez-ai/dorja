package com.example.ui.listing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.data.model.LegalDocument
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

data class PhotoAssignmentItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val caption: String = "Property Photo",
    var assignedRoomId: String? = null // null means Cover / General Property Photo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    onListingCreated: (String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()

    // 1. Basic Listing Info
    var title by remember { mutableStateOf("") }
    var intent by remember { mutableStateOf("RENT") }
    var propertyType by remember { mutableStateOf("APARTMENT") }
    var publicArea by remember { mutableStateOf("") }
    var exactAddress by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var bedrooms by remember { mutableIntStateOf(1) }
    var bathrooms by remember { mutableIntStateOf(1) }
    var balconies by remember { mutableIntStateOf(0) }
    var sqftText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val selectedTags = remember {
        mutableStateListOf<String>()
    }
    var customTagInput by remember { mutableStateOf("") }

    // 2. Rooms State (Empty by default - no fake rooms or scans)
    val customRooms = remember {
        mutableStateListOf<RoomItem>()
    }

    var showAddRoomDialog by remember { mutableStateOf(false) }
    var newRoomType by remember { mutableStateOf("BEDROOM") }
    var newRoomName by remember { mutableStateOf("") }
    var newRoomDimensions by remember { mutableStateOf("") }
    var newRoomDescription by remember { mutableStateOf("") }

    // 3. Photos & Assignment State
    val photoAssignments = remember {
        mutableStateListOf<PhotoAssignmentItem>()
    }

    var coverPhotoIndex by remember { mutableIntStateOf(0) }
    var showMultiPhotoSelectorDialog by remember { mutableStateOf(false) }

    // 4. 3D Scan Section States
    var show3DScannerForRooms by remember { mutableStateOf(false) }
    var activeScanningRoomId by remember { mutableStateOf<String?>(null) }
    var roomToRescanWarn by remember { mutableStateOf<RoomItem?>(null) }
    var showRoomPickerForScanDialog by remember { mutableStateOf(false) }

    // 5. Legal Documents (Empty by default - no fake documents)
    val customLegalDocs = remember {
        mutableStateListOf<LegalDocument>()
    }

    var showAddDocDialog by remember { mutableStateOf(false) }
    var newDocType by remember { mutableStateOf("KHATIAN_PORCHA") }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocNumber by remember { mutableStateOf("") }
    var newDocAuthority by remember { mutableStateOf("") }
    var newDocYear by remember { mutableStateOf("2024") }
    var newDocNotes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val intentOptions = listOf(Pair("RENT", "For Rent"), Pair("SALE", "For Sale"))
    val typeOptions = listOf(
        Pair("APARTMENT", "Apartment"),
        Pair("HOUSE", "Independent House"),
        Pair("ROOM", "Bachelor Room"),
        Pair("SUBLET", "Family Sublet"),
        Pair("OFFICE", "Commercial Office"),
        Pair("SHOP", "Retail Shop"),
        Pair("LAND", "Plot / Land")
    )

    val docTypeOptions = listOf(
        Pair("KHATIAN_PORCHA", "Khatian / Porcha"),
        Pair("MUTATION_NAMZARI", "Mutation / Namzari"),
        Pair("RAJUK_APPROVAL", "RAJUK / CDA Plan"),
        Pair("TAX_DAKHILA", "Municipal Tax Dakhila"),
        Pair("NEC_CERTIFICATE", "NEC Certificate"),
        Pair("SALE_DEED", "Registered Sale Deed"),
        Pair("OTHER", "Other Document")
    )

    val commonAmenities = listOf(
        "Lift", "24/7 Generator", "Gas Connection", "Dedicated Parking",
        "CCTV & Security", "Rooftop Garden", "South Facing", "Pre-paid Meter",
        "High Speed Fiber", "Fire Safety", "Water Reservoir", "Solar Power", "Servant Room"
    )

    // Helper to start scanning a room (with already-scanned warning check)
    fun requestScanForRoom(room: RoomItem) {
        if (room.has3DScan) {
            roomToRescanWarn = room
        } else {
            activeScanningRoomId = room.id
            show3DScannerForRooms = true
        }
    }

    // WARNING DIALOG BEFORE RE-SCANNING AN ALREADY SCANNED ROOM
    if (roomToRescanWarn != null) {
        val warnRoom = roomToRescanWarn!!
        AlertDialog(
            onDismissRequest = { roomToRescanWarn = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Room Already Scanned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DorjaColors.Ink950
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Warning: '${warnRoom.displayName}' already has a completed and verified 3D spatial scan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Scanning this room again will overwrite the existing 360° panorama and spatial tour data. Do you wish to proceed and re-scan?",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Re-Scan Room",
                    onClick = {
                        val targetId = warnRoom.id
                        roomToRescanWarn = null
                        activeScanningRoomId = targetId
                        show3DScannerForRooms = true
                    },
                    modifier = Modifier.width(140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { roomToRescanWarn = null }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ROOM PICKER DIALOG FOR 3D SCAN
    if (showRoomPickerForScanDialog) {
        AlertDialog(
            onDismissRequest = { showRoomPickerForScanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = DorjaColors.Jol600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Room to 3D Scan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Choose which room to perform a 360° spatial scan:", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                    Spacer(modifier = Modifier.height(4.dp))

                    customRooms.forEach { room ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DorjaColors.Sand100,
                            border = BorderStroke(1.dp, if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.5f) else DorjaColors.BentoCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRoomPickerForScanDialog = false
                                    requestScanForRoom(room)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = if (room.has3DScan) DorjaColors.Success else DorjaColors.Gray700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(room.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DorjaColors.Ink950)
                                        if (room.dimensions.isNotBlank()) {
                                            Text(room.dimensions, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray500, fontSize = 11.sp)
                                        }
                                    }
                                }

                                DorjaBadge(
                                    text = if (room.has3DScan) "SCANNED" else "READY",
                                    backgroundColor = if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.15f) else DorjaColors.Jol600.copy(alpha = 0.15f),
                                    textColor = if (room.has3DScan) DorjaColors.Success else DorjaColors.Jol600
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoomPickerForScanDialog = false }) {
                    Text("Close", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // MULTI-PHOTO SELECTION MODAL
    if (showMultiPhotoSelectorDialog) {
        val selectedPhotoUrls = remember { mutableStateListOf<String>() }
        var customUrlInput by remember { mutableStateOf("") }
        var customCaptionInput by remember { mutableStateOf("") }
        var selectedRoomForNewPhoto by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showMultiPhotoSelectorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = DorjaColors.Jol600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Property Photos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Add photos and assign them directly to designated rooms:",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )

                    // Quick Room Photo Slot Generators
                    Text(
                        text = "QUICK ADD ROOM PHOTO SLOTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customRooms.forEach { room ->
                            val alreadyHasPhoto = photoAssignments.any { it.assignedRoomId == room.id }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (alreadyHasPhoto) DorjaColors.BentoBlueBg else DorjaColors.Sand100,
                                border = BorderStroke(1.dp, if (alreadyHasPhoto) DorjaColors.Jol600 else DorjaColors.BentoCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoAssignments.add(
                                            PhotoAssignmentItem(
                                                url = "local://${room.roomType.lowercase()}_photo_${photoAssignments.size + 1}",
                                                caption = "${room.displayName} View",
                                                assignedRoomId = room.id
                                            )
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.MeetingRoom,
                                            contentDescription = null,
                                            tint = DorjaColors.Jol600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "+ Add ${room.displayName} Photo",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DorjaColors.Ink950
                                            )
                                            Text(
                                                text = if (alreadyHasPhoto) "Photo slot active" else "Tap to add room photo slot",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DorjaColors.Gray500,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (alreadyHasPhoto) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Added",
                                            tint = DorjaColors.Jol600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ADD CUSTOM PHOTO URL OR PATH",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customCaptionInput,
                        onValueChange = { customCaptionInput = it },
                        label = { Text("Photo Caption / Label") },
                        placeholder = { Text("e.g. Master Bedroom Balcony") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            placeholder = { Text("https://example.com/photo.jpg", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DorjaColors.White,
                                unfocusedContainerColor = DorjaColors.White,
                                focusedBorderColor = DorjaColors.Jol600,
                                unfocusedBorderColor = DorjaColors.BentoCardBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        DorjaButton(
                            text = "+ Add",
                            onClick = {
                                if (customUrlInput.isNotBlank()) {
                                    photoAssignments.add(
                                        PhotoAssignmentItem(
                                            url = customUrlInput.trim(),
                                            caption = customCaptionInput.ifBlank { "Property Photo ${photoAssignments.size + 1}" },
                                            assignedRoomId = selectedRoomForNewPhoto
                                        )
                                    )
                                    customUrlInput = ""
                                    customCaptionInput = ""
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Done (${photoAssignments.size} Photos)",
                    onClick = {
                        showMultiPhotoSelectorDialog = false
                    },
                    modifier = Modifier.width(160.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showMultiPhotoSelectorDialog = false }) {
                    Text("Close", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ADD ROOM DIALOG
    if (showAddRoomDialog) {
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = DorjaColors.Jol600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Room to Property", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Room Category", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("BEDROOM", "LIVING_ROOM", "DINING_ROOM", "KITCHEN", "BATHROOM", "BALCONY", "OFFICE", "SERVANT_ROOM", "STORE_ROOM", "OTHER")) { type ->
                            DorjaChip(
                                selected = newRoomType == type,
                                label = type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                onClick = {
                                    newRoomType = type
                                    if (newRoomName.isBlank()) {
                                        newRoomName = type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("Room Display Name") },
                        placeholder = { Text("e.g. Master Bedroom, Dining Lounge, Chef Kitchen") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = newRoomDimensions,
                        onValueChange = { newRoomDimensions = it },
                        label = { Text("Dimensions / Size") },
                        placeholder = { Text("e.g. 15 x 14 ft (210 sqft)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = newRoomDescription,
                        onValueChange = { newRoomDescription = it },
                        label = { Text("Room Highlights / Features") },
                        placeholder = { Text("e.g. South facing, attached bathroom, wooden wardrobe") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Add Room",
                    onClick = {
                        val displayName = if (newRoomName.isNotBlank()) newRoomName.trim() else "Room ${customRooms.size + 1}"
                        customRooms.add(
                            RoomItem(
                                id = "r_" + UUID.randomUUID().toString().take(6),
                                listingId = "",
                                roomType = newRoomType,
                                displayName = displayName,
                                dimensions = newRoomDimensions.trim(),
                                description = newRoomDescription.trim(),
                                ordinal = customRooms.size,
                                has3DScan = false
                            )
                        )
                        newRoomName = ""
                        newRoomDescription = ""
                        showAddRoomDialog = false
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

    // ADD LEGAL DOCUMENT DIALOG
    if (showAddDocDialog) {
        AlertDialog(
            onDismissRequest = { showAddDocDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = DorjaColors.Jol600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Legal Document", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Document Type", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(docTypeOptions) { (type, label) ->
                            DorjaChip(
                                selected = newDocType == type,
                                label = label,
                                onClick = {
                                    newDocType = type
                                    if (newDocTitle.isBlank()) newDocTitle = label
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Document Title") },
                        placeholder = { Text("e.g. CS / RS Khatian Record") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = newDocNumber,
                        onValueChange = { newDocNumber = it },
                        label = { Text("Document / Deed / Permit No.") },
                        placeholder = { Text("e.g. KHT-DHN-8849/2018") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = newDocAuthority,
                        onValueChange = { newDocAuthority = it },
                        label = { Text("Issuing Authority / Office") },
                        placeholder = { Text("e.g. AC Land Dhanmondi, RAJUK") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = newDocYear,
                        onValueChange = { newDocYear = it },
                        label = { Text("Issue Year / Date") },
                        placeholder = { Text("e.g. 2024") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    OutlinedTextField(
                        value = newDocNotes,
                        onValueChange = { newDocNotes = it },
                        label = { Text("Notes / Verification Details") },
                        placeholder = { Text("e.g. Verified against sub-registry record room.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Add Document",
                    onClick = {
                        val titleToAdd = if (newDocTitle.isNotBlank()) newDocTitle else "Legal Document ${customLegalDocs.size + 1}"
                        val numberToAdd = if (newDocNumber.isNotBlank()) newDocNumber else "DOC-BD-${(1000..9999).random()}"
                        val authorityToAdd = if (newDocAuthority.isNotBlank()) newDocAuthority else "Authorized Sub-Registry Office"

                        customLegalDocs.add(
                            LegalDocument(
                                id = "doc_" + UUID.randomUUID().toString().take(6),
                                listingId = "",
                                documentType = newDocType,
                                documentTitle = titleToAdd,
                                documentNumber = numberToAdd,
                                issuingAuthority = authorityToAdd,
                                issueDate = newDocYear,
                                verificationStatus = "VERIFIED",
                                notes = newDocNotes
                            )
                        )
                        newDocTitle = ""
                        newDocNumber = ""
                        newDocAuthority = ""
                        newDocNotes = ""
                        showAddDocDialog = false
                    },
                    modifier = Modifier.width(140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddDocDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // 3D ROOM SPATIAL SCANNER DIALOG (WITH GUIDED CONNECTING DOTS & DIRECTIONAL ARROWS)
    if (show3DScannerForRooms) {
        Guided3DPanoramaScannerDialog(
            listingTitle = title.ifBlank { "New Property Listing" },
            rooms = customRooms,
            initialSelectedRoomId = activeScanningRoomId,
            onRoomScanSaved = { roomId ->
                val index = customRooms.indexOfFirst { it.id == roomId }
                if (index != -1) {
                    customRooms[index] = customRooms[index].copy(has3DScan = true)
                }
            },
            onRequestRescanWarning = { room ->
                roomToRescanWarn = room
            },
            onDismiss = { show3DScannerForRooms = false }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("create_listing_screen"),
        containerColor = DorjaColors.CanvasBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DorjaColors.CanvasBg)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DorjaColors.White)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DorjaColors.Ink950
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Create New Listing",
                            style = MaterialTheme.typography.titleLarge,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Host Suite • Dorja Bangladesh",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Intent & Type
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PROPERTY INTENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intentOptions.forEach { (key, label) ->
                            DorjaChip(
                                selected = intent == key,
                                label = label,
                                onClick = { intent = key },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "PROPERTY TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(typeOptions) { (key, label) ->
                            DorjaChip(
                                selected = propertyType == key,
                                label = label,
                                onClick = { propertyType = key }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    DorjaInput(
                        value = title,
                        onValueChange = { title = it },
                        label = "Property Title",
                        placeholder = "e.g. 3-Bed Luxury Apartment, Dhanmondi 8/A",
                        leadingIcon = Icons.Default.Home,
                        testTag = "input_listing_title"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price
                    DorjaInput(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = if (intent == "RENT") "Monthly Rent (BDT)" else "Asking Price (BDT)",
                        placeholder = "e.g. 35000",
                        leadingIcon = Icons.Default.Payments,
                        keyboardType = KeyboardType.Number,
                        testTag = "input_listing_price"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Public Area
                    DorjaInput(
                        value = publicArea,
                        onValueChange = { publicArea = it },
                        label = "Public Area / Neighborhood",
                        placeholder = "e.g. Dhanmondi 8/A, Dhaka",
                        leadingIcon = Icons.Default.LocationOn,
                        testTag = "input_listing_public_area"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Exact Address
                    OutlinedTextField(
                        value = exactAddress,
                        onValueChange = { exactAddress = it },
                        label = { Text("Exact Address (Protected)") },
                        placeholder = { Text("House 24, Road 8/A, Block C (Unlocked only with SafeView pass)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_listing_exact_address"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Specs Counters Bento Card
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PROPERTY SPECIFICATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CounterBox(
                            label = "Beds",
                            count = bedrooms,
                            onIncrement = { bedrooms++ },
                            onDecrement = { if (bedrooms > 0) bedrooms-- },
                            modifier = Modifier.weight(1f)
                        )
                        CounterBox(
                            label = "Baths",
                            count = bathrooms,
                            onIncrement = { bathrooms++ },
                            onDecrement = { if (bathrooms > 0) bathrooms-- },
                            modifier = Modifier.weight(1f)
                        )
                        CounterBox(
                            label = "Balconies",
                            count = balconies,
                            onIncrement = { balconies++ },
                            onDecrement = { if (balconies > 0) balconies-- },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    DorjaInput(
                        value = sqftText,
                        onValueChange = { sqftText = it },
                        label = "Total Floor Area (Sqft)",
                        placeholder = "e.g. 1450",
                        leadingIcon = Icons.Default.SquareFoot,
                        keyboardType = KeyboardType.Number,
                        testTag = "input_listing_sqft"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // ______ 1. ADD ROOMS SECTION ______
            // ==========================================
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MeetingRoom,
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1. ADD ROOMS (${customRooms.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Specify individual rooms for photos and 3D spatial scanning",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        DorjaButton(
                            text = "+ Add Room",
                            onClick = {
                                newRoomName = ""
                                newRoomDescription = ""
                                showAddRoomDialog = true
                            },
                            icon = Icons.Default.Add,
                            modifier = Modifier.height(36.dp),
                            testTag = "add_room_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (customRooms.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DorjaColors.Sand100,
                            border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddRoomDialog = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No rooms added yet", fontWeight = FontWeight.Bold, color = DorjaColors.Ink950)
                                Text("Tap to add your first room (e.g. Master Bed, Living Room)", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customRooms.forEachIndexed { index, room ->
                                val photosForRoom = photoAssignments.count { it.assignedRoomId == room.id }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DorjaColors.White,
                                    border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DorjaColors.BentoBlueBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MeetingRoom,
                                                contentDescription = null,
                                                tint = DorjaColors.Jol600,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = room.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = DorjaColors.Ink950,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (room.dimensions.isNotBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "• ${room.dimensions}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DorjaColors.Gray500,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            if (room.description.isNotBlank()) {
                                                Text(
                                                    text = room.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DorjaColors.Gray700,
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                DorjaBadge(
                                                    text = if (photosForRoom > 0) "$photosForRoom Photo${if (photosForRoom > 1) "s" else ""} Assigned" else "No Photos",
                                                    backgroundColor = if (photosForRoom > 0) DorjaColors.BentoBlueBg else DorjaColors.Sand300.copy(alpha = 0.3f),
                                                    textColor = if (photosForRoom > 0) DorjaColors.Jol600 else DorjaColors.Gray700
                                                )

                                                DorjaBadge(
                                                    text = if (room.has3DScan) "3D SCAN READY" else "3D PENDING",
                                                    backgroundColor = if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.15f) else DorjaColors.Sand300.copy(alpha = 0.3f),
                                                    textColor = if (room.has3DScan) DorjaColors.Success else DorjaColors.Gray700
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                // Unassign photos from this room before removing
                                                photoAssignments.forEach {
                                                    if (it.assignedRoomId == room.id) it.assignedRoomId = null
                                                }
                                                customRooms.removeAt(index)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Room",
                                                tint = DorjaColors.Gray500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // ______ 2. ADD PICTURES FOR ROOMS SECTION ______
            // ==========================================
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2. ADD PICTURES FOR ROOMS (${photoAssignments.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Select multiple photos and assign each to specific rooms",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        DorjaButton(
                            text = "+ Add Photos",
                            onClick = { showMultiPhotoSelectorDialog = true },
                            icon = Icons.Default.AddPhotoAlternate,
                            modifier = Modifier.height(36.dp),
                            testTag = "select_multiple_photos_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (photoAssignments.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DorjaColors.Sand100,
                            border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMultiPhotoSelectorDialog = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No photos selected yet", fontWeight = FontWeight.Bold, color = DorjaColors.Ink950, style = MaterialTheme.typography.bodyMedium)
                                Text("Tap to select multiple images and assign them to your rooms", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                            }
                        }
                    } else {
                        // Interactive Photo Cards List with In-Card Room Dropdown Assignment
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            photoAssignments.forEachIndexed { index, photoItem ->
                                val isCover = index == coverPhotoIndex
                                val assignedRoom = customRooms.find { it.id == photoItem.assignedRoomId }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DorjaColors.White,
                                    border = BorderStroke(
                                        width = if (isCover) 2.dp else 1.dp,
                                        color = if (isCover) DorjaColors.Jol600 else DorjaColors.BentoCardBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Image thumbnail with Cover Badge
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = photoItem.url,
                                                contentDescription = photoItem.caption,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )

                                            if (isCover) {
                                                Surface(
                                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                    color = DorjaColors.Jol600,
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = "COVER",
                                                        color = DorjaColors.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        // Assignment & Room Dropdown Selector
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Photo ${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DorjaColors.Gray500,
                                                fontFamily = FontFamily.Monospace
                                            )

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Room Assignment Picker Menu
                                            var showRoomMenu by remember { mutableStateOf(false) }

                                            Box {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = DorjaColors.Sand100,
                                                    border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { showRoomMenu = true }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = if (assignedRoom != null) Icons.Default.MeetingRoom else Icons.Default.Home,
                                                                contentDescription = null,
                                                                tint = DorjaColors.Jol600,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = assignedRoom?.displayName ?: "General / Cover Photo",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = DorjaColors.Ink950,
                                                                fontWeight = FontWeight.SemiBold,
                                                                maxLines = 1
                                                            )
                                                        }
                                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select Room", tint = DorjaColors.Gray700, modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                DropdownMenu(
                                                    expanded = showRoomMenu,
                                                    onDismissRequest = { showRoomMenu = false },
                                                    modifier = Modifier.background(DorjaColors.White)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("General / Exterior Cover", color = DorjaColors.Ink950) },
                                                        onClick = {
                                                            photoAssignments[index] = photoAssignments[index].copy(assignedRoomId = null)
                                                            showRoomMenu = false
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = DorjaColors.Gray700) }
                                                    )
                                                    HorizontalDivider()
                                                    customRooms.forEach { room ->
                                                        DropdownMenuItem(
                                                            text = { Text(room.displayName, color = DorjaColors.Ink950) },
                                                            onClick = {
                                                                photoAssignments[index] = photoAssignments[index].copy(assignedRoomId = room.id)
                                                                showRoomMenu = false
                                                            },
                                                            leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = DorjaColors.Jol600) }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            if (!isCover) {
                                                Text(
                                                    text = "Set as Listing Cover",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DorjaColors.Jol600,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier
                                                        .clickable { coverPhotoIndex = index }
                                                        .padding(vertical = 2.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                if (photoAssignments.size > 0) {
                                                    photoAssignments.removeAt(index)
                                                    if (coverPhotoIndex >= photoAssignments.size) coverPhotoIndex = 0
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Photo", tint = DorjaColors.Gray500, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // ______ 3. 3D SCAN SECTION ______
            // ==========================================
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ViewInAr,
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "3. 3D SPATIAL ROOM SCAN (${customRooms.count { it.has3DScan }}/${customRooms.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Pick which room to scan with guided 360° dots & arrows HUD",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        if (customRooms.isNotEmpty()) {
                            DorjaButton(
                                text = "3D Scan Room",
                                onClick = {
                                    val firstUnscanned = customRooms.find { !it.has3DScan }
                                    if (firstUnscanned != null) {
                                        requestScanForRoom(firstUnscanned)
                                    } else {
                                        showRoomPickerForScanDialog = true
                                    }
                                },
                                icon = Icons.Default.ViewInAr,
                                modifier = Modifier.height(36.dp),
                                testTag = "start_3d_scan_button"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (customRooms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Please add rooms in Step 1 first before scanning 3D tours.", color = DorjaColors.Gray500, fontSize = 12.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customRooms.forEach { room ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DorjaColors.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.5f) else DorjaColors.BentoCardBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (room.has3DScan) DorjaColors.Success.copy(alpha = 0.15f) else DorjaColors.Sand100),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom,
                                                    contentDescription = null,
                                                    tint = if (room.has3DScan) DorjaColors.Success else DorjaColors.Gray700,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = room.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = DorjaColors.Ink950,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (room.has3DScan) "360° Panorama Scanned" else "Not Scanned",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (room.has3DScan) DorjaColors.Success else DorjaColors.Gray500,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        DorjaOutlinedButton(
                                            text = if (room.has3DScan) "Re-Scan" else "Scan 3D",
                                            onClick = { requestScanForRoom(room) },
                                            icon = Icons.Default.CameraAlt,
                                            modifier = Modifier.height(32.dp),
                                            testTag = "scan_btn_${room.id}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Verified Legal Documents Bento Card
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "4. LEGAL & TITLE DOCUMENTS (${customLegalDocs.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Khatian, Mutation, RAJUK & Tax Records",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        DorjaButton(
                            text = "+ Add Doc",
                            onClick = {
                                newDocTitle = ""
                                newDocNumber = ""
                                newDocAuthority = ""
                                newDocNotes = ""
                                showAddDocDialog = true
                            },
                            icon = Icons.Default.Add,
                            modifier = Modifier.height(36.dp),
                            testTag = "add_legal_doc_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (customLegalDocs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No legal documents attached yet. Add Khatian, Mutation or RAJUK plan to earn Verified Listing status.",
                                color = DorjaColors.Gray500,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customLegalDocs.forEachIndexed { index, doc ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = DorjaColors.White,
                                    border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DorjaColors.BentoBlueBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = DorjaColors.Jol600,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = doc.documentTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = DorjaColors.Ink950,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                DorjaBadge(
                                                    text = doc.verificationStatus,
                                                    backgroundColor = DorjaColors.Success.copy(alpha = 0.15f),
                                                    textColor = DorjaColors.Success
                                                )
                                            }
                                            Text(
                                                text = "No: ${doc.documentNumber} • ${doc.issuingAuthority}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DorjaColors.Gray500,
                                                fontSize = 11.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { customLegalDocs.removeAt(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Document",
                                                tint = DorjaColors.Gray500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amenities & Tags Bento Card
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "AMENITIES & FEATURES (${selectedTags.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonAmenities.forEach { amenity ->
                            val isSelected = selectedTags.contains(amenity)
                            DorjaChip(
                                selected = isSelected,
                                label = amenity,
                                onClick = {
                                    if (isSelected) selectedTags.remove(amenity)
                                    else selectedTags.add(amenity)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description Bento Card
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "PROPERTY DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Notes for Seekers") },
                        placeholder = { Text("Describe neighborhood advantages, natural ventilation, handover condition...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_listing_description"),
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DorjaColors.Error.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, DorjaColors.Error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DorjaColors.Error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = DorjaColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit Button
            DorjaButton(
                text = "Publish Property Listing",
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Please enter a property title."
                        return@DorjaButton
                    }
                    val price = priceText.toIntOrNull() ?: 25000
                    val sqft = sqftText.toIntOrNull() ?: 1200
                    val tagsString = selectedTags.joinToString(",")

                    // Update room photo paths based on assignments
                    val finalRooms = customRooms.map { room ->
                        val assignedPhoto = photoAssignments.find { it.assignedRoomId == room.id }
                        if (assignedPhoto != null) {
                            room.copy(photoPath = assignedPhoto.url)
                        } else {
                            room
                        }
                    }

                    val coverPhoto = photoAssignments.getOrNull(coverPhotoIndex)?.url
                        ?: photoAssignments.find { it.assignedRoomId == null }?.url
                        ?: photoAssignments.firstOrNull()?.url

                    scope.launch {
                        val newId = repository.createListingWithRooms(
                            title = title,
                            intent = intent,
                            propertyType = propertyType,
                            publicArea = publicArea,
                            exactAddress = exactAddress,
                            priceAmount = price,
                            bedrooms = bedrooms,
                            bathrooms = bathrooms,
                            balconies = balconies,
                            sqft = sqft,
                            tags = tagsString,
                            virtualTourUrl = null,
                            coverPhotoUrl = coverPhoto,
                            description = description,
                            customRooms = finalRooms,
                            legalDocs = customLegalDocs
                        )
                        onListingCreated(newId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                testTag = "publish_listing_button"
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun CounterBox(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DorjaColors.White,
        border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.CanvasBg)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                }
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.CanvasBg)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

private data class GuidedScanTarget(
    val index: Int,
    val name: String,
    val angleDegree: Float,
    val elevationPitch: Float,
    val directionInstruction: String,
    val arrowOrientation: String // "RIGHT", "UP_RIGHT", "UP", "DOWN", "LEFT", "UP_LEFT", "DOWN_LEFT", "DOWN_RIGHT"
)

@Composable
fun Guided3DPanoramaScannerDialog(
    listingTitle: String,
    rooms: List<RoomItem>,
    initialSelectedRoomId: String?,
    onRoomScanSaved: (String) -> Unit,
    onRequestRescanWarning: (RoomItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedRoomId by remember {
        mutableStateOf(initialSelectedRoomId ?: rooms.firstOrNull()?.id)
    }
    val activeRoom = rooms.find { it.id == selectedRoomId } ?: rooms.firstOrNull()

    var showRoomSwitchMenu by remember { mutableStateOf(false) }

    // Camera Permission State
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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 12 Guided Panorama Target Degrees around 360° with vertical elevation
    val targets = remember {
        listOf(
            GuidedScanTarget(0, "Center Front", 0f, 0f, "Align Front 0°", "UP"),
            GuidedScanTarget(1, "Front-Right Corner", 45f, 0f, "Rotate 45° Right", "UP_RIGHT"),
            GuidedScanTarget(2, "Right Wall", 90f, 0f, "Pan 90° Right", "RIGHT"),
            GuidedScanTarget(3, "Rear-Right Corner", 135f, 0f, "Turn 135° Right", "DOWN_RIGHT"),
            GuidedScanTarget(4, "Rear Wall", 180f, 0f, "Turn 180° Back", "DOWN"),
            GuidedScanTarget(5, "Rear-Left Corner", 225f, 0f, "Turn 225° Left", "DOWN_LEFT"),
            GuidedScanTarget(6, "Left Wall", 270f, 0f, "Pan 270° Left", "LEFT"),
            GuidedScanTarget(7, "Front-Left Corner", 315f, 0f, "Turn 315° Left", "UP_LEFT"),
            GuidedScanTarget(8, "Ceiling Zenith", 0f, -45f, "Tilt Up to Ceiling +45°", "UP"),
            GuidedScanTarget(9, "Ceiling Corner", 90f, -45f, "Tilt Up Right Corner +45°", "UP_RIGHT"),
            GuidedScanTarget(10, "Floor Surface", 0f, 45f, "Tilt Down to Floor -45°", "DOWN"),
            GuidedScanTarget(11, "Floor Corner", 270f, 45f, "Tilt Down Left Corner -45°", "DOWN_LEFT")
        )
    }

    var currentTargetIndex by remember(selectedRoomId) { mutableIntStateOf(0) }
    val capturedSlices = remember(selectedRoomId) { mutableStateListOf<Int>() }
    var isPreviewMode by remember(selectedRoomId) { mutableStateOf(false) }
    var showOrientationPrompt by remember(selectedRoomId) { mutableStateOf(true) }
    var shutterFlash by remember { mutableStateOf(false) }

    var previewPan by remember { mutableFloatStateOf(0f) }
    var previewTilt by remember { mutableFloatStateOf(0f) }

    val currentTarget = targets.getOrElse(currentTargetIndex) { targets.first() }

    LaunchedEffect(shutterFlash) {
        if (shutterFlash) {
            delay(120)
            shutterFlash = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.Ink950)
                .testTag("guided_3d_scanner_dialog")
        ) {
            // Camera Preview
            if (hasCameraPermission && !isPreviewMode) {
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

            // Flash effect
            if (shutterFlash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }

            // Orientation Prompt (shown before scanning starts)
            if (showOrientationPrompt) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DorjaColors.Ink950.copy(alpha = 0.95f))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Hold your phone upright",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Stand in the center of ${activeRoom?.displayName ?: "the room"} and keep the phone at chest height in portrait mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Sand300,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "You will capture 12 angles: 8 around you, 2 ceiling, and 2 floor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Jol600,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    DorjaButton(
                        text = "Start 3D Scan",
                        onClick = { showOrientationPrompt = false },
                        icon = Icons.Default.ViewInAr,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(52.dp),
                        testTag = "start_scan_from_prompt"
                    )
                }
            }

            // Scanner HUD Layout (only when orientation prompt is dismissed)
            if (!showOrientationPrompt) Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Room Selector & Close
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.Gray700.copy(alpha = 0.85f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = DorjaColors.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "3D Room Spatial Scanner",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DorjaColors.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scanning: ${activeRoom?.displayName ?: "Room"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Jol600,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Room Switching Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DorjaColors.Gray700.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, DorjaColors.Jol600),
                            modifier = Modifier.clickable { showRoomSwitchMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Switch Room", color = DorjaColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Room Selection Menu
                    DropdownMenu(
                        expanded = showRoomSwitchMenu,
                        onDismissRequest = { showRoomSwitchMenu = false },
                        modifier = Modifier.background(DorjaColors.Ink950)
                    ) {
                        rooms.forEach { room ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(room.displayName, color = DorjaColors.White, fontWeight = FontWeight.Bold)
                                        if (room.has3DScan) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(Scanned)", color = DorjaColors.Success, fontSize = 11.sp)
                                        }
                                    }
                                },
                                onClick = {
                                    showRoomSwitchMenu = false
                                    if (room.has3DScan) {
                                        onRequestRescanWarning(room)
                                    } else {
                                        selectedRoomId = room.id
                                        currentTargetIndex = 0
                                        isPreviewMode = false
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (room.has3DScan) Icons.Default.CheckCircle else Icons.Default.MeetingRoom,
                                        contentDescription = null,
                                        tint = if (room.has3DScan) DorjaColors.Success else DorjaColors.Jol600
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress strip across all 12 target angles
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DorjaColors.Ink950.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, DorjaColors.Sand300.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            targets.forEachIndexed { idx, _ ->
                                val isCaptured = capturedSlices.contains(idx)
                                val isCurrent = currentTargetIndex == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
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

                // Middle: Viewfinder with Guided Dots & Directional Arrows
                if (isPreviewMode) {
                    // 360 Drag View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DorjaColors.Gray700.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, DorjaColors.Jol600),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        previewPan += dragAmount.x * 0.5f
                                        previewTilt = (previewTilt - dragAmount.y * 0.3f).coerceIn(-40f, 40f)
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val panOffset = (previewPan % w)

                                    drawRect(color = Color(0xFF071B2C))
                                    drawRect(
                                        color = Color(0xFF0F304E),
                                        topLeft = Offset(panOffset, h * 0.2f),
                                        size = Size(w * 0.8f, h * 0.6f)
                                    )
                                    drawRect(
                                        color = Color(0xFF007C78).copy(alpha = 0.25f),
                                        topLeft = Offset(panOffset, h * 0.7f),
                                        size = Size(w * 0.8f, h * 0.3f)
                                    )
                                    drawLine(
                                        color = Color(0xFF007C78),
                                        start = Offset(0f, (h / 2f) + previewTilt),
                                        end = Offset(w, (h / 2f) + previewTilt),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                    ) {
                                    Text(
                                        text = (activeRoom?.displayName ?: "ROOM").uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Sand300,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "360° Spatial Reconstruction Complete",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Success,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "TOUCH & DRAG TO ROTATE 360°",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Sand300,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 10.dp)
                                )
                            }
                        }
                    }
                } else {
                    // LIVE GUIDED SCANNER CANVAS WITH CONNECTED DOTS & DIRECTIONAL ARROWS
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Direction Instruction Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = DorjaColors.Ink950.copy(alpha = 0.85f),
                            border = BorderStroke(1.5.dp, DorjaColors.Jol600)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (currentTarget.arrowOrientation) {
                                        "UP" -> Icons.Default.KeyboardArrowUp
                                        "DOWN" -> Icons.Default.KeyboardArrowDown
                                        "LEFT" -> Icons.AutoMirrored.Filled.ArrowBack
                                        "RIGHT" -> Icons.AutoMirrored.Filled.ArrowForward
                                        "UP_RIGHT" -> Icons.Default.KeyboardArrowUp
                                        "UP_LEFT" -> Icons.Default.KeyboardArrowUp
                                        "DOWN_RIGHT" -> Icons.Default.KeyboardArrowDown
                                        "DOWN_LEFT" -> Icons.Default.KeyboardArrowDown
                                        else -> Icons.Default.Navigation
                                    },
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${currentTarget.directionInstruction} (${currentTargetIndex + 1}/${targets.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DorjaColors.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // GUIDED DOT MATRIX & ARROW CANVAS
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(300.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            GuidedAngleDotsCanvas(
                                currentTargetIndex = currentTargetIndex,
                                capturedSlices = capturedSlices,
                                targets = targets
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SNAP ANGLE BUTTON
                        DorjaButton(
                            text = "Connect & Snap Angle (${currentTargetIndex + 1}/${targets.size})",
                            onClick = {
                                shutterFlash = true
                                if (!capturedSlices.contains(currentTargetIndex)) {
                                    capturedSlices.add(currentTargetIndex)
                                }
                                if (currentTargetIndex < targets.size - 1) {
                                    currentTargetIndex++
                                } else {
                                    isPreviewMode = true
                                }
                            },
                            icon = Icons.Default.CameraAlt,
                            modifier = Modifier.fillMaxWidth(0.85f),
                            testTag = "snap_angle_button"
                        )
                    }
                }

                // Bottom Action Bar: Preview verification or continue scanning
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isPreviewMode || capturedSlices.size >= targets.size) {
                        // SCAN PREVIEW: Verify the 3D scan looks correct
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DorjaColors.Ink950.copy(alpha = 0.9f),
                            border = BorderStroke(1.5.dp, DorjaColors.Success),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DorjaColors.Success, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SCAN COMPLETE — PREVIEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Success,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Drag the preview above to verify the 3D scan looks correct before saving.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Sand300,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Save scan
                        DorjaButton(
                            text = "✓ Save Scan (${activeRoom?.displayName ?: "Room"})",
                            onClick = {
                                if (activeRoom != null) {
                                    onRoomScanSaved(activeRoom.id)
                                }
                                onDismiss()
                            },
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "finish_scan_button"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Re-scan this room
                            DorjaOutlinedButton(
                                text = "Re-Scan",
                                onClick = {
                                    capturedSlices.clear()
                                    currentTargetIndex = 0
                                    isPreviewMode = false
                                },
                                icon = Icons.Default.Refresh,
                                modifier = Modifier.weight(1f),
                                testTag = "rescan_button"
                            )

                            // Skip — don't save
                            DorjaOutlinedButton(
                                text = "Skip",
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                testTag = "skip_scan_button"
                            )
                        }

                        // Scan another room (if available)
                        val nextRoom = rooms.find { !it.has3DScan && it.id != activeRoom?.id }
                        if (nextRoom != null) {
                            DorjaOutlinedButton(
                                text = "Scan Next: ${nextRoom.displayName}",
                                onClick = {
                                    if (activeRoom != null) {
                                        onRoomScanSaved(activeRoom.id)
                                    }
                                    selectedRoomId = nextRoom.id
                                    currentTargetIndex = 0
                                    isPreviewMode = false
                                },
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "scan_another_room_button"
                            )
                        }
                    } else {
                        DorjaOutlinedButton(
                            text = "Cancel Scan",
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidedAngleDotsCanvas(
    currentTargetIndex: Int,
    capturedSlices: List<Int>,
    targets: List<GuidedScanTarget>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
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
        val radius = 100.dp.toPx()

        // 1. Draw Compass Orientation Circle
        drawCircle(
            color = Color(0xFF007C78).copy(alpha = 0.35f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
        )

        // 2. Draw Center Reticle Crosshairs
        drawLine(
            color = Color(0xFF007C78).copy(alpha = 0.5f),
            start = Offset(cx, cy - 20.dp.toPx()),
            end = Offset(cx, cy + 20.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )
        drawLine(
            color = Color(0xFF007C78).copy(alpha = 0.5f),
            start = Offset(cx - 20.dp.toPx(), cy),
            end = Offset(cx + 20.dp.toPx(), cy),
            strokeWidth = 1.5.dp.toPx()
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 12.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )

        // 3. Draw Constellation of Dots for 8 compass + 4 elevation nodes
        val horizontalTargets = targets.filter { it.elevationPitch == 0f }
        val ceilingTargets = targets.filter { it.elevationPitch < 0f }
        val floorTargets = targets.filter { it.elevationPitch > 0f }
        val dotPositions = mutableListOf<Offset>()

        // Draw horizontal ring dots
        horizontalTargets.forEach { target ->
            val rad = Math.toRadians(target.angleDegree.toDouble() - 90.0)
            val dx = cx + (radius * cos(rad)).toFloat()
            val dy = cy + (radius * sin(rad)).toFloat()
            val pt = Offset(dx, dy)
            dotPositions.add(pt)

            val isCaptured = capturedSlices.contains(target.index)
            val isCurrent = currentTargetIndex == target.index

            drawCircle(
                color = when {
                    isCaptured -> Color(0xFF2E7D32)
                    isCurrent -> Color(0xFF007C78)
                    else -> Color.White.copy(alpha = 0.4f)
                },
                radius = if (isCurrent) 10.dp.toPx() * pulseAnim else 6.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = if (isCurrent) 4.dp.toPx() else 2.5.dp.toPx(),
                center = pt
            )
        }

        // Draw ceiling dots (above center)
        ceilingTargets.forEach { target ->
            val rad = Math.toRadians(target.angleDegree.toDouble() - 90.0)
            val dx = cx + (radius * 0.55f * cos(rad)).toFloat()
            val dy = cy - (radius * 0.6f) + (radius * 0.15f * sin(rad)).toFloat()
            val pt = Offset(dx, dy)

            val isCaptured = capturedSlices.contains(target.index)
            val isCurrent = currentTargetIndex == target.index

            drawCircle(
                color = when {
                    isCaptured -> Color(0xFF2E7D32)
                    isCurrent -> Color(0xFFFFA000)
                    else -> Color.White.copy(alpha = 0.3f)
                },
                radius = if (isCurrent) 9.dp.toPx() * pulseAnim else 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = if (isCurrent) 3.5.dp.toPx() else 2.dp.toPx(),
                center = pt
            )
        }

        // Draw floor dots (below center)
        floorTargets.forEach { target ->
            val rad = Math.toRadians(target.angleDegree.toDouble() - 90.0)
            val dx = cx + (radius * 0.55f * cos(rad)).toFloat()
            val dy = cy + (radius * 0.6f) + (radius * 0.15f * sin(rad)).toFloat()
            val pt = Offset(dx, dy)

            val isCaptured = capturedSlices.contains(target.index)
            val isCurrent = currentTargetIndex == target.index

            drawCircle(
                color = when {
                    isCaptured -> Color(0xFF2E7D32)
                    isCurrent -> Color(0xFFFFA000)
                    else -> Color.White.copy(alpha = 0.3f)
                },
                radius = if (isCurrent) 9.dp.toPx() * pulseAnim else 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = if (isCurrent) 3.5.dp.toPx() else 2.dp.toPx(),
                center = pt
            )
        }

        // Connecting lines between horizontal dots
        for (i in 0 until dotPositions.size) {
            val nextIdx = (i + 1) % dotPositions.size
            val isLineActive = capturedSlices.contains(horizontalTargets[i].index) && capturedSlices.contains(horizontalTargets[nextIdx].index)
            drawLine(
                color = if (isLineActive) Color(0xFF2E7D32).copy(alpha = 0.7f) else Color(0xFF007C78).copy(alpha = 0.25f),
                start = dotPositions[i],
                end = dotPositions[nextIdx],
                strokeWidth = if (isLineActive) 2.dp.toPx() else 1.dp.toPx()
            )
        }

        // Connect ceiling dots to each other
        val ceilingPositions = ceilingTargets.map { target ->
            val rad = Math.toRadians(target.angleDegree.toDouble() - 90.0)
            Offset(
                cx + (radius * 0.55f * cos(rad)).toFloat(),
                cy - (radius * 0.6f) + (radius * 0.15f * sin(rad)).toFloat()
            )
        }
        for (i in 0 until ceilingPositions.size) {
            val nextIdx = (i + 1) % ceilingPositions.size
            val isLineActive = capturedSlices.contains(ceilingTargets[i].index) && capturedSlices.contains(ceilingTargets[nextIdx].index)
            drawLine(
                color = if (isLineActive) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color(0xFFFFA000).copy(alpha = 0.2f),
                start = ceilingPositions[i],
                end = ceilingPositions[nextIdx],
                strokeWidth = if (isLineActive) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }

        // Connect floor dots to each other
        val floorPositions = floorTargets.map { target ->
            val rad = Math.toRadians(target.angleDegree.toDouble() - 90.0)
            Offset(
                cx + (radius * 0.55f * cos(rad)).toFloat(),
                cy + (radius * 0.6f) + (radius * 0.15f * sin(rad)).toFloat()
            )
        }
        for (i in 0 until floorPositions.size) {
            val nextIdx = (i + 1) % floorPositions.size
            val isLineActive = capturedSlices.contains(floorTargets[i].index) && capturedSlices.contains(floorTargets[nextIdx].index)
            drawLine(
                color = if (isLineActive) Color(0xFF2E7D32).copy(alpha = 0.5f) else Color(0xFFFFA000).copy(alpha = 0.2f),
                start = floorPositions[i],
                end = floorPositions[nextIdx],
                strokeWidth = if (isLineActive) 1.5.dp.toPx() else 1.dp.toPx()
            )
        }

        // 4. Directional Guiding Arrow pointing from center directly to active target
        val currentTarget = targets.getOrElse(currentTargetIndex) { targets.first() }
        val activeTargetPos = if (currentTarget.elevationPitch == 0f) {
            val rad = Math.toRadians(currentTarget.angleDegree.toDouble() - 90.0)
            Offset(cx + (radius * cos(rad)).toFloat(), cy + (radius * sin(rad)).toFloat())
        } else if (currentTarget.elevationPitch < 0f) {
            // Ceiling Zenith
            Offset(cx, cy - (radius * 0.6f))
        } else {
            // Floor Nadir
            Offset(cx, cy + (radius * 0.6f))
        }

        // Guide arrow line to active node
        drawLine(
            color = Color(0xFF007C78),
            start = Offset(cx, cy),
            end = activeTargetPos,
            strokeWidth = 2.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )

        // Draw directional guidance arrows on each angle node
        dotPositions.forEachIndexed { i, pt ->
            val angle = i * 45.0 - 90.0
            val arrowRad = Math.toRadians(angle)
            val tipX = pt.x + (10.dp.toPx() * cos(arrowRad)).toFloat()
            val tipY = pt.y + (10.dp.toPx() * sin(arrowRad)).toFloat()
            val isCurrent = currentTargetIndex == i

            drawLine(
                color = if (isCurrent) Color(0xFF007C78) else Color.White.copy(alpha = 0.4f),
                start = pt,
                end = Offset(tipX, tipY),
                strokeWidth = if (isCurrent) 2.dp.toPx() else 1.dp.toPx()
            )
        }

        // Active Target Outer Glow Ring
        drawCircle(
            color = Color(0xFF007C78).copy(alpha = 0.4f),
            radius = 16.dp.toPx() * pulseAnim,
            center = activeTargetPos,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
