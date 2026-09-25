package com.example.ui.detail

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Balcony
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.R
import com.example.data.country.CountryRegistry
import com.example.data.country.LiveabilityField
import com.example.ui.util.DisclosurePackExporter
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.data.model.AppealRecord
import com.example.data.model.EvidenceLevel
import com.example.data.model.LegalDocument
import com.example.data.model.Report
import com.example.data.model.ReportResponse
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.components.EvidenceBadge
import com.example.ui.components.SafeAddressShield
import com.example.data.model.ReportReason
import com.example.ui.negotiation.ConflictCard
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.filled.Mic
import com.example.ai.PropertyAiContext
import com.example.ai.VoiceAssistantHelper
import com.example.ui.ai.HeyDorjaAssistantSheet
import com.example.ui.components.DorjaLogo
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.util.Formatters
import kotlinx.coroutines.launch
import java.io.File

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
    /** Invoked when the AI sheet's "Go to Settings" asks for the Settings tab. */
    onOpenSettingsTab: () -> Unit = {},
    /** Buyer-only: opens the landscape split-screen comparison stage. */
    onOpenCompare: () -> Unit = {},

) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val listing by repository.observeListingById(listingId).collectAsState(initial = null)
    val rooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())
    val passport by repository.observePassportForListing(listingId).collectAsState(initial = null)
    val endorsements by repository.observeEndorsementsForListing(listingId).collectAsState(initial = emptyList())
    val legalDocs by repository.getLegalDocumentsByListing(listingId).collectAsState(initial = emptyList())
    val promises by repository.getPromisesByListing(listingId).collectAsState(initial = emptyList())
    val listingReports by repository.observeReportsForListing(listingId).collectAsState(initial = emptyList<Report>())
    val reportResponsesById = listingReports.associate { report ->
        report.id to repository.observeResponsesForReport(report.id).collectAsState(initial = emptyList<ReportResponse>()).value
    }
    val reportAppealsById = listingReports.associate { report ->
        report.id to repository.observeAppealsForReport(report.id).collectAsState(initial = emptyList<AppealRecord>()).value
    }
    val currentUser by repository.currentUser.collectAsState()

    var showHeyDorjaSheet by remember { mutableStateOf(false) }

    val propertyAiContext = remember(listing, rooms, passport, legalDocs, promises, endorsements) {
        listing?.let {
            PropertyAiContext.from(
                listing = it,
                rooms = rooms,
                passport = passport,
                documents = legalDocs,
                promises = promises,
                endorsements = endorsements
            )
        }
    }

    // "Hey Dorja" Hotword and Voice Trigger
    val voiceHelper = remember { VoiceAssistantHelper(context) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { granted ->
        hasAudioPermission = granted
        if (granted) {
            voiceHelper.startListening(onResult = { recognized ->
                val lower = recognized.lowercase()
                if (matchesWakeWord(lower)) {
                    showHeyDorjaSheet = true
                }
            })
        }
    }

    DisposableEffect(hasAudioPermission) {
        if (hasAudioPermission && voiceHelper.isAvailable()) {
            voiceHelper.startListening(onResult = { recognized ->
                val lower = recognized.lowercase()
                if (matchesWakeWord(lower)) {
                    showHeyDorjaSheet = true
                }
            })
        }
        onDispose {
            voiceHelper.stopListening()
        }
    }

    var showVisitRequestDialog by remember { mutableStateOf(false) }
    var visitScheduledSuccess by remember { mutableStateOf(false) }
    var generatedPassToken by remember { mutableStateOf("") }
    var selectedRoomForDetail by remember { mutableStateOf<RoomItem?>(null) }
    var show3DTourDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }
    var exportingPack by remember { mutableStateOf(false) }
    var showEndorsementDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showRespondDialog by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf(false) }
    var showAppealDialog by remember { mutableStateOf(false) }
    var showDocUploadSheet by remember { mutableStateOf(false) }
    var resolveReportId by remember { mutableStateOf("") }
    var appealReportId by remember { mutableStateOf("") }
    var resolveNote by remember { mutableStateOf("") }
    var appealGrounds by remember { mutableStateOf("") }
    var docTypeInput by remember { mutableStateOf("") }
    var docNumberInput by remember { mutableStateOf("") }
    var docAuthorityInput by remember { mutableStateOf("") }
    var pickedDocUri by remember { mutableStateOf<Uri?>(null) }
    var docBusy by remember { mutableStateOf(false) }
    var docPendingDelete by remember { mutableStateOf<LegalDocument?>(null) }

    // Report dialog state (Phase 5)
    var reportReason by remember { mutableStateOf(ReportReason.INACCURATE_CLAIM.code) }
    var reportDetails by remember { mutableStateOf("") }
    var reportClaim by remember { mutableStateOf("") }
    // Respond dialog state
    var respondReportId by remember { mutableStateOf("") }
    var respondClaim by remember { mutableStateOf("") }
    var respondEvidence by remember { mutableStateOf("") }

    // Professional handoff dialog state
    var endSection by remember { mutableStateOf("OWNERSHIP") }
    var endName by remember { mutableStateOf("") }
    var endLicence by remember { mutableStateOf("") }
    // Section-specific detail fields — labels change with the endorsed section.
    var endField1 by remember { mutableStateOf("") }
    var endField2 by remember { mutableStateOf("") }
    var endField3 by remember { mutableStateOf("") }

    // Appeal awaiting a decision (owner) — set from a ConflictCard action
    var pendingAppealDecision by remember { mutableStateOf<Pair<String, String>?>(null) }

    val legalDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // The file is copied into app storage when the upload sheet is saved,
        // so the document keeps working after app restarts.
        if (uri != null) {
            pickedDocUri = uri
            showDocUploadSheet = true
        }
    }

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

    // List scroll state drives the parallax hero and the scroll-aware action bar.
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 240 }
    }
    val heroParallax by remember {
        derivedStateOf { if (galleryPhotos.isEmpty()) 0f else listState.firstVisibleItemScrollOffset * 0.5f }
    }
    val heroAlpha by remember {
        derivedStateOf { (1f - listState.firstVisibleItemScrollOffset / 600f).coerceIn(0f, 1f) }
    }

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

    // Professional Endorsement Dialog (Phase 4)
    if (showEndorsementDialog) {
        val sectionOptions = listOf(
            "OWNERSHIP" to "Ownership & title",
            "CONDITION" to "Building condition",
            "MEASUREMENTS" to "Measurements & boundaries",
            "DISCLOSURE" to "Disclosure completeness",
            "ENERGY" to "Energy / running costs"
        )
        // Each section asks for the details that matter for that document type.
        // Triple: (label, placeholder, optional)
        val sectionFields: Map<String, List<Triple<String, String, Boolean>>> = mapOf(
            "OWNERSHIP" to listOf(
                Triple("Deed / Title Number", "e.g. LR-2024-0087142", false),
                Triple("Issuing Authority", "e.g. Sub-registry office, Dhaka", false),
                Triple("Verification URL", "https://…", true)
            ),
            "CONDITION" to listOf(
                Triple("Inspection Date", "e.g. 2026-09-14", false),
                Triple("Report Reference", "e.g. INSP-5521", false),
                Triple("Defects Noted", "e.g. seepage in NW wall", true)
            ),
            "MEASUREMENTS" to listOf(
                Triple("Measured Area (m²)", "e.g. 116.2", false),
                Triple("Measurement Standard", "e.g. RERA carpet area", false),
                Triple("Instrument Used", "e.g. laser disto", true)
            ),
            "DISCLOSURE" to listOf(
                Triple("Checklist Completed On", "e.g. 2026-09-10", false),
                Triple("Items Not Disclosed", "e.g. none / list items", false),
                Triple("Shared With", "e.g. buyer + tenant", true)
            ),
            "ENERGY" to listOf(
                Triple("Certificate Class", "e.g. B / 3", false),
                Triple("Issuing Body", "e.g. DECC certified assessor", false),
                Triple("Valid Until", "e.g. 2031-06", true)
            )
        )
        val fieldsForSection = sectionFields[endSection] ?: emptyList()
        val fieldValueFor: (Int) -> String = { i -> when (i) { 0 -> endField1; 1 -> endField2; else -> endField3 } }
        val allRequiredFilled = endName.isNotBlank() && endLicence.isNotBlank() &&
            fieldsForSection.withIndex().filter { !it.value.third }.all { (position, _) ->
                fieldValueFor(position).isNotBlank()
            }
        AlertDialog(
            onDismissRequest = { showEndorsementDialog = false },
            icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = DorjaColors.BentoPurpleIcon) },
            title = { Text("Professional Endorsement", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The professional takes responsibility for one section. DORJA records who, when, and for what — it does not verify the licence.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500
                    )
                    Text("Section", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sectionOptions.forEach { (code, label) ->
                            DorjaChip(
                                selected = endSection == code,
                                label = label,
                                onClick = {
                                    endSection = code
                                    endField1 = ""; endField2 = ""; endField3 = ""
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = endName,
                        onValueChange = { endName = it },
                        label = { Text("Professional Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endLicence,
                        onValueChange = { endLicence = it },
                        label = { Text("Licence / Registration ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // Section-specific detail fields (labels adapt to the section)
                    fieldsForSection.forEachIndexed { index, (label, placeholder, optional) ->
                        val fieldValue = when (index) {
                            0 -> endField1; 1 -> endField2; else -> endField3
                        }
                        val onFieldChange: (String) -> Unit = { v ->
                            when (index) {
                                0 -> endField1 = v; 1 -> endField2 = v; else -> endField3 = v
                            }
                        }
                        OutlinedTextField(
                            value = fieldValue,
                            onValueChange = onFieldChange,
                            label = { Text(if (optional) "$label (optional)" else label) },
                            placeholder = { Text(placeholder) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Record Endorsement",
                    onClick = {
                        if (allRequiredFilled) {
                            val details = fieldsForSection.mapIndexedNotNull { index, (label, _, optional) ->
                                val v = when (index) {
                                    0 -> endField1; 1 -> endField2; else -> endField3
                                }.trim()
                                if (v.isNotBlank()) "$label: $v" else null
                            }
                            scope.launch {
                                repository.addEndorsement(
                                    listingId = safeListing.id,
                                    section = endSection,
                                    professionalName = endName.trim(),
                                    licenceId = endLicence.trim(),
                                    roleLabel = CountryRegistry.profile(safeListing.countryCode)
                                        .professionalRoles.firstOrNull() ?: "Licensed professional",
                                    statement = details.joinToString(" • ")
                                )
                                endName = ""; endLicence = ""
                                endField1 = ""; endField2 = ""; endField3 = ""
                                showEndorsementDialog = false
                            }
                        }
                    },
                    enabled = allRequiredFilled
                )
            },
            dismissButton = {
                TextButton(onClick = { showEndorsementDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // Report a Problem dialog (Phase 5, atlas §2)
    if (showReportDialog) {
        val reasonOptions = ReportReason.entries.toList()
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = DorjaColors.BentoAmberIcon) },
            title = { Text("Report a Problem", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This creates a neutral record. The host can respond and both claims remain visible to everyone — DORJA does not judge truth.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500
                    )
                    Text("Reason", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        reasonOptions.forEach { reason ->
                            DorjaChip(
                                selected = reportReason == reason.code,
                                label = reason.label,
                                onClick = { reportReason = reason.code }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reportClaim,
                        onValueChange = { reportClaim = it },
                        label = { Text("What do you say is true?") },
                        placeholder = { Text("e.g. The listing states 1250 sqft but the flat is visibly smaller.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        label = { Text("Additional details (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Submit Report",
                    onClick = {
                        if (reportClaim.isNotBlank()) {
                            scope.launch {
                                repository.addReport(
                                    listingId = safeListing.id,
                                    reportedByUserId = currentUser?.id ?: "",
                                    reason = reportReason,
                                    details = reportDetails.trim(),
                                    subjectClaim = reportClaim.trim()
                                )
                                reportReason = ReportReason.INACCURATE_CLAIM.code
                                reportDetails = ""; reportClaim = ""
                                showReportDialog = false
                            }
                        }
                    },
                    enabled = reportClaim.isNotBlank()
                )
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // Respond to a Report dialog (host side, Phase 5)
    if (showRespondDialog) {
        val openReports = listingReports.filter { it.state == "OPEN" || it.state == "COUNTERPARTY_RESPONDED" }
        AlertDialog(
            onDismissRequest = { showRespondDialog = false },
            icon = { Icon(Icons.Default.Balance, contentDescription = null, tint = DorjaColors.BentoBlueIcon) },
            title = { Text("Respond to a Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (openReports.isEmpty()) {
                        Text("No open reports on this listing.", color = DorjaColors.Gray700)
                    } else {
                        Text("Select report", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            openReports.forEach { report ->
                                DorjaChip(
                                    selected = respondReportId == report.id,
                                    label = ReportReason.fromCode(report.reason).label,
                                    onClick = { respondReportId = report.id }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = respondClaim,
                            onValueChange = { respondClaim = it },
                            label = { Text("Your side — what do you say is true?") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = respondEvidence,
                            onValueChange = { respondEvidence = it },
                            label = { Text("Evidence reference (document no., promise, deed clause…)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Submit Response",
                    onClick = {
                        if (respondReportId.isNotBlank() && respondClaim.isNotBlank()) {
                            scope.launch {
                                repository.addReportResponse(
                                    reportId = respondReportId,
                                    respondedByUserId = currentUser?.id ?: safeListing.ownerId,
                                    counterClaim = respondClaim.trim(),
                                    evidenceReference = respondEvidence.trim()
                                )
                                respondReportId = ""; respondClaim = ""; respondEvidence = ""
                                showRespondDialog = false
                            }
                        }
                    },
                    enabled = respondReportId.isNotBlank() && respondClaim.isNotBlank()
                )
            },
            dismissButton = {
                TextButton(onClick = { showRespondDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ── Resolve a dispute (owner) ──
    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = DorjaColors.Teal900) },
            title = { Text("Record a Resolution", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Describe how the claim conflict was settled. The note is recorded neutrally on both sides' records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = resolveNote,
                        onValueChange = { resolveNote = it },
                        label = { Text("Resolution note") },
                        placeholder = { Text("e.g. Seller supplied the up-to-date khatian; claim withdrawn by reporter.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Mark Resolved",
                    enabled = resolveNote.isNotBlank(),
                    onClick = {
                        scope.launch {
                            repository.resolveReport(resolveReportId, resolveNote.trim())
                            resolveNote = ""; resolveReportId = ""
                            showResolveDialog = false
                        }
                    },
                    modifier = Modifier.widthIn(min = 140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ── Appeal a closed outcome (either party) ──
    if (showAppealDialog) {
        AlertDialog(
            onDismissRequest = { showAppealDialog = false },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = DorjaColors.BentoPurpleIcon) },
            title = { Text("Appeal the Outcome", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Appeals stay on the record. The listing owner reviews the grounds and upholds or overturns the outcome.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = appealGrounds,
                        onValueChange = { appealGrounds = it },
                        label = { Text("Grounds for appeal") },
                        placeholder = { Text("e.g. The resolution ignored the sub-registry receipt I attached.") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Submit Appeal",
                    enabled = appealGrounds.isNotBlank(),
                    onClick = {
                        scope.launch {
                            repository.addAppeal(
                                reportId = appealReportId,
                                appealedByUserId = currentUser?.id ?: safeListing.ownerId,
                                grounds = appealGrounds.trim()
                            )
                            appealGrounds = ""; appealReportId = ""
                            showAppealDialog = false
                        }
                    },
                    modifier = Modifier.widthIn(min = 140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showAppealDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ── Decide a submitted appeal (owner) ──
    pendingAppealDecision?.let { (reportId, appealId) ->
        AlertDialog(
            onDismissRequest = { pendingAppealDecision = null },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = DorjaColors.BentoPurpleIcon) },
            title = { Text("Decide Appeal", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Uphold keeps the recorded outcome. Overturn reopens the dispute for a new response and resolution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DorjaButton(
                        text = "Uphold",
                        onClick = {
                            scope.launch {
                                repository.decideAppeal(reportId, appealId, upheld = true, decisionNote = "Outcome stands after review")
                                pendingAppealDecision = null
                            }
                        },
                        modifier = Modifier.widthIn(min = 100.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository.decideAppeal(reportId, appealId, upheld = false, decisionNote = "Outcome overturned — dispute reopened")
                                repository.reopenReport(reportId)
                                pendingAppealDecision = null
                            }
                        }
                    ) {
                        Text("Overturn & reopen", color = DorjaColors.Gray700)
                    }
                }
            }
        )
    }

    // ── Document upload sheet — file + metadata, copied into app storage ──
    if (showDocUploadSheet) {
        AlertDialog(
            onDismissRequest = { if (!docBusy) showDocUploadSheet = false },
            icon = { Icon(Icons.Default.Description, contentDescription = null, tint = DorjaColors.Jol600) },
            title = { Text("Attach a Document", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The file is copied into DORJA's private storage so this record survives app restarts. It is labelled self-declared until a professional or issuer confirms it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DorjaColors.Sand100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !docBusy) { legalDocPicker.launch("*/*") }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DorjaColors.Jol600, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pickedDocUri?.let { uri ->
                                    queryDocDisplayName(context, uri) ?: "File selected"
                                } ?: "Choose a file (PDF, image, any type)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (pickedDocUri != null) DorjaColors.Ink950 else DorjaColors.Gray700,
                                fontWeight = if (pickedDocUri != null) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TITLE_DEED", "KHATIAN", "COMPLETION_CERT", "SATHEEN", "MUTATION", "OTHER").forEach { type ->
                            DorjaChip(
                                selected = docTypeInput == type,
                                label = type.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                                onClick = { docTypeInput = type }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = docNumberInput,
                        onValueChange = { docNumberInput = it },
                        label = { Text("Document / deed number (optional)") },
                        placeholder = { Text("e.g. KHT-DHN-8849/2018") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = docAuthorityInput,
                        onValueChange = { docAuthorityInput = it },
                        label = { Text("Issuing authority (optional)") },
                        placeholder = { Text("e.g. Sub-registry office, Dhaka") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = if (docBusy) "Saving…" else "Attach",
                    enabled = pickedDocUri != null && !docBusy,
                    onClick = {
                        val uri = pickedDocUri ?: return@DorjaButton
                        docBusy = true
                        scope.launch {
                            val result = repository.addLegalDocument(
                                listingId = listingId,
                                uri = uri,
                                documentType = docTypeInput.ifBlank { "UNKNOWN" },
                                documentNumber = docNumberInput.trim(),
                                issuingAuthority = docAuthorityInput.trim()
                            )
                            docBusy = false
                            if (result.isSuccess) {
                                pickedDocUri = null; docTypeInput = ""; docNumberInput = ""; docAuthorityInput = ""
                                showDocUploadSheet = false
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Could not attach the file. Try a different one.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDocUploadSheet = false },
                    enabled = !docBusy
                ) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ── Delete document confirmation ──
    docPendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docPendingDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DorjaColors.Error) },
            title = { Text("Delete Document", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${doc.documentTitle}\" and its stored file will be removed from this listing's record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )
            },
            confirmButton = {
                DorjaButton(
                    text = "Delete",
                    containerColor = DorjaColors.Error,
                    onClick = {
                        scope.launch {
                            repository.deleteLegalDocument(doc.id)
                            docPendingDelete = null
                        }
                    },
                    modifier = Modifier.widthIn(min = 110.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { docPendingDelete = null }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
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
                    modifier = Modifier.widthIn(min = 160.dp)
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
                            modifier = Modifier.widthIn(min = 140.dp)
                        )
                    }
                    DorjaOutlinedButton(
                        text = "Done",
                        onClick = { selectedRoomForDetail = null },
                        modifier = Modifier.widthIn(min = 80.dp)
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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = DorjaColors.Paper50,
                        border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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
                            val seekerId = currentUser?.id ?: ""
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
                    modifier = Modifier.widthIn(min = 200.dp),
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
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            }
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    //  THE REDESIGNED SURFACE
    //
    //  One continuous canvas: deep ink in dark mode, warm cream in light
    //  mode. The photo gallery fills the entire top; an editorial sheet
    //  slides over it with a rounded top edge. Stats are a snap-scrolling
    //  ribbon, rooms are story cards, every action floats.
    // ═════════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (LocalDarkTheme.current) DorjaColors.InverseBg else DorjaColors.CanvasBg)
            .testTag("property_detail_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("property_detail_scroll"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── IMMERSIVE HERO — full-bleed gallery, parallaxed, fading ──
            item {
                if (galleryPhotos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                            .offset(y = heroParallax.dp / 2f)
                            .background(if (LocalDarkTheme.current) DorjaColors.InverseBg else DorjaColors.Sand100)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val photo = galleryPhotos[page]
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = photo.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { fullScreenPhotoUrl = photo.url }
                                )
                            }
                        }
                        // Cinematic gradient: reads as depth, not as a dimmer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Black.copy(alpha = 0.30f),
                                        0.35f to Color.Transparent,
                                        0.72f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.72f)
                                    )
                                )
                        )

                        // Glass index pill + room label
                        Surface(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = 58.dp, end = 16.dp)
                                .align(Alignment.TopEnd),
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                val currentPhoto = galleryPhotos.getOrNull(pagerState.currentPage)
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${galleryPhotos.size}" +
                                        (currentPhoto?.roomName?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // For Rent / For Sale — floating intent marker
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(top = 58.dp, start = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = if (safeListing.intent == "RENT") DorjaColors.Jol600 else DorjaColors.BentoPurpleIcon,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = if (safeListing.intent == "RENT") "FOR RENT" else "FOR SALE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }

                        // Dots + expand hint
                        if (galleryPhotos.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 46.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                galleryPhotos.indices.forEach { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (index == pagerState.currentPage) 18.dp else 5.dp, 5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (index == pagerState.currentPage) Color.White
                                                else Color.White.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Tap photo to expand",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 16.dp, end = 16.dp)
                        )
                    }
                } else {
                    // No photos: a serene architectural gradient panel with the identity
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.linearGradient(
                                    if (LocalDarkTheme.current) listOf(
                                        DorjaColors.InverseBg,
                                        DorjaColors.DrawerSidebar,
                                        DorjaColors.InverseBg
                                    ) else listOf(
                                        DorjaColors.Sand100,
                                        DorjaColors.Jol100.copy(alpha = 0.55f),
                                        DorjaColors.Sand100
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.10f),
                            modifier = Modifier.size(160.dp)
                        )
                    }
                }
            }

            // ── THE OVERLAPPING SHEET — slides over the hero ──
            item {
                Column(
                    modifier = Modifier
                        .offset(y = (-28).dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(DorjaColors.CanvasBg)
                        .padding(top = 30.dp, bottom = 4.dp, start = 20.dp, end = 20.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(DorjaColors.Gray300)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Price hero — the number IS the headline
                    Text(
                        text = Formatters.formatPrice(safeListing.priceAmount, safeListing.currency, safeListing.intent),
                        style = MaterialTheme.typography.displayMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (safeListing.intent == "RENT") "per month · For Rent" else "asking price · For Sale",
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray500
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = safeListing.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = DorjaColors.BentoBlueIcon,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = safeListing.publicArea,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DorjaColors.Gray700
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Jurisdiction row — country + emirate/state when set
                    val jurisdictionProfile = CountryRegistry.profile(safeListing.countryCode)
                    val jurisdictionSubnational = jurisdictionProfile.subnational(safeListing.subnationalCode)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DorjaBadge(
                            text = jurisdictionProfile.displayName,
                            icon = Icons.Default.Public,
                            backgroundColor = DorjaColors.BentoBlueBg,
                            textColor = DorjaColors.BentoBlueText
                        )
                        if (jurisdictionSubnational != null) {
                            DorjaBadge(
                                text = jurisdictionSubnational.displayName,
                                icon = Icons.Default.LocationOn,
                                backgroundColor = DorjaColors.BentoPurpleBg,
                                textColor = DorjaColors.BentoPurpleText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    // Location strip — safe shield + map, fused into one calm row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            SafeAddressShield(publicArea = safeListing.publicArea)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (LocalDarkTheme.current) DorjaColors.InverseBg else DorjaColors.BentoBlueBg,
                            modifier = Modifier
                                .clickable {
                                    val lat = safeListing.approximateLat ?: 23.8041
                                    val lng = safeListing.approximateLng ?: 90.3468
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=$lat,$lng"))
                                    )
                                }
                                .widthIn(min = 52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Open in Google Maps",
                                tint = if (LocalDarkTheme.current) Color.White else DorjaColors.BentoBlueIcon,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(22.dp)
                            )
                        }
                    }

                    if (isOwner && safeListing.exactAddress.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DorjaColors.BentoBlueBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Host Confidential Address: ${safeListing.exactAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.BentoBlueText,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // ── SNAP-SCROLL STAT RIBBON — one key number per card, always readable ──
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        StatRibbonCard(
                            icon = Icons.Default.SquareFoot,
                            value = "${safeListing.sqft}",
                            unit = "sqft",
                            label = "Living area"
                        )
                    }
                    item {
                        StatRibbonCard(
                            icon = Icons.Default.Bed,
                            value = "${safeListing.bedrooms}",
                            unit = if (safeListing.bedrooms == 1) "bed" else "beds",
                            label = "Bedrooms"
                        )
                    }
                    item {
                        StatRibbonCard(
                            icon = Icons.Default.Bathtub,
                            value = "${safeListing.bathrooms}",
                            unit = if (safeListing.bathrooms == 1) "bath" else "baths",
                            label = "Bathrooms"
                        )
                    }
                    item {
                        StatRibbonCard(
                            icon = Icons.Default.Balcony,
                            value = "${safeListing.balconies}",
                            unit = if (safeListing.balconies == 1) "balcony" else "balconies",
                            label = "Balconies"
                        )
                    }
                }
            }

            // ── THE EXPERIENCE BANNER — 3D tour / scan entry lives here ──
            item {
                if (has3DScans || isOwner) {
                    val scannedCount = rooms.count { it.has3DScan }
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        if (has3DScans) {
                            ImmersiveTourBanner(
                                scannedCount = scannedCount,
                                onClick = { onOpen3DTour(safeListing.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        if (isOwner) {
                            ScanBanner(
                                onClick = { onOpenScanner(safeListing.id) }
                            )
                        }
                    }
                }
            }

            // ── THE SPACE — story-style room cards ──
            item {
                SectionHeader(
                    number = "01",
                    title = "The Space",
                    subtitle = "${rooms.size} rooms documented"
                )
            }
            item {
                if (rooms.isEmpty()) {
                    Text(
                        text = "No individual rooms documented yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray500,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(rooms, key = { it.id }) { room ->
                            RoomStoryCard(
                                room = room,
                                isOwner = isOwner,
                                onClick = { selectedRoomForDetail = room },
                                onOpen3D = { onOpen3DTour(safeListing.id) }
                            )
                        }
                    }
                }
            }

            // ── ABOUT — editorial paragraph ──
            item {
                SectionHeader(
                    number = "02",
                    title = "About this Home",
                    subtitle = null
                )
            }
            item {
                Text(
                    text = if (safeListing.description.isNotBlank()) safeListing.description
                    else "Verified residential unit listed on Dorja.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DorjaColors.Gray700,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                if (safeListing.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        safeListing.tags.split(",").forEach { tag ->
                            if (tag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = DorjaColors.Sand100,
                                    border = BorderStroke(1.dp, DorjaColors.Sand300)
                                ) {
                                    Text(
                                        text = tag.trim(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DorjaColors.Gray700,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── LIVEABILITY & ENERGY — quiet data, styled as a dashboard strip ──
            val liveabilityProfile = CountryRegistry.profile(safeListing.countryCode)
            val energyRows = listOfNotNull(
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.ENERGY_CLASS) && !safeListing.energyCertificateClass.isNullOrBlank())
                    LiveabilityField.ENERGY_CLASS.label to safeListing.energyCertificateClass!! else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.ENERGY_ISSUER) && !safeListing.energyCertificateIssuer.isNullOrBlank())
                    LiveabilityField.ENERGY_ISSUER.label to safeListing.energyCertificateIssuer!! else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.HEATING_COST) && safeListing.annualHeatingCost != null)
                    LiveabilityField.HEATING_COST.label to
                        "${Formatters.formatAmountLong(safeListing.annualHeatingCost!!, safeListing.currency)} / year" else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.RENOVATION_YEAR) && safeListing.renovationYear != null)
                    LiveabilityField.RENOVATION_YEAR.label to safeListing.renovationYear!!.toString() else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.POWER_BACKUP) && !safeListing.powerBackup.isNullOrBlank())
                    LiveabilityField.POWER_BACKUP.label to safeListing.powerBackup!! else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.WATER_SUPPLY) && !safeListing.waterSupply.isNullOrBlank())
                    LiveabilityField.WATER_SUPPLY.label to safeListing.waterSupply!! else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.FLOOD_RISK) && !safeListing.floodRisk.isNullOrBlank())
                    LiveabilityField.FLOOD_RISK.label to safeListing.floodRisk!! else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.BUILDING_CONDITION) && !safeListing.buildingCondition.isNullOrBlank())
                    LiveabilityField.BUILDING_CONDITION.label to safeListing.buildingCondition!! else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.BUILDING_AGE) && safeListing.buildingAgeYears != null)
                    LiveabilityField.BUILDING_AGE.label to "${safeListing.buildingAgeYears} years" else null,
                if (liveabilityProfile.liveabilityFields.contains(LiveabilityField.DISASTER_CONTEXT) && !safeListing.disasterContext.isNullOrBlank())
                    LiveabilityField.DISASTER_CONTEXT.label to safeListing.disasterContext!! else null
            )
            if (energyRows.isNotEmpty()) {
                item {
                    SectionHeader(
                        number = "03",
                        title = "Liveability & Energy",
                        subtitle = "Source-labelled claims from the host"
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DorjaColors.Paper50)
                            .padding(4.dp)
                    ) {
                        energyRows.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowItems.forEach { (label, value) ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = DorjaColors.White,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = label.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DorjaColors.Gray500,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = value,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = DorjaColors.Ink950,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // ── THE PAPER TRAIL — handover passport + documents + endorsements ──
            item {
                SectionHeader(
                    number = "04",
                    title = "The Paper Trail",
                    subtitle = "Evidence the honest way"
                )
            }
            item {
                PaperTrailCard(
                    title = "Digital Handover Passport",
                    subtitle = stringResource(id = R.string.handover_passport_subtitle),
                    detail = passport?.let { p ->
                        "Property Passport ${p.id.uppercase()} · ${CountryRegistry.profile(p.countryCode).displayName}"
                    },
                    icon = Icons.Default.Description,
                    tint = DorjaColors.BentoGreenIcon,
                    chipBg = DorjaColors.BentoGreenBg,
                    onClick = { onViewHandoverPassport(safeListing.id) }
                )
            }
            item {
                if (legalDocs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DorjaColors.White)
                            .border(0.5.dp, DorjaColors.BentoCardBorder, RoundedCornerShape(20.dp))
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Legal Documents · ${legalDocs.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            if (isOwner) {
                                TextButton(onClick = { legalDocPicker.launch("*/*") }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = DorjaColors.Jol600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", color = DorjaColors.Jol600, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        legalDocs.forEach { doc ->
                            val storedFile = repository.getLegalDocumentFile(doc)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = storedFile != null) {
                                        // Open the stored copy through FileProvider
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            storedFile!!
                                        )
                                        val open = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, docMimeType(storedFile.name))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(open)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No app can open this file type", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (storedFile != null) DorjaColors.Jol600 else DorjaColors.Gray500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.documentTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DorjaColors.Ink950,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = buildString {
                                            append("Type: ${doc.documentType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}")
                                            if (doc.documentNumber.isNotBlank()) append(" · No. ${doc.documentNumber}")
                                            if (storedFile != null) append(" · tap to open")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Gray500
                                    )
                                }
                                if (isOwner) {
                                    IconButton(onClick = { docPendingDelete = doc }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete document",
                                            tint = DorjaColors.Gray500,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                EvidenceBadge(level = EvidenceLevel.fromCode(doc.evidenceLevel))
                            }
                            HorizontalDivider(color = DorjaColors.BentoCardBorder.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "No legal documents attached.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray500
                        )
                        if (isOwner) {
                            TextButton(onClick = { legalDocPicker.launch("*/*") }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = DorjaColors.Jol600, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DorjaColors.White)
                        .border(0.5.dp, DorjaColors.BentoCardBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = DorjaColors.BentoPurpleIcon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PROFESSIONAL HANDOFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (endorsements.isEmpty()) {
                        Text(
                            text = "No licensed professional has taken responsibility for a section yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray500
                        )
                    } else {
                        endorsements.forEach { end ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = DorjaColors.BentoGreenIcon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${end.section.replaceFirstChar { it.uppercase() }} — ${end.professionalName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Ink950,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = buildString {
                                            append(end.roleLabel.ifBlank { "Licensed professional" })
                                            if (end.licenceId.isNotBlank()) append(" • Lic. ${end.licenceId}")
                                            append(" • ${Formatters.formatDateOnly(end.endorsedAt)}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Gray500
                                    )
                                    if (end.statement.isNotBlank()) {
                                        Text(
                                            text = end.statement,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DorjaColors.Gray700
                                        )
                                    }
                                }
                                if (isOwner) {
                                    IconButton(onClick = {
                                        scope.launch { repository.deleteEndorsement(end.id) }
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove endorsement",
                                            tint = DorjaColors.Gray500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isOwner) {
                        Spacer(modifier = Modifier.height(6.dp))
                        DorjaOutlinedButton(
                            text = "Add Professional Endorsement",
                            onClick = { showEndorsementDialog = true },
                            icon = Icons.Default.WorkspacePremium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = "DORJA records the endorsement and never verifies the licence — check it with the issuing authority.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            item {
                PaperTrailCard(
                    title = "Export Decision Pack (PDF)",
                    subtitle = "Country-specific checklist, evidence-labelled documents & promises",
                    detail = null,
                    icon = Icons.Default.Share,
                    tint = DorjaColors.BentoBlueIcon,
                    chipBg = DorjaColors.BentoBlueBg,
                    busy = exportingPack,
                    onClick = {
                        if (!exportingPack) {
                            scope.launch {
                                exportingPack = true
                                val file = DisclosurePackExporter.generate(context, safeListing.id)
                                exportingPack = false
                                if (file != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "DORJA Decision Pack — ${safeListing.title}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(share, "Share Decision Pack"))
                                } else {
                                    Toast.makeText(context, "Could not generate decision pack", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
            }

            // ── FAIR PLAY — disputes, reports, responses ──
            if (listingReports.isNotEmpty() || !isOwner || isOwner) {
                item {
                    SectionHeader(
                        number = "05",
                        title = "Fair Play",
                        subtitle = "Neutral records, both sides visible"
                    )
                }
            }
            if (listingReports.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DorjaColors.White)
                            .border(0.5.dp, DorjaColors.BentoCardBorder, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        listingReports.forEach { report ->
                            ConflictCard(
                                report = report,
                                responses = reportResponsesById[report.id] ?: emptyList(),
                                appeals = reportAppealsById[report.id] ?: emptyList(),
                                viewerId = currentUser?.id,
                                isOwner = isOwner,
                                onResolve = { reportId ->
                                    resolveReportId = reportId
                                    showResolveDialog = true
                                },
                                onWithdraw = { reportId ->
                                    scope.launch { repository.withdrawReport(reportId) }
                                },
                                onAppeal = { reportId ->
                                    appealReportId = reportId
                                    showAppealDialog = true
                                },
                                onDecideAppeal = { reportId, appealId ->
                                    pendingAppealDecision = reportId to appealId
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
            if (!isOwner) {
                item {
                    PaperTrailCard(
                        title = "Report a Problem",
                        subtitle = "Record a claim conflict. The host can respond; both sides stay visible.",
                        detail = null,
                        icon = Icons.Default.Flag,
                        tint = DorjaColors.BentoAmberIcon,
                        chipBg = DorjaColors.BentoAmberBg,
                        onClick = { showReportDialog = true }
                    )
                }
            }
            if (isOwner && listingReports.any { it.state == "OPEN" || it.state == "COUNTERPARTY_RESPONDED" }) {
                item {
                    PaperTrailCard(
                        title = "Respond to a Report",
                        subtitle = "State your side with an evidence reference. Both claims stay visible.",
                        detail = null,
                        icon = Icons.Default.Balance,
                        tint = DorjaColors.BentoBlueIcon,
                        chipBg = DorjaColors.BentoBlueBg,
                        onClick = { showRespondDialog = true }
                    )
                }
            }

            // Breathing room below the floating action bar
            item { Spacer(modifier = Modifier.height(120.dp)) }
        }

        // ── FLOATING BACK BUTTON — glass circle over everything ──
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 6.dp, start = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .testTag("detail_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // ── SCROLL-AWARE COMPACT TITLE BAR — rises once the hero is gone ──
        AnimatedVisibility(
            visible = isScrolled,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = DorjaColors.CanvasBg.copy(alpha = 0.96f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = safeListing.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Formatters.formatPrice(safeListing.priceAmount, safeListing.currency, safeListing.intent),
                        style = MaterialTheme.typography.titleSmall,
                        color = DorjaColors.Jol600,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── FLOATING GLASS ACTION BAR — a pill, not a slab ──
        // Light mode: solid white pill so controls stay legible on bright
        // pages; dark mode keeps the near-black glass.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (LocalDarkTheme.current) DorjaColors.InverseBg.copy(alpha = 0.96f) else DorjaColors.White,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, if (LocalDarkTheme.current) Color.White.copy(alpha = 0.14f) else DorjaColors.BentoCardBorder)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwner) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (LocalDarkTheme.current) Color.White.copy(alpha = 0.10f) else DorjaColors.ErrorContainer,
                        onClick = {
                            scope.launch {
                                repository.deleteListing(safeListing.id)
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("delete_listing_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = DorjaColors.Error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Delete Listing",
                                style = MaterialTheme.typography.labelLarge,
                                color = DorjaColors.Error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (currentUser?.role == "BUYER") {
                        CompareButton(onClick = onOpenCompare)
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    HeyDorjaPill(onClick = {
                        if (!hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        showHeyDorjaSheet = true
                    })
                } else {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, if (LocalDarkTheme.current) Color.White.copy(alpha = 0.35f) else DorjaColors.Gray300),
                        onClick = {
                            val seekerId = currentUser?.id ?: ""
                            onChatWithSeller(safeListing.id, seekerId, safeListing.ownerId)
                        },
                        modifier = Modifier.testTag("chat_with_seller_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = if (LocalDarkTheme.current) Color.White else DorjaColors.Ink950,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Chat",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (LocalDarkTheme.current) Color.White else DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = DorjaColors.Jol600,
                        onClick = { showVisitRequestDialog = true },
                        modifier = Modifier.testTag("request_visit_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Book Visit",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (currentUser?.role == "BUYER") {
                        CompareButton(onClick = onOpenCompare)
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    HeyDorjaPill(onClick = {
                        if (!hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        showHeyDorjaSheet = true
                    })
                }
            }
        }
    }

    if (showHeyDorjaSheet) {
        HeyDorjaAssistantSheet(
            propertyContext = propertyAiContext,
            onDismiss = { showHeyDorjaSheet = false },
            onNavigateToSettings = onOpenSettingsTab
        )
    }
}

/**
 * Wake-word matcher for the "Hey Dorja" voice trigger.
 *
 * Speech recognizers transliterate the brand name inconsistently depending on
 * the speaker's accent and the recognizer locale ("dorja", "doria", "dhaka"…),
 * and some users just say "Dorja" without "Hey" (or vice versa). This accepts:
 *  - the standalone word "hey"
 *  - "dorja" or any close phonetic rendering (j/g/y soft-g variants)
 *  - any combination of the two, in any order
 */
/** Mime type guess for opening a stored document with an external viewer. */
private fun docMimeType(fileName: String): String = when {
    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
    fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
    fileName.endsWith(".txt", ignoreCase = true) || fileName.endsWith(".csv", ignoreCase = true) -> "text/plain"
    else -> "*/*"
}

/** Best-effort display name for a picked document Uri (no persistence). */
private fun queryDocDisplayName(context: android.content.Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}

private fun matchesWakeWord(lower: String): Boolean {
    if (lower.contains("hey")) return true
    // d + o + (r | l) + soft consonant + final vowel — covers dorja/doria/dorga/dolja…
    // without false-positives on everyday words like "dog".
    if (Regex("\\bd[o0]r?[ljgyzi][ae]\\b") in lower) return true
    return false
}

/**
 * Editorial section header — oversized faint index number, strong title.
 * This is the rhythm that replaces the bento grid: one column, numbered
 * chapters, generous whitespace.
 */
@Composable
private fun SectionHeader(number: String, title: String, subtitle: String?) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = number,
                style = MaterialTheme.typography.displayMedium,
                color = DorjaColors.Jol600.copy(alpha = 0.22f),
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray500
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DorjaColors.BentoCardBorder.copy(alpha = 0.6f))
    }
}

/**
 * One key number, one card, snap-scrolled horizontally. No grid: each stat
 * gets full typographic room, values never squish.
 */
@Composable
private fun StatRibbonCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DorjaColors.White,
        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder),
        modifier = Modifier.width(132.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DorjaColors.BentoBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DorjaColors.BentoBlueIcon, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = DorjaColors.Gray500,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500
            )
        }
    }
}

/**
 * The 3D experience banner: a dark gradient card with a slow-breathing glow
 * behind the ViewInAr glyph — the scan is the product, so it gets presence.
 */
@Composable
private fun ImmersiveTourBanner(scannedCount: Int, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "tourGlow")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("see_3d_scans_menu_card")
    ) {
        Box(
            modifier = Modifier
                .background(
                    // Dark mode keeps the deep-forest cinematic gradient; light
                    // mode gets a soft mint band so it never reads as a dark blob.
                    if (LocalDarkTheme.current) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF122B1F),
                                Color(0xFF0D1F17),
                                Color(0xFF122B1F)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE7F4EA),
                                Color(0xFFDCF0E3),
                                Color(0xFFE7F4EA)
                            )
                        )
                    }
                )
                .padding(18.dp)
        ) {
            // breathing glow
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(DorjaColors.DrawerAccent.copy(alpha = glow * 0.35f), Color.Transparent)))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DorjaColors.DrawerAccent.copy(alpha = if (LocalDarkTheme.current) 0.18f else 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = "3D Scan",
                        tint = if (LocalDarkTheme.current) DorjaColors.DrawerAccent else DorjaColors.Success,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Step Inside",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (LocalDarkTheme.current) Color.White else DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (scannedCount > 0) "$scannedCount room${if (scannedCount == 1) "" else "s"} scanned in immersive 360°" else "Immersive 360° walkthrough available",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (LocalDarkTheme.current) DorjaColors.DrawerAccent else DorjaColors.Success,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (LocalDarkTheme.current) DorjaColors.DrawerAccent else DorjaColors.Success,
                    modifier = Modifier.testTag("see_3d_scans_hero_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enter",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Host-side scan banner — same visual family, cyan accent, quieter.
 */
@Composable
private fun ScanBanner(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "scanGlow")
    val glow by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanGlowAlpha"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("host_scan_3d_rooms_card")
    ) {
        Box(
            modifier = Modifier
                .background(
                    // Same light-mode treatment as the green banner: keep the
                    // deep-cyan cinematic gradient for dark mode only.
                    if (LocalDarkTheme.current) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF0E2430),
                                Color(0xFF0B1A24),
                                Color(0xFF0E2430)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFE1F3F7),
                                Color(0xFFD2ECF3),
                                Color(0xFFE1F3F7)
                            )
                        )
                    }
                )
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF00BCD4).copy(alpha = glow * 0.4f), Color.Transparent)))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF00838F).copy(alpha = if (LocalDarkTheme.current) 0.16f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan 3D",
                        tint = if (LocalDarkTheme.current) Color(0xFF00BCD4) else Color(0xFF00838F),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scan a New Room",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (LocalDarkTheme.current) Color.White else DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "360° capture with gyroscope guidance",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (LocalDarkTheme.current) Color(0xFF00BCD4) else Color(0xFF00838F),
                        fontSize = 11.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (LocalDarkTheme.current) Color(0xFF00BCD4) else Color(0xFF00838F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Story-style room card — tall photo, overlay label, scan badge. The gallery
 * of the home, not a table row.
 */
@Composable
private fun RoomStoryCard(
    room: RoomItem,
    isOwner: Boolean,
    onClick: () -> Unit,
    onOpen3D: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DorjaColors.White,
        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder),
        onClick = onClick,
        modifier = Modifier.width(190.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (!room.photoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = room.photoPath,
                        contentDescription = room.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(DorjaColors.BentoBlueBg, DorjaColors.Paper50)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = DorjaColors.BentoBlueIcon.copy(alpha = 0.5f),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                if (room.has3DScan) {
                    val isV2 = try {
                        org.json.JSONObject(room.panoramaData).optInt("version", 1) >= 2
                    } catch (_: Exception) { false }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (LocalDarkTheme.current) DorjaColors.InverseBg.copy(alpha = 0.85f) else DorjaColors.BentoBlueBg,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = DorjaColors.BentoBlueIcon,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isV2) "360°" else "3D",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.BentoBlueText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = room.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (room.dimensions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = room.dimensions,
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (!isOwner && room.has3DScan) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DorjaColors.Jol100,
                        onClick = onOpen3D,
                        modifier = Modifier.testTag("detail_room_3d_btn_${room.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = DorjaColors.Jol700,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View in 3D",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Jol700,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Paper-trail row card — icon chip, title, subtitle, optional detail line.
 * Used for handover passport, decision pack, report entries.
 */
@Composable
private fun PaperTrailCard(
    title: String,
    subtitle: String,
    detail: String?,
    icon: ImageVector,
    tint: Color,
    chipBg: Color,
    busy: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DorjaColors.White,
        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(chipBg),
                contentAlignment = Alignment.Center
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = tint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700,
                    fontSize = 11.sp
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontSize = 10.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Buyer-only circular compare button that sits beside the Hey Dorja pill in
 * the floating action bar. Opens the landscape split-screen comparison stage.
 */
@Composable
private fun CompareButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = DorjaColors.BentoBlueBg,
        border = BorderStroke(1.dp, DorjaColors.BentoBlueIcon.copy(alpha = 0.55f)),
        onClick = onClick,
        modifier = Modifier.testTag("compare_button")
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = L("compare_title"),
                tint = DorjaColors.BentoBlueIcon,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * The compact "Hey Dorja" pill that lives inside the floating action bar.
 */
@Composable
private fun HeyDorjaPill(onClick: () -> Unit) {
    // Fully opaque in both modes — the pill must never read as disabled.
    Surface(
        shape = CircleShape,
        color = DorjaColors.BentoGreenBg,
        border = BorderStroke(1.dp, DorjaColors.BentoGreenIcon.copy(alpha = 0.55f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DorjaLogo(modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Dorja",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = DorjaColors.BentoGreenText
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = DorjaColors.BentoGreenIcon,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
