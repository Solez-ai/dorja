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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.R
import com.example.data.country.AuthorityLinks
import com.example.data.country.CountryRegistry
import com.example.data.country.LiveabilityField
import com.example.data.model.DEFAULT_SELF_DECLARED_NOTE
import com.example.data.model.EvidenceLevel
import com.example.data.model.LegalDocument
import com.example.data.model.Promise
import com.example.data.model.RoomItem
import com.example.ui.components.BentoCard
import com.example.ui.components.CountryPicker
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaInput
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.components.EvidenceBadge
import com.example.ui.components.GovernmentSourceCard
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import kotlin.math.min
import kotlin.math.max

@Composable
private fun energyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DorjaColors.White,
    unfocusedContainerColor = DorjaColors.White,
    focusedBorderColor = DorjaColors.Jol600,
    unfocusedBorderColor = DorjaColors.BentoCardBorder
)

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
    onListingCreated: (String) -> Unit,
    onScanRooms: ((String) -> Unit)? = null
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
    var countryCode by remember { mutableStateOf(repository.currentUser.value?.countryCode ?: "BD") }
    // Phase 4: emirate/state subdivision (drives document types via documentTypesFor)
    var subnationalCode by remember { mutableStateOf<String?>(null) }

    // Phase 3 liveability/energy evidence — shown only for profiles that expect it
    var energyClassText by remember { mutableStateOf("") }
    var energyIssuerText by remember { mutableStateOf("") }
    var heatingCostText by remember { mutableStateOf("") }
    var renovationYearText by remember { mutableStateOf("") }
    var powerBackupText by remember { mutableStateOf("") }
    var waterSupplyText by remember { mutableStateOf("") }
    var floodRiskText by remember { mutableStateOf("") }
    var buildingConditionText by remember { mutableStateOf("") }
    var buildingAgeText by remember { mutableStateOf("") }
    var disasterContextText by remember { mutableStateOf("") }

    val selectedTags = remember {
        mutableStateListOf<String>()
    }
    var customTagInput by remember { mutableStateOf("") }

    // 2. Rooms State (Empty by default - no fake rooms or scans)
    val customRooms = remember {
        mutableStateListOf<RoomItem>()
    }

    var createdListingId by remember { mutableStateOf<String?>(null) }

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

    // Crop state
    var showCropDialog by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropAssignedRoomId by remember { mutableStateOf<String?>(null) }
    var cropCaption by remember { mutableStateOf("") }


    // 5. Legal Documents (Empty by default - no fake documents)
    val customLegalDocs = remember {
        mutableStateListOf<LegalDocument>()
    }

    // 6. Promises
    val customPromises = remember {
        mutableStateListOf<Promise>()
    }

    var showAddDocDialog by remember { mutableStateOf(false) }
    var newDocType by remember {
        mutableStateOf(CountryRegistry.profile(repository.currentUser.value?.countryCode ?: "BD")
            .documentTypes.firstOrNull()?.code ?: "OTHER")
    }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocNumber by remember { mutableStateOf("") }
    var newDocAuthority by remember { mutableStateOf("") }
    var newDocYear by remember { mutableStateOf("2024") }
    var newDocNotes by remember { mutableStateOf("") }
    var newDocEvidenceLevel by remember { mutableStateOf(EvidenceLevel.SELF_DECLARED.code) }
    var newDocOfficialSourceUrl by remember { mutableStateOf("") }

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

    // Document types come from the selected country's profile (Phase 0: global adapters),
    // merged with the selected emirate/state's extra types (Phase 4: documentTypesFor).
    val activeProfile = CountryRegistry.profile(countryCode)
    val docTypeOptions = activeProfile.documentTypesFor(subnationalCode).map { Pair(it.code, it.label) }

    val commonAmenities = listOf(
        "Lift", "24/7 Generator", "Gas Connection", "Dedicated Parking",
        "CCTV & Security", "Rooftop Garden", "South Facing", "Pre-paid Meter",
        "High Speed Fiber", "Fire Safety", "Water Reservoir", "Solar Power", "Servant Room"
    )



    // CROP DIALOG (4:5 aspect ratio enforcement)
    if (showCropDialog && cropBitmap != null) {
        val bitmap = cropBitmap!!
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        val coroutineScope = rememberCoroutineScope()

        // 4:5 crop overlay dimensions
        val cropRatio = 4f / 5f

        Dialog(
            onDismissRequest = { showCropDialog = false; cropBitmap = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val cropCtx = LocalContext.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DorjaColors.Ink950)
            ) {
                // Image with drag-to-position
                val imageWidth = 360f
                val imageHeight = imageWidth * (bitmap.height.toFloat() / bitmap.width.toFloat())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo to crop",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4:5 Crop Overlay (darken outside, clear inside)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cropW = w * 0.85f
                    val cropH = cropW / cropRatio
                    val left = (w - cropW) / 2f
                    val top = (h - cropH) / 2f

                    // Darken outside
                    drawRect(Color.Black.copy(alpha = 0.6f))
                    // Clear inside
                    drawRect(Color.Transparent, topLeft = Offset(left, top), size = androidx.compose.ui.geometry.Size(cropW, cropH))
                    // Border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(cropW, cropH),
                        style = Stroke(2.dp.toPx())
                    )
                    // Corner accents
                    val cornerLen = 20.dp.toPx()
                    val corners = listOf(
                        Offset(left, top) to Offset(left + cornerLen, top),
                        Offset(left, top) to Offset(left, top + cornerLen),
                        Offset(left + cropW, top) to Offset(left + cropW - cornerLen, top),
                        Offset(left + cropW, top) to Offset(left + cropW, top + cornerLen),
                        Offset(left, top + cropH) to Offset(left + cornerLen, top + cropH),
                        Offset(left, top + cropH) to Offset(left, top + cropH - cornerLen),
                        Offset(left + cropW, top + cropH) to Offset(left + cropW - cornerLen, top + cropH),
                        Offset(left + cropW, top + cropH) to Offset(left + cropW, top + cropH - cornerLen)
                    )
                    corners.forEach { (from, to) -> drawLine(Color(0xFF00BCD4), from, to, 3.dp.toPx()) }
                }

                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 20.dp, end = 20.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = "Crop to 4:5 Ratio",
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Drag image to position within the frame",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Sand300,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bottom buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DorjaOutlinedButton(
                        text = "Cancel",
                        onClick = { showCropDialog = false; cropBitmap = null },
                        modifier = Modifier.weight(1f)
                    )
                    DorjaButton(
                        text = "Crop & Save",
                        onClick = {
                            // Auto-crop to center 4:5 region
                            val ratio = 4f / 5f
                            val cropW: Int
                            val cropH: Int
                            if (bitmap.width.toFloat() / bitmap.height.toFloat() > ratio) {
                                cropH = bitmap.height
                                cropW = (bitmap.height * ratio).toInt()
                            } else {
                                cropW = bitmap.width
                                cropH = (bitmap.width / ratio).toInt()
                            }
                            val x = (bitmap.width - cropW) / 2
                            val y = (bitmap.height - cropH) / 2
                            val cropped = Bitmap.createBitmap(bitmap, max(0, x), max(0, y), min(cropW, bitmap.width), min(cropH, bitmap.height))

                            val file = java.io.File(cropCtx.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                            java.io.FileOutputStream(file).use { out: java.io.FileOutputStream ->
                                cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                            }

                            photoAssignments.add(
                                PhotoAssignmentItem(
                                    url = "file://${file.absolutePath}",
                                    caption = cropCaption.ifBlank { "Photo ${photoAssignments.size + 1}" },
                                    assignedRoomId = cropAssignedRoomId
                                )
                            )

                            showCropDialog = false
                            cropBitmap = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // MULTI-PHOTO SELECTION MODAL
    if (showMultiPhotoSelectorDialog) {
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

                    // Room filter for assignment
                    if (customRooms.isNotEmpty()) {
                        Text(
                            text = "ASSIGN TO ROOM (optional)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DorjaChip(
                                selected = selectedRoomForNewPhoto == null,
                                label = "General / Cover",
                                onClick = { selectedRoomForNewPhoto = null }
                            )
                            customRooms.forEach { room ->
                                DorjaChip(
                                    selected = selectedRoomForNewPhoto == room.id,
                                    label = room.displayName,
                                    onClick = { selectedRoomForNewPhoto = room.id }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Caption input
                    OutlinedTextField(
                        value = customCaptionInput,
                        onValueChange = { customCaptionInput = it },
                        label = { Text("Photo Caption (optional)") },
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pick buttons — auto-crop to 4:5 silently (no dialog)
                    val ctx = LocalContext.current
                    val galleryLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            try {
                                // Decode bounds first to avoid OOM on huge images
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                                val sampleSize = (opts.outHeight / 2048).coerceAtLeast(1)
                                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                                val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                                if (bmp != null && !bmp.isRecycled) {
                                    val croppedFile = autoCropToFourFive(ctx, bmp)
                                    if (croppedFile != null) {
                                        photoAssignments.add(
                                            PhotoAssignmentItem(
                                                url = "file://${croppedFile.absolutePath}",
                                                caption = customCaptionInput.ifBlank { "Photo ${photoAssignments.size + 1}" },
                                                assignedRoomId = selectedRoomForNewPhoto
                                            )
                                        )
                                    }
                                }
                            } catch (_: Exception) {}
                            customCaptionInput = ""
                        }
                    }

                    val cameraLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicturePreview()
                    ) { bitmap ->
                        try {
                            if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                                val croppedFile = autoCropToFourFive(ctx, bitmap)
                                if (croppedFile != null) {
                                    photoAssignments.add(
                                        PhotoAssignmentItem(
                                            url = "file://${croppedFile.absolutePath}",
                                            caption = customCaptionInput.ifBlank { "Photo ${photoAssignments.size + 1}" },
                                            assignedRoomId = selectedRoomForNewPhoto
                                        )
                                    )
                                }
                            }
                        } catch (_: Exception) {
                            // Camera capture failed — silently ignore
                        } finally {
                            customCaptionInput = ""
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DorjaOutlinedButton(
                            text = "Gallery",
                            onClick = { galleryLauncher.launch("image/*") },
                            icon = Icons.Default.Image,
                            modifier = Modifier.weight(1f)
                        )
                        DorjaOutlinedButton(
                            text = "Camera",
                            onClick = { cameraLauncher.launch(null) },
                            icon = Icons.Default.CameraAlt,
                            modifier = Modifier.weight(1f)
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

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Evidence Level (honest status)", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(EvidenceLevel.entries.filter { it != EvidenceLevel.NOT_PROVIDED }) { level ->
                            DorjaChip(
                                selected = newDocEvidenceLevel == level.code,
                                label = level.label,
                                onClick = { newDocEvidenceLevel = level.code }
                            )
                        }
                    }
                    if (newDocEvidenceLevel == EvidenceLevel.SELF_DECLARED.code) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DorjaColors.BentoAmberIcon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Upload only — DORJA has not independently verified this document.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.BentoAmberIcon
                            )
                        }
                    }

                    // ── Government-source verification (Phase 2/4 authority rails) ──
                    // Subdivision-specific rail first (e.g. Dubai DLD), then country rail.
                    val activeRail = activeProfile.subnational(subnationalCode)?.let { sub ->
                        AuthorityLinks.railFor(sub.code, newDocType) ?: AuthorityLinks.railFor(countryCode, newDocType)
                    } ?: AuthorityLinks.railFor(countryCode, newDocType)
                    if (newDocEvidenceLevel == EvidenceLevel.GOVERNMENT_SOURCE_LINKED.code) {
                        if (activeRail != null) {
                            GovernmentSourceCard(
                                url = activeRail.url,
                                label = activeRail.label,
                                title = "Verify on ${activeRail.label}"
                            )
                        }
                        OutlinedTextField(
                            value = newDocOfficialSourceUrl,
                            onValueChange = { newDocOfficialSourceUrl = it },
                            label = { Text("Official Source Link (required)") },
                            placeholder = { Text("e.g. https://rera.mohua.gov.in/project/UPRERAPRJ12345") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DorjaColors.White,
                                unfocusedContainerColor = DorjaColors.White,
                                focusedBorderColor = DorjaColors.Jol600,
                                unfocusedBorderColor = DorjaColors.BentoCardBorder
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = DorjaColors.BentoAmberIcon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Green means you attached an official source reference — not that DORJA checked the registry itself.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.BentoAmberIcon
                            )
                        }
                        if (newDocOfficialSourceUrl.isBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DorjaColors.BentoAmberIcon, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Government-source-linked evidence requires the official link.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.BentoAmberIcon
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Document Title") },
                        placeholder = { Text(stringResource(id = R.string.document_title_placeholder)) },
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
                        placeholder = { Text(stringResource(id = R.string.authority_placeholder)) },
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
                        val numberToAdd = if (newDocNumber.isNotBlank()) newDocNumber else "DOC-${(1000..9999).random()}"
                        val authorityToAdd = if (newDocAuthority.isNotBlank()) newDocAuthority
                        else activeProfile.professionalRoles.lastOrNull() ?: "Authorized Sub-Registry Office"

                        val level = EvidenceLevel.fromCode(newDocEvidenceLevel)

                        // Atlas honesty rule: GOVERNMENT_SOURCE_LINKED requires an
                        // explicit official source reference (Phase 2 authority rails).
                        val govtUrl = newDocOfficialSourceUrl.trim()
                        if (level == EvidenceLevel.GOVERNMENT_SOURCE_LINKED && govtUrl.isBlank()) {
                            errorMessage = "Government-source-linked evidence requires the official source link."
                            return@DorjaButton
                        }

                        // Attach the source link to notes so it is visible in the
                        // doc list and in the exported Disclosure Pack.
                        val notesWithSource = if (govtUrl.isNotBlank()) {
                            if (newDocNotes.isNotBlank()) "${newDocNotes}\nOfficial source: $govtUrl"
                            else "Official source: $govtUrl"
                        } else newDocNotes

                        customLegalDocs.add(
                            LegalDocument(
                                id = "doc_" + UUID.randomUUID().toString().take(6),
                                listingId = "",
                                documentType = newDocType,
                                documentTitle = titleToAdd,
                                documentNumber = numberToAdd,
                                issuingAuthority = authorityToAdd,
                                issueDate = newDocYear,
                                verificationStatus = if (EvidenceLevel.isConfirmed(level)) "VERIFIED" else "PENDING_REVIEW",
                                notes = notesWithSource,
                                evidenceLevel = level.code,
                                checkedAt = if (level != EvidenceLevel.SELF_DECLARED) System.currentTimeMillis() else null,
                                limitationNote = if (level == EvidenceLevel.SELF_DECLARED) DEFAULT_SELF_DECLARED_NOTE else ""
                            )
                        )
                        newDocTitle = ""
                        newDocNumber = ""
                        newDocAuthority = ""
                        newDocNotes = ""
                        newDocOfficialSourceUrl = ""
                        newDocEvidenceLevel = EvidenceLevel.SELF_DECLARED.code
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
                            text = stringResource(id = R.string.host_suite_subtitle),
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

                    CountryPicker(
                        selected = countryCode,
                        onSelect = { code ->
                            countryCode = code
                            // Reset the subdivision when the country changes
                            val prof = CountryRegistry.profile(code)
                            subnationalCode = prof.subnationalProfiles.firstOrNull()?.code
                            if (newDocType !in prof.documentTypesFor(subnationalCode).map { it.code }) {
                                newDocType = prof.documentTypesFor(subnationalCode).firstOrNull()?.code ?: "OTHER"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Emirate / state picker — only for countries where transaction
                    // rules differ by subdivision (e.g. UAE emirates).
                    val activeSubnationalProfiles = activeProfile.subnationalProfiles
                    if (activeSubnationalProfiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${activeProfile.displayName.uppercase()} EMIRATE / STATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeSubnationalProfiles) { sub ->
                                DorjaChip(
                                    selected = subnationalCode == sub.code,
                                    label = sub.displayName,
                                    onClick = {
                                        subnationalCode = sub.code
                                        if (newDocType !in activeProfile.documentTypesFor(sub.code).map { it.code }) {
                                            newDocType = activeProfile.documentTypesFor(sub.code).firstOrNull()?.code ?: "OTHER"
                                        }
                                    }
                                )
                            }
                        }
                        val subNotes = activeProfile.subnational(subnationalCode)?.notes
                        if (!subNotes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = subNotes,
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray700
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
                        placeholder = "e.g. 3-Bed Luxury Apartment",
                        leadingIcon = Icons.Default.Home,
                        testTag = "input_listing_title"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price
                    DorjaInput(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = if (intent == "RENT") {
                            stringResource(id = R.string.rent_label, activeProfile.currencyCode)
                        } else {
                            stringResource(id = R.string.price_label, activeProfile.currencyCode)
                        },
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
                        placeholder = "e.g. Area / Neighborhood",
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Use My Location Button
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var locationLoading by remember { mutableStateOf(false) }
                    var lastFetchedLocation by remember { mutableStateOf("") }
                    var hasLocationPermission by remember {
                        mutableStateOf(
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context, android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    }
                    val locationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val granted = permissions.values.any { it }
                        hasLocationPermission = granted
                        if (granted) {
                            locationLoading = true
                            try {
                                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                @Suppress("MissingPermission")
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    locationLoading = false
                                    if (location != null) {
                                        val lat = String.format("%.4f", location.latitude)
                                        val lng = String.format("%.4f", location.longitude)
                                        lastFetchedLocation = "$lat, $lng"
                                        if (exactAddress.isBlank()) exactAddress = "GPS: $lat, $lng"
                                        if (publicArea.isBlank()) publicArea = "Nearby Location"
                                    } else {
                                        lastFetchedLocation = "Location unavailable"
                                    }
                                }.addOnFailureListener {
                                    locationLoading = false
                                    lastFetchedLocation = "Error getting location"
                                }
                            } catch (e: Exception) {
                                locationLoading = false
                                lastFetchedLocation = "Error: ${e.message}"
                            }
                        }
                    }

                    DorjaOutlinedButton(
                        text = if (locationLoading) "Getting Location..." else if (lastFetchedLocation.isNotBlank()) "📍 $lastFetchedLocation" else "Use My Current Location",
                        onClick = {
                            if (hasLocationPermission) {
                                locationLoading = true
                                try {
                                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                                    @Suppress("MissingPermission")
                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                        locationLoading = false
                                        if (location != null) {
                                            val lat = String.format("%.4f", location.latitude)
                                            val lng = String.format("%.4f", location.longitude)
                                            lastFetchedLocation = "$lat, $lng"
                                            if (exactAddress.isBlank()) exactAddress = "GPS: $lat, $lng"
                                            if (publicArea.isBlank()) publicArea = "Nearby Location"
                                        } else {
                                            lastFetchedLocation = "Location unavailable"
                                        }
                                    }.addOnFailureListener {
                                        locationLoading = false
                                        lastFetchedLocation = "Error getting location"
                                    }
                                } catch (e: Exception) {
                                    locationLoading = false
                                    lastFetchedLocation = "Error: ${e.message}"
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        icon = Icons.Default.LocationOn,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "use_my_location_button"
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
                            shape = RoundedCornerShape(12.dp),
                            color = DorjaColors.Sand100,
                            border = BorderStroke(1.5.dp, DorjaColors.BentoCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMultiPhotoSelectorDialog = true }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(DorjaColors.BentoBlueBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = DorjaColors.BentoBlueIcon, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No photos added yet", fontWeight = FontWeight.Bold, color = DorjaColors.Ink950, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Tap here to add photos and assign them to rooms", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
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

                                            // 4:5 ratio enforcement badge
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 6.dp),
                                                color = Color(0xFF00BCD4).copy(alpha = 0.85f),
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                Text(
                                                    text = "4:5",
                                                    color = DorjaColors.White,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                                                )
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
            // ______ 3. 3D PANORAMA SCANNER SECTION ______
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
                                    tint = Color(0xFF00BCD4),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "3. 3D PANORAMA SCANNER",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Capture 360° cylindrical panoramas for each room",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        DorjaButton(
                            text = "Scan Rooms",
                            onClick = {
                                val lid = createdListingId
                                if (lid != null && onScanRooms != null) {
                                    onScanRooms(lid)
                                }
                            },
                            icon = Icons.Default.ViewInAr,
                            modifier = Modifier.height(36.dp),
                            testTag = "scan_rooms_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (customRooms.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DorjaColors.Sand100,
                            border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Add rooms first, then scan", fontWeight = FontWeight.Bold, color = DorjaColors.Ink950)
                                Text("3D panoramas are captured per-room. Add rooms above first.", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customRooms.forEach { room ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (room.has3DScan) Color(0xFF00BCD4).copy(alpha = 0.08f) else DorjaColors.White,
                                    border = BorderStroke(1.dp, if (room.has3DScan) Color(0xFF00BCD4) else DorjaColors.BentoCardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (room.has3DScan) Color(0xFF00BCD4).copy(alpha = 0.15f) else DorjaColors.BentoBlueBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (room.has3DScan) Icons.Default.Check else Icons.Default.ViewInAr,
                                                contentDescription = null,
                                                tint = if (room.has3DScan) Color(0xFF00BCD4) else DorjaColors.Gray500,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(room.displayName, style = MaterialTheme.typography.titleSmall, color = DorjaColors.Ink950, fontWeight = FontWeight.Bold)
                                            Text(
                                                if (room.has3DScan) "3D Scan Ready" else "Scan Pending",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (room.has3DScan) Color(0xFF00BCD4) else DorjaColors.Gray500,
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (room.has3DScan) {
                                            DorjaBadge(text = "SCANNED", backgroundColor = Color(0xFF00BCD4).copy(alpha = 0.15f), textColor = Color(0xFF00BCD4))
                                        } else {
                                            DorjaBadge(text = "PENDING", backgroundColor = DorjaColors.Sand300.copy(alpha = 0.3f), textColor = DorjaColors.Gray700)
                                        }
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
                                text = stringResource(id = R.string.verified_listing_docs_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        DorjaButton(
                            text = "+ Add Doc",
                            onClick = {
                                newDocTitle = ""
                                newDocNumber = ""
                                newDocAuthority = ""                            newDocNotes = ""
                            newDocEvidenceLevel = EvidenceLevel.SELF_DECLARED.code
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
                                stringResource(id = R.string.no_docs_yet),
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
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                EvidenceBadge(
                                                    level = EvidenceLevel.fromCode(doc.evidenceLevel)
                                                )
                                            }
                                            Text(
                                                text = "No: ${doc.documentNumber} • ${doc.issuingAuthority}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DorjaColors.Gray500,
                                                fontSize = 11.sp
                                            )
                                            // Official source link (Phase 2 rails) — tap to open the portal
                                            doc.notes.lineSequence()
                                                .firstOrNull { it.startsWith("Official source: ") }
                                                ?.removePrefix("Official source: ")
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let { sourceUrl ->
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    GovernmentSourceCard(
                                                        url = sourceUrl,
                                                        label = "Official source",
                                                        compact = true
                                                    )
                                                }
                                            if (doc.limitationNote.isNotBlank()) {
                                                Text(
                                                    text = doc.limitationNote,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DorjaColors.BentoAmberIcon,
                                                    fontSize = 10.sp
                                                )
                                            }
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

            // Liveability & Energy Card (Phase 3) — fields come from the selected
            // country's profile; a market gets only the fields it expects.
            if (activeProfile.liveabilityFields.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "LIVEABILITY & ENERGY (${activeProfile.displayName.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        activeProfile.liveabilityFields.forEach { field ->
                            when (field) {
                                LiveabilityField.ENERGY_CLASS -> OutlinedTextField(
                                    value = energyClassText,
                                    onValueChange = { energyClassText = it },
                                    label = { Text(field.label + " (e.g. DPE: C)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.ENERGY_ISSUER -> OutlinedTextField(
                                    value = energyIssuerText,
                                    onValueChange = { energyIssuerText = it },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. Certifier name / diagnostics body") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.HEATING_COST -> OutlinedTextField(
                                    value = heatingCostText,
                                    onValueChange = { heatingCostText = it.filter { c -> c.isDigit() } },
                                    label = { Text("${field.label} (${activeProfile.currencySymbol}/year)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.RENOVATION_YEAR -> OutlinedTextField(
                                    value = renovationYearText,
                                    onValueChange = { renovationYearText = it.filter { c -> c.isDigit() }.take(4) },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. 2019") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.POWER_BACKUP -> OutlinedTextField(
                                    value = powerBackupText,
                                    onValueChange = { powerBackupText = it },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. IPS/UPS, generator for lift & common areas") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.WATER_SUPPLY -> OutlinedTextField(
                                    value = waterSupplyText,
                                    onValueChange = { waterSupplyText = it },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. WASA line + rooftop reservoir") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.FLOOD_RISK -> OutlinedTextField(
                                    value = floodRiskText,
                                    onValueChange = { floodRiskText = it },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. area waterlogs in monsoon; ground floor affected") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.BUILDING_CONDITION -> OutlinedTextField(
                                    value = buildingConditionText,
                                    onValueChange = { buildingConditionText = it },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. well-maintained 2015 reinforced-concrete building") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.BUILDING_AGE -> OutlinedTextField(
                                    value = buildingAgeText,
                                    onValueChange = { buildingAgeText = it.filter { c -> c.isDigit() }.take(3) },
                                    label = { Text("${field.label} (years)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                                LiveabilityField.DISASTER_CONTEXT -> OutlinedTextField(
                                    value = disasterContextText,
                                    onValueChange = { disasterContextText = it },
                                    label = { Text(field.label) },
                                    placeholder = { Text("e.g. outside mapped flood zone; 1981 seismic code or newer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = energyFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Promises Section
            var showAddPromiseDialog by remember { mutableStateOf(false) }
            var newPromiseCategory by remember { mutableStateOf("HANDOVER_DATE") }
            var newPromiseTitle by remember { mutableStateOf("") }
            var newPromiseText by remember { mutableStateOf("") }

            val promiseCategoryOptions = listOf(
                Pair("HANDOVER_DATE", "Handover Date"),
                Pair("UNIT_SIZE_OR_LAYOUT", "Unit Size / Layout"),
                Pair("PARKING", "Parking"),
                Pair("FIXTURES", "Fixtures & Fittings"),
                Pair("AMENITIES", "Amenities")
            )

            if (showAddPromiseDialog) {
                AlertDialog(
                    onDismissRequest = { showAddPromiseDialog = false },
                    title = { Text("Add Seller Promise", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Category", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray500)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                promiseCategoryOptions.take(3).forEach { (value, label) ->
                                    DorjaChip(
                                        selected = newPromiseCategory == value,
                                        label = label,
                                        onClick = { newPromiseCategory = value }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                promiseCategoryOptions.drop(3).forEach { (value, label) ->
                                    DorjaChip(
                                        selected = newPromiseCategory == value,
                                        label = label,
                                        onClick = { newPromiseCategory = value }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newPromiseTitle,
                                onValueChange = { newPromiseTitle = it },
                                label = { Text("Promise Title") },
                                placeholder = { Text("e.g. Handover by Q4 2026") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DorjaColors.White,
                                    unfocusedContainerColor = DorjaColors.White,
                                    focusedBorderColor = DorjaColors.Jol600,
                                    unfocusedBorderColor = DorjaColors.BentoCardBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newPromiseText,
                                onValueChange = { newPromiseText = it },
                                label = { Text("Commitment Description") },
                                placeholder = { Text("Formal contract clause or guarantee...") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
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
                            text = "Add Promise",
                            onClick = {
                                if (newPromiseTitle.isNotBlank()) {
                                    customPromises.add(
                                        Promise(
                                            id = "p_" + UUID.randomUUID().toString().take(8),
                                            listingId = "pending",
                                            category = newPromiseCategory,
                                            title = newPromiseTitle,
                                            originalText = newPromiseText.ifBlank { newPromiseTitle }
                                        )
                                    )
                                    newPromiseTitle = ""
                                    newPromiseText = ""
                                    showAddPromiseDialog = false
                                }
                            },
                            modifier = Modifier.width(130.dp)
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPromiseDialog = false }) {
                            Text("Cancel", color = DorjaColors.Gray700)
                        }
                    }
                )
            }

            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = DorjaColors.BentoGreenIcon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "5. SELLER PROMISES (${customPromises.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Binding commitments for the Handover Passport",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray700,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        DorjaOutlinedButton(
                            text = "+ Promise",
                            onClick = { showAddPromiseDialog = true },
                            modifier = Modifier.height(32.dp),
                            testTag = "add_promise_button"
                        )
                    }

                    if (customPromises.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customPromises.forEachIndexed { index, promise ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DorjaColors.Sand100,
                                    border = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DorjaColors.BentoGreenIcon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = promise.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = DorjaColors.Ink950,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = promise.category.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DorjaColors.Gray500,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { customPromises.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                tint = DorjaColors.Error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
                            legalDocs = customLegalDocs,
                            countryCode = countryCode,
                            subnationalCode = subnationalCode,
                            energyCertificateClass = energyClassText.trim().ifBlank { null },
                            energyCertificateIssuer = energyIssuerText.trim().ifBlank { null },
                            annualHeatingCost = heatingCostText.toLongOrNull(),
                            renovationYear = renovationYearText.toIntOrNull(),
                            powerBackup = powerBackupText.trim().ifBlank { null },
                            waterSupply = waterSupplyText.trim().ifBlank { null },
                            floodRisk = floodRiskText.trim().ifBlank { null },
                            buildingCondition = buildingConditionText.trim().ifBlank { null },
                            buildingAgeYears = buildingAgeText.toIntOrNull(),
                            disasterContext = disasterContextText.trim().ifBlank { null }
                        )
                        // Save promises for this listing
                        customPromises.forEach { promise ->
                            repository.addPromise(
                                listingId = newId,
                                category = promise.category,
                                title = promise.title,
                                originalText = promise.originalText,
                                evidenceNote = promise.evidenceNote
                            )
                        }
                        createdListingId = newId
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

/**
 * Auto-crops a bitmap to strict 4:5 ratio (center crop) and saves as JPEG.
 * Returns the saved file, or null on failure.
 */
private fun autoCropToFourFive(context: android.content.Context, bitmap: Bitmap): java.io.File? {
    return try {
        if (bitmap.width < 2 || bitmap.height < 2) return null

        val ratio = 4f / 5f
        val cropW: Int
        val cropH: Int
        if (bitmap.width.toFloat() / bitmap.height.toFloat() > ratio) {
            cropH = bitmap.height
            cropW = (bitmap.height * ratio).toInt()
        } else {
            cropW = bitmap.width
            cropH = (bitmap.width / ratio).toInt()
        }
        val safeW = cropW.coerceIn(2, bitmap.width)
        val safeH = cropH.coerceIn(2, bitmap.height)
        val x = ((bitmap.width - safeW) / 2).coerceAtLeast(0)
        val y = ((bitmap.height - safeH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(
            bitmap,
            x,
            y,
            min(safeW, bitmap.width - x),
            min(safeH, bitmap.height - y)
        )
        val file = java.io.File(context.cacheDir, "photo_4x5_${System.currentTimeMillis()}.jpg")
        java.io.FileOutputStream(file).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        file
    } catch (_: Exception) {
        null
    }
}

