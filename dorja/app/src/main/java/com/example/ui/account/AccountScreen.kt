package com.example.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.country.CountryRegistry
import com.example.data.model.EvidenceSummary
import com.example.data.model.Report
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.CountryPicker
import com.example.ui.components.DorjaAvatar
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.i18n.L
import com.example.ui.i18n.Lf
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.components.DorjaLogo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountScreen(
    onNavigateToSellerSuite: () -> Unit = {},
    onNavigateToRelocation: (origin: String, destination: String) -> Unit = { _, _ -> }
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("SELLER") }
    var editCountryCode by remember { mutableStateOf("BD") }

    // Evidence health + privacy controls state
    var evidenceSummary by remember { mutableStateOf<EvidenceSummary?>(null) }
    var isReconfirming by remember { mutableStateOf(false) }
    var reconfirmedCount by remember { mutableStateOf(0) }
    var showReconfirmDone by remember { mutableStateOf(false) }
    var showDeleteContentDialog by remember { mutableStateOf(false) }
    var showEraseAccountDialog by remember { mutableStateOf(false) }
    var isPrivacyWorking by remember { mutableStateOf(false) }
    var showContentDeletedDone by remember { mutableStateOf(false) }
    var showEraseDone by remember { mutableStateOf(false) }

    // Load the evidence-health snapshot for this account
    LaunchedEffect(Unit) {
        evidenceSummary = repository.getEvidenceSummary()
    }

    // Edit Profile Modal
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(L("account_edit_profile_role"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(L("account_select_mode"), style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray500)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DorjaChip(
                            selected = editRole == "SELLER",
                            label = L("account_host_seller"),
                            onClick = { editRole = "SELLER" }
                        )
                        DorjaChip(
                            selected = editRole == "BUYER",
                            label = L("account_seeker_buyer"),
                            onClick = { editRole = "BUYER" }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(L("account_display_name")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(L("account_phone_number")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text(L("account_city_area")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text(L("account_bio")) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CountryPicker(
                        selected = editCountryCode,
                        onSelect = { editCountryCode = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = L("account_save_changes"),
                    onClick = {
                        scope.launch {
                            repository.updateUserProfile(
                                displayName = editName,
                                phone = editPhone,
                                email = editEmail,
                                location = editLocation,
                                bio = editBio,
                                role = editRole,
                                countryCode = editCountryCode
                            )
                            showEditProfileDialog = false
                        }
                    },
                    modifier = Modifier.width(140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(L("common_cancel"), color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showDeleteContentDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContentDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DorjaColors.Error) },
            title = { Text(L("account_delete_content_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text(L("account_delete_content_body"))
            },
            confirmButton = {
                DorjaButton(
                    text = L("account_delete_everything"),
                    onClick = {
                        scope.launch {
                            isPrivacyWorking = true
                            repository.deleteAllMyContent()
                            evidenceSummary = repository.getEvidenceSummary()
                            isPrivacyWorking = false
                            showDeleteContentDialog = false
                            showContentDeletedDone = true
                        }
                    },
                    modifier = Modifier.width(160.dp),
                    enabled = !isPrivacyWorking,
                    containerColor = DorjaColors.Error
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteContentDialog = false },
                    enabled = !isPrivacyWorking
                ) {
                    Text(L("common_cancel"), color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showEraseAccountDialog) {
        AlertDialog(
            onDismissRequest = { showEraseAccountDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DorjaColors.Error) },
            title = { Text(L("account_erase_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text(L("account_erase_body"))
            },
            confirmButton = {
                DorjaButton(
                    text = L("account_erase_everything"),
                    onClick = {
                        scope.launch {
                            isPrivacyWorking = true
                            repository.eraseAllMyData()
                            evidenceSummary = repository.getEvidenceSummary()
                            isPrivacyWorking = false
                            showEraseAccountDialog = false
                            showEraseDone = true
                        }
                    },
                    modifier = Modifier.width(160.dp),
                    enabled = !isPrivacyWorking,
                    containerColor = DorjaColors.Error
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showEraseAccountDialog = false },
                    enabled = !isPrivacyWorking
                ) {
                    Text(L("common_cancel"), color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showReconfirmDone) {
        AlertDialog(
            onDismissRequest = { showReconfirmDone = false },
            title = { Text(L("account_reconfirm_done_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text(Lf("account_reconfirm_done_body", reconfirmedCount))
            },
            confirmButton = {
                TextButton(onClick = { showReconfirmDone = false }) {
                    Text(L("common_ok"), color = DorjaColors.Jol600)
                }
            }
        )
    }

    if (showContentDeletedDone) {
        AlertDialog(
            onDismissRequest = { showContentDeletedDone = false },
            title = { Text(L("account_content_deleted_title"), fontWeight = FontWeight.Bold) },
            text = { Text(L("account_content_deleted_body")) },
            confirmButton = {
                TextButton(onClick = { showContentDeletedDone = false }) {
                    Text(L("common_ok"), color = DorjaColors.Jol600)
                }
            }
        )
    }

    if (showEraseDone) {
        AlertDialog(
            onDismissRequest = { showEraseDone = false },
            title = { Text(L("account_erased_title"), fontWeight = FontWeight.Bold) },
            text = { Text(L("account_erased_body")) },
            confirmButton = {
                TextButton(onClick = { showEraseDone = false }) {
                    Text(L("common_ok"), color = DorjaColors.Jol600)
                }
            }
        )
    }

    if (currentUser == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.CanvasBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = DorjaColors.BentoBlueIcon)
                Spacer(modifier = Modifier.height(12.dp))
                Text(L("account_loading"), style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
            }
        }
        return
    }

    val user = currentUser!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("account_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.CanvasBg)
                .padding(top = 6.dp, start = 16.dp, end = 16.dp, bottom = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                DorjaLogo(modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = L("account_title_full"),
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = L("account_subtitle"),
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray700,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Card Bento
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DorjaAvatar(name = user.displayName, size = 56.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                // Badges wrap across lines instead of being clipped in portrait.
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DorjaBadge(
                                        text = if (user.role == "SELLER") L("account_role_host") else L("account_role_buyer"),
                                        backgroundColor = if (user.role == "SELLER") DorjaColors.BentoBlueBg else DorjaColors.BentoGreenBg,
                                        textColor = if (user.role == "SELLER") DorjaColors.BentoBlueText else DorjaColors.BentoGreenText
                                    )
                                    DorjaBadge(
                                        text = Lf(
                                            "account_identity_verified_fmt",
                                            CountryRegistry.identityCredential(user.countryCode).shortName
                                        ),
                                        icon = Icons.Default.VerifiedUser,
                                        backgroundColor = DorjaColors.BentoGreenBg,
                                        textColor = DorjaColors.BentoGreenText
                                    )
                                    DorjaBadge(
                                        text = CountryRegistry.profile(user.countryCode).displayName,
                                        icon = Icons.Default.Place,
                                        backgroundColor = DorjaColors.BentoBlueBg,
                                        textColor = DorjaColors.BentoBlueText
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    editName = user.displayName
                                    editPhone = user.phone
                                    editEmail = user.email
                                    editLocation = user.location
                                    editBio = user.bio
                                    editRole = user.role
                                    editCountryCode = user.countryCode
                                    showEditProfileDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.CanvasBg)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = L("account_edit_profile"), modifier = Modifier.size(18.dp), tint = DorjaColors.Ink950)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // User Details Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = DorjaColors.Gray500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(user.phone, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                        }

                        if (user.location.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = DorjaColors.Gray500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(user.location, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = DorjaColors.Gray500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${CountryRegistry.profile(user.countryCode).displayName} • ${CountryRegistry.profile(user.countryCode).currencyCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }

                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(user.bio, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            }

            // Quick Account Switch Bento Card
            item {
                val targetUserId = if (user.role == "SELLER") "u2" else "u1"
                val targetUserLabel = if (user.role == "SELLER") "Samin (Buyer)" else "Shovro (Host)"
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            repository.switchUser(targetUserId)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DorjaColors.BentoPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = DorjaColors.BentoPurpleIcon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (user.role == "SELLER") L("account_switch_to_buyer") else L("account_switch_to_host"),
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Lf("account_switch_subtitle", targetUserLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DorjaColors.Gray500
                        )
                    }
                }
            }

            // Trust & Verification Credentials Bento Card
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("account_security"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SecurityRow(
                            title = Lf(
                                "account_identity_verification_fmt",
                                CountryRegistry.identityCredential(user.countryCode).shortName
                            ),
                            status = L("account_status_passed"),
                            icon = Icons.Default.Shield
                        )
                        SecurityRow(title = L("account_safeview_gps"), status = L("account_status_active"), icon = Icons.Default.Lock)
                        SecurityRow(title = L("account_local_persistence"), status = L("account_status_on_device"), icon = Icons.Default.CheckCircle)
                    }
                }
            }

            // Evidence Health Bento Card (atlas §3 honesty vocabulary, expiry & staleness)
            item {
                val summary = evidenceSummary
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("account_evidence_health"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (summary == null) {
                            Text(
                                text = L("account_checking_evidence"),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        } else if (summary.totalDocs == 0) {
                            Text(
                                text = L("account_no_docs"),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        } else {
                            EvidenceStatRow(
                                icon = Icons.Default.FactCheck,
                                label = L("account_docs_on_record"),
                                value = summary.totalDocs.toString(),
                                tone = EvidenceTone.NEUTRAL
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.VerifiedUser,
                                label = L("account_issuer_confirmed"),
                                value = summary.confirmedDocs.toString(),
                                tone = EvidenceTone.GOOD
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.Info,
                                label = L("account_self_declared"),
                                value = summary.selfDeclaredDocs.toString(),
                                tone = EvidenceTone.WARN
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.History,
                                label = L("account_stale_checks"),
                                value = summary.staleDocs.toString(),
                                tone = if (summary.staleDocs > 0) EvidenceTone.WARN else EvidenceTone.GOOD
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.Warning,
                                label = L("account_marked_expired"),
                                value = summary.expiredDocs.toString(),
                                tone = if (summary.expiredDocs > 0) EvidenceTone.BAD else EvidenceTone.GOOD
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DorjaOutlinedButton(
                            text = if (isReconfirming) L("account_reconfirming") else L("account_reconfirm"),
                            onClick = {
                                scope.launch {
                                    isReconfirming = true
                                    reconfirmedCount = repository.reconfirmEvidence()
                                    evidenceSummary = repository.getEvidenceSummary()
                                    isReconfirming = false
                                    showReconfirmDone = true
                                }
                            },
                            enabled = !isReconfirming && (evidenceSummary?.totalDocs ?: 0) > 0,
                            icon = Icons.Default.FactCheck,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L("account_reconfirm_hint"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500
                        )
                    }
                }
            }

            // Cross-border Relocation Mode (atlas §8)
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onNavigateToRelocation(user.countryCode, if (user.countryCode == "BD") "IN" else "BD")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DorjaColors.BentoBlueBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = DorjaColors.BentoBlueIcon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L("account_relocation"),
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L("account_relocation_subtitle"),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DorjaColors.Gray500
                        )
                    }
                }
            }

            // Reports & Appeals (Phase 5, atlas §2 appeal & dispute record)
            item {
                val myReports by repository.observeReportsByUser(user.id).collectAsState(initial = emptyList<Report>())
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("account_reports"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L("account_reports_intro"),
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (myReports.isEmpty()) {
                            Text(
                                text = L("account_no_reports"),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500
                            )
                        } else {
                            myReports.take(5).forEach { report ->
                                val reasonLabel = com.example.data.model.ReportReason.fromCode(report.reason).label
                                val stateLabel = report.state.replace('_', ' ').lowercase()
                                    .replaceFirstChar { it.uppercase() }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = DorjaColors.BentoAmberIcon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reasonLabel,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DorjaColors.Ink950,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stateLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DorjaColors.Gray500
                                        )
                                    }
                                    if (report.state == "OPEN" || report.state == "RESOLVED") {
                                        TextButton(onClick = {
                                            scope.launch { repository.withdrawReport(report.id) }
                                        }) {
                                            Text(L("account_withdraw"), color = DorjaColors.Gray700)
                                        }
                                    }
                                }
                            }
                            if (myReports.size > 5) {
                                Text(
                                    text = Lf("account_and_more", myReports.size - 5),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Gray500
                                )
                            }
                        }
                    }
                }
            }

            // Privacy & Data Bento Card (GDPR-style right to erasure)
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("account_privacy_data"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = L("account_privacy_intro"),
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PrivacyActionRow(
                            icon = Icons.Default.PrivacyTip,
                            title = L("account_delete_content"),
                            subtitle = L("account_delete_content_sub"),
                            onClick = { showDeleteContentDialog = true }
                        )
                        PrivacyActionRow(
                            icon = Icons.Default.DeleteForever,
                            title = L("account_erase_data"),
                            subtitle = L("account_erase_data_sub"),
                            destructive = true,
                            onClick = { showEraseAccountDialog = true }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = DorjaColors.Gray500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = L("account_consent_note"),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class EvidenceTone { GOOD, WARN, BAD, NEUTRAL }

@Composable
private fun EvidenceStatRow(
    icon: ImageVector,
    label: String,
    value: String,
    tone: EvidenceTone
) {
    val (bg, fg) = when (tone) {
        EvidenceTone.GOOD -> DorjaColors.BentoGreenBg to DorjaColors.BentoGreenText
        EvidenceTone.WARN -> DorjaColors.BentoAmberBg to DorjaColors.BentoAmberText
        EvidenceTone.BAD -> DorjaColors.ErrorContainer to DorjaColors.Error
        EvidenceTone.NEUTRAL -> DorjaColors.Sand100 to DorjaColors.Ink950
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Ink950)
        }
        DorjaBadge(text = value, backgroundColor = bg, textColor = fg)
    }
}

@Composable
private fun PrivacyActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (destructive) DorjaColors.ErrorContainer.copy(alpha = 0.45f) else DorjaColors.Sand100)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (destructive) DorjaColors.Error else DorjaColors.BentoBlueIcon,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (destructive) DorjaColors.Error else DorjaColors.Ink950,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DorjaColors.Gray700
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = DorjaColors.Gray500
        )
    }
}

@Composable
private fun SecurityRow(title: String, status: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = DorjaColors.BentoGreenIcon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Ink950)
        }
        DorjaBadge(text = status, backgroundColor = DorjaColors.BentoGreenBg, textColor = DorjaColors.BentoGreenText)
    }
}
