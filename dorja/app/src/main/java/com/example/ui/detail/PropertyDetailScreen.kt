package com.example.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Balcony
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.R
import com.example.data.country.CountryRegistry
import androidx.compose.ui.res.stringResource
import com.example.data.model.RoomItem
import com.example.ui.components.BentoCard
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.components.SafeAddressShield
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import kotlinx.coroutines.launch

data class GalleryPhotoItem(
    val url: String,
    val caption: String,
    val roomName: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PropertyDetailScreen(
    listingId: String,
    onBack: () -> Unit,
    onOpen3DTour: (String) -> Unit,
    onOpenScanner: (String) -> Unit = {},
    onChatWithSeller: (String, String, String) -> Unit,
    onViewHandoverPassport: (String) -> Unit,

) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val listing by repository.observeListingById(listingId).collectAsState(initial = null)
    val rooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())
    val passport by repository.observePassportForListing(listingId).collectAsState(initial = null)
    val currentUser by repository.currentUser.collectAsState()

    var showVisitRequestDialog by remember { mutableStateOf(false) }
    var visitScheduledSuccess by remember { mutableStateOf(false) }
    var generatedPassToken by remember { mutableStateOf("") }
    var selectedRoomForDetail by remember { mutableStateOf<RoomItem?>(null) }
    var show3DTourDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    if (listing == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.CanvasBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Listing not found", color = DorjaColors.Ink950)
        }
        return
    }

    val safeListing = listing!!
    val isOwner = currentUser?.id == safeListing.ownerId && currentUser?.role == "SELLER"

    // Assemble high-quality photo list for buyer view
    val galleryPhotos = remember(safeListing, rooms) {
        val list = mutableListOf<GalleryPhotoItem>()
        if (!safeListing.coverPhotoUrl.isNullOrBlank()) {
            list.add(GalleryPhotoItem(safeListing.coverPhotoUrl, "Cover Photo", "Exterior / Main"))
        }
        rooms.forEach { room ->
            if (!room.photoPath.isNullOrBlank() && list.none { it.url == room.photoPath }) {
                list.add(GalleryPhotoItem(room.photoPath, room.displayName, room.displayName))
            }
        }
        list
    }

    val pagerState = rememberPagerState(pageCount = { galleryPhotos.size })
    val has3DScans = safeListing.hasScan || rooms.any { it.has3DScan } || !safeListing.virtualTourUrl.isNullOrBlank()

    // Full Screen Photo Modal
    if (fullScreenPhotoUrl != null) {
        AlertDialog(
            onDismissRequest = { fullScreenPhotoUrl = null },
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(16.dp),
            containerColor = DorjaColors.Ink950,
            text = {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f)) {
                    AsyncImage(
                        model = fullScreenPhotoUrl,
                        contentDescription = "Full Screen Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    IconButton(
                        onClick = { fullScreenPhotoUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DorjaColors.White.copy(alpha = 0.85f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = DorjaColors.Ink950)
                    }
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Close Preview",
                    onClick = { fullScreenPhotoUrl = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    // 3D Tour Modal Dialog
    if (show3DTourDialog) {
        val tourUrl = safeListing.virtualTourUrl ?: "https://dorja.bd/tours/${safeListing.id}"
        AlertDialog(
            onDismissRequest = { show3DTourDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = DorjaColors.Jol600,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("3D Virtual Tour", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Spatial walkthrough registered for '${safeListing.title}'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Ink950
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target Tour URL: $tourUrl",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Launch Tour Web",
                    onClick = {
                        show3DTourDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tourUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // fallback
                        }
                    },
                    modifier = Modifier.width(160.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { show3DTourDialog = false }) {
                    Text("Close", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // Room Detail Dialog
    if (selectedRoomForDetail != null) {
        val room = selectedRoomForDetail!!
        AlertDialog(
            onDismissRequest = { selectedRoomForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = DorjaColors.BentoBlueIcon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(room.displayName, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    if (!room.photoPath.isNullOrBlank()) {
                        AsyncImage(
                            model = room.photoPath,
                            contentDescription = room.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 5f)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Text(
                        text = "Category: ${room.roomType.replace("_", " ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace
                    )
                    if (room.dimensions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dimensions: ${room.dimensions}",
                            style = MaterialTheme.typography.titleSmall,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (room.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = room.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DorjaColors.Gray700
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (room.has3DScan) {
                        DorjaButton(
                            text = "View 3D Scan",
                            onClick = {
                                selectedRoomForDetail = null
                                onOpen3DTour(safeListing.id)
                            },
                            icon = Icons.Default.ViewInAr,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    DorjaOutlinedButton(
                        text = "Done",
                        onClick = { selectedRoomForDetail = null },
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        )
    }

    // SafeView Visit Request Dialog
    if (showVisitRequestDialog) {
        AlertDialog(
            onDismissRequest = { showVisitRequestDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = DorjaColors.BentoGreenIcon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Request SafeView Visit",
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "To protect both parties against fake listings and unvetted visitors, the exact address will unlock via QR pass only during your confirmed inspection slot.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Gray700
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PROPOSED INSPECTION WINDOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Today, 4:30 PM - 5:30 PM",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Location: ${safeListing.publicArea}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }
                    }
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Confirm & Generate Pass",
                    onClick = {
                        scope.launch {
                            val seekerId = currentUser?.id ?: "u2"
                            val viewing = repository.requestViewing(
                                listingId = safeListing.id,
                                seekerId = seekerId,
                                hostId = safeListing.ownerId,
                                startsAt = System.currentTimeMillis() + 1000 * 60 * 60,
                                endsAt = System.currentTimeMillis() + 1000 * 60 * 120
                            )
                            generatedPassToken = viewing.passToken
                            showVisitRequestDialog = false
                            visitScheduledSuccess = true
                        }
                    },
                    modifier = Modifier.width(200.dp),
                    testTag = "confirm_visit_request_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showVisitRequestDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // Success Confirmation Dialog
    if (visitScheduledSuccess) {
        AlertDialog(
            onDismissRequest = { visitScheduledSuccess = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DorjaColors.BentoGreenIcon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Viewing Pass Issued",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Your SafeView pass ($generatedPassToken) has been created and saved under your Visits tab. Present the QR token to the host upon arrival.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DorjaColors.Gray700
                )
            },
            confirmButton = {
                DorjaButton(
                    text = "Great",
                    onClick = { visitScheduledSuccess = false },
                    modifier = Modifier.width(120.dp)
                )
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("property_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Top Hero Photo Carousel or Compact Architectural Header
            if (galleryPhotos.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
                        .background(DorjaColors.Ink950)
                ) {
                    // Swipeable / Scrollable High-Res Photo Gallery (full 4:5 display)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val photo = galleryPhotos[page]
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = photo.url,
                                contentDescription = photo.caption,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { fullScreenPhotoUrl = photo.url }
                            )

                            // Subtle dark gradient overlay at top & bottom for high text contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                        }
                    }

                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(top = 40.dp, start = 12.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DorjaColors.White.copy(alpha = 0.9f))
                            .align(Alignment.TopStart)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DorjaColors.Ink950
                        )
                    }

                    // Photo Index Pill & Room Label
                    val currentPhoto = galleryPhotos.getOrNull(pagerState.currentPage)
                    Surface(
                        modifier = Modifier
                            .padding(top = 40.dp, end = 12.dp)
                            .align(Alignment.TopEnd),
                        shape = RoundedCornerShape(16.dp),
                        color = DorjaColors.Ink950.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, DorjaColors.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = DorjaColors.Sand300,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${pagerState.currentPage + 1} / ${galleryPhotos.size}" + (currentPhoto?.roomName?.let { " • $it" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Intent & 3D Tour Badges on bottom
                    Row(
                        modifier = Modifier
                            .padding(bottom = 10.dp, start = 12.dp, end = 12.dp)
                            .fillMaxWidth()
                            .align(Alignment.BottomStart),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DorjaBadge(
                            text = if (safeListing.intent == "RENT") "FOR RENT" else "FOR SALE",
                            backgroundColor = if (safeListing.intent == "RENT") DorjaColors.BentoBlueBg else DorjaColors.BentoPurpleBg,
                            textColor = if (safeListing.intent == "RENT") DorjaColors.BentoBlueText else DorjaColors.BentoPurpleText
                        )

                        if (has3DScans) {
                            DorjaBadge(
                                text = "3D TOUR READY",
                                icon = Icons.Default.ViewInAr,
                                backgroundColor = DorjaColors.Jol600,
                                textColor = DorjaColors.White
                            )
                        }
                    }
                }

                // Thumbnail Strip below Hero for quick photo switching
                if (galleryPhotos.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(galleryPhotos.indices.toList()) { index ->
                            val photo = galleryPhotos[index]
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(width = 60.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) DorjaColors.Jol600 else DorjaColors.BentoCardBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = photo.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            } else {
                // Compact Blueprint Header (No fake photos)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DorjaColors.Ink950)
                        .padding(top = 40.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.White.copy(alpha = 0.15f))
                                    .testTag("detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DorjaColors.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = safeListing.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DorjaColors.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = safeListing.publicArea,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Sand300
                                )
                            }
                        }

                        if (has3DScans) {
                            DorjaBadge(
                                text = "3D READY",
                                icon = Icons.Default.ViewInAr,
                                backgroundColor = DorjaColors.Jol600,
                                textColor = DorjaColors.White
                            )
                        }
                    }
                }
            }

            // Body Details
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title and Price Bento Card
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = safeListing.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatPrice(safeListing.priceAmount, safeListing.currency, safeListing.intent),
                            style = MaterialTheme.typography.titleMedium,
                            color = DorjaColors.Jol600,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // SafeView Protected Address Shield
                        SafeAddressShield(approximateArea = safeListing.publicArea)

                        // Open in Google Maps button
                        Spacer(modifier = Modifier.height(6.dp))
                        BentoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val lat = safeListing.approximateLat ?: 23.8041
                                    val lng = safeListing.approximateLng ?: 90.3468
                                    val uri = Uri.parse("https://www.google.com/maps?q=$lat,$lng")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = DorjaColors.BentoBlueIcon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Open Location in Google Maps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DorjaColors.BentoBlueText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (isOwner && safeListing.exactAddress.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DorjaColors.BentoBlueBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Host Confidential Address: ${safeListing.exactAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.BentoBlueText,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // Key Specs Grid Bento Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpecPill(icon = Icons.Default.Bed, label = "${safeListing.bedrooms} Beds", modifier = Modifier.weight(1f))
                    SpecPill(icon = Icons.Default.Bathtub, label = "${safeListing.bathrooms} Baths", modifier = Modifier.weight(1f))
                    SpecPill(icon = Icons.Default.Balcony, label = "${safeListing.balconies} Balconies", modifier = Modifier.weight(1f))
                    SpecPill(icon = Icons.Default.SquareFoot, label = "${safeListing.sqft} Sqft", modifier = Modifier.weight(1f))
                }

                // BUYER REQUIREMENT: If 3D scans exist, the FIRST thing in the buyer menu is "SEE 3D SCANS"
                if (has3DScans) {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen3DTour(safeListing.id) }
                            .testTag("see_3d_scans_menu_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DorjaColors.Jol600),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = "3D Scan",
                                        tint = DorjaColors.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "3D Virtual Reality Tour",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DorjaColors.Ink950,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val scannedCount = rooms.count { it.has3DScan }
                                    Text(
                                        text = if (scannedCount > 0) "$scannedCount Scanned Rooms Available" else "Spatial Walkthrough Available",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Jol600,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            DorjaButton(
                                text = "See 3D Scans",
                                onClick = { onOpen3DTour(safeListing.id) },
                                icon = Icons.Default.ViewInAr,
                                modifier = Modifier.height(34.dp),
                                testTag = "see_3d_scans_hero_button"
                            )
                        }
                    }
                }

                // HOST: Scan 3D Rooms button (only visible to owner)
                if (isOwner) {
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenScanner(safeListing.id) }
                            .testTag("host_scan_3d_rooms_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(androidx.compose.ui.graphics.Color(0xFF00BCD4).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = "Scan 3D",
                                        tint = androidx.compose.ui.graphics.Color(0xFF00BCD4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Scan 3D Rooms",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DorjaColors.Ink950,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Capture 360° panoramas with gyroscope guidance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color(0xFF00BCD4),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            DorjaButton(
                                text = "Start Scan",
                                onClick = { onOpenScanner(safeListing.id) },
                                icon = Icons.Default.ViewInAr,
                                modifier = Modifier.height(34.dp),
                                testTag = "host_start_scan_button"
                            )
                        }
                    }
                }

                // Rooms Showcase Bento Card with Photos
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Room-by-Room Details & Photos",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${rooms.size} rooms documented",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray500,
                                    fontSize = 11.sp
                                )
                            }


                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (rooms.isEmpty()) {
                            Text(
                                text = "No individual rooms documented yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                rooms.forEach { room ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRoomForDetail = room },
                                        shape = RoundedCornerShape(8.dp),
                                        color = DorjaColors.White,
                                        border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!room.photoPath.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = room.photoPath,
                                                    contentDescription = room.displayName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(DorjaColors.BentoBlueBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.MeetingRoom,
                                                        contentDescription = null,
                                                        tint = DorjaColors.BentoBlueIcon,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = room.displayName,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = DorjaColors.Ink950,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    if (room.has3DScan) {
                                                        DorjaBadge(
                                                            text = "3D SCAN",
                                                            icon = Icons.Default.CheckCircle,
                                                            backgroundColor = DorjaColors.BentoGreenBg,
                                                            textColor = DorjaColors.BentoGreenText
                                                        )
                                                    }
                                                }
                                                if (room.dimensions.isNotBlank()) {
                                                    Text(
                                                        text = "Dimensions: ${room.dimensions}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DorjaColors.Gray500,
                                                        fontSize = 11.sp
                                                    )
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
                                            }

                                            if (room.has3DScan && !isOwner) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                DorjaOutlinedButton(
                                                    text = "3D",
                                                    onClick = { onOpen3DTour(safeListing.id) },
                                                    icon = Icons.Default.ViewInAr,
                                                    modifier = Modifier.height(30.dp),
                                                    testTag = "detail_room_3d_btn_${room.id}"
                                                )

                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // About Property Bento Card
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "About this property",
                            style = MaterialTheme.typography.titleSmall,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (safeListing.description.isNotBlank()) safeListing.description else "Verified residential unit listed on Dorja.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DorjaColors.Gray700,
                            lineHeight = 20.sp,
                            fontSize = 13.sp
                        )
                    }
                }

                // Amenities & Features Bento Card
                if (safeListing.tags.isNotBlank()) {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Amenities & Features",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                safeListing.tags.split(",").forEach { tag ->
                                    if (tag.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DorjaColors.Sand100,
                                            border = BorderStroke(1.dp, DorjaColors.Sand300)
                                        ) {
                                            Text(
                                                text = tag.trim(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DorjaColors.Ink950,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Handover Passport Bento Card
                BentoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewHandoverPassport(safeListing.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DorjaColors.BentoGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = DorjaColors.BentoGreenIcon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Digital Handover Passport",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.handover_passport_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700,
                                fontSize = 11.sp
                            )
                            if (passport != null) {
                                Text(
                                    text = "Property Passport ${passport!!.id.uppercase()} • ${CountryRegistry.profile(passport!!.countryCode).displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.BentoGreenIcon,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "View",
                            tint = DorjaColors.BentoGreenIcon
                        )
                    }
                }
            }
        }

        // Fixed Bottom Action Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = DorjaColors.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwner) {
                    DorjaButton(
                        text = "Delete Listing",
                        onClick = {
                            scope.launch {
                                repository.deleteListing(safeListing.id)
                                onBack()
                            }
                        },
                        icon = Icons.Default.Delete,
                        modifier = Modifier.weight(1f),
                        testTag = "delete_listing_button"
                    )
                } else {
                    // Chat with Host
                    DorjaOutlinedButton(
                        text = "Chat with Host",
                        onClick = {
                            val seekerId = currentUser?.id ?: "u2"
                            onChatWithSeller(safeListing.id, seekerId, safeListing.ownerId)
                        },
                        icon = Icons.AutoMirrored.Filled.Chat,
                        modifier = Modifier.weight(1f),
                        testTag = "chat_with_seller_button"
                    )

                    // Request SafeView Visit
                    DorjaButton(
                        text = "Book Visit",
                        onClick = { showVisitRequestDialog = true },
                        icon = Icons.Default.CalendarMonth,
                        modifier = Modifier.weight(1.2f),
                        testTag = "request_visit_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecPill(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = DorjaColors.White,
        border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DorjaColors.BentoBlueIcon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}
