package com.example.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FactCheck
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.country.CountryRegistry
import com.example.data.model.EvidenceSummary
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.CountryPicker
import com.example.ui.components.DorjaAvatar
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@Composable
fun AccountScreen(
    onNavigateToSellerSuite: () -> Unit = {}
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

    var showResetDialog by remember { mutableStateOf(false) }

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
                Text("Edit Profile & Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Select Active Account Mode", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray500)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DorjaChip(
                            selected = editRole == "SELLER",
                            label = "Host / Seller",
                            onClick = { editRole = "SELLER" }
                        )
                        DorjaChip(
                            selected = editRole == "BUYER",
                            label = "Seeker / Buyer",
                            onClick = { editRole = "BUYER" }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
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
                        label = { Text("Phone Number") },
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
                        label = { Text("City / Area") },
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
                        label = { Text("Bio / Tagline") },
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
                    text = "Save Changes",
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
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Local Storage", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all local listings, chats, and viewing passes.") },
            confirmButton = {
                DorjaButton(
                    text = "Reset All",
                    onClick = {
                        scope.launch {
                            repository.resetAllData()
                            showResetDialog = false
                        }
                    },
                    modifier = Modifier.width(120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showDeleteContentDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContentDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DorjaColors.Error) },
            title = { Text("Delete My Content", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This permanently deletes your listings, rooms, 3D scans, legal documents, " +
                        "property passports, chats, messages, and viewing passes from this device. " +
                        "Your profile rows are kept. This cannot be undone."
                )
            },
            confirmButton = {
                DorjaButton(
                    text = "Delete Everything",
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
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showEraseAccountDialog) {
        AlertDialog(
            onDismissRequest = { showEraseAccountDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DorjaColors.Error) },
            title = { Text("Erase My Account Data", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This erases EVERYTHING stored about you on this device: all content plus your " +
                        "profile rows. The app then re-initializes with clean demo accounts. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                DorjaButton(
                    text = "Erase Everything",
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
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showReconfirmDone) {
        AlertDialog(
            onDismissRequest = { showReconfirmDone = false },
            title = { Text("Evidence Re-confirmed", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "$reconfirmedCount document(s) re-confirmed. This is your own re-attestation — " +
                        "the evidence level of each document is unchanged and DORJA has not independently " +
                        "verified anything new."
                )
            },
            confirmButton = {
                TextButton(onClick = { showReconfirmDone = false }) {
                    Text("OK", color = DorjaColors.Jol600)
                }
            }
        )
    }

    if (showContentDeletedDone) {
        AlertDialog(
            onDismissRequest = { showContentDeletedDone = false },
            title = { Text("Content Deleted", fontWeight = FontWeight.Bold) },
            text = { Text("All of your listings, evidence, chats, and viewing passes have been removed from this device.") },
            confirmButton = {
                TextButton(onClick = { showContentDeletedDone = false }) {
                    Text("OK", color = DorjaColors.Jol600)
                }
            }
        )
    }

    if (showEraseDone) {
        AlertDialog(
            onDismissRequest = { showEraseDone = false },
            title = { Text("Account Data Erased", fontWeight = FontWeight.Bold) },
            text = { Text("All personal data stored on this device has been erased and the app has been reset to clean demo accounts.") },
            confirmButton = {
                TextButton(onClick = { showEraseDone = false }) {
                    Text("OK", color = DorjaColors.Jol600)
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
                Text("Loading account...", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
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
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dorja_logo),
                    contentDescription = "Dorja Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "My Dorja Account",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Identity & Real Estate Credentials • Local Persistence",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card Bento
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DorjaBadge(
                                        text = if (user.role == "SELLER") "HOST / SELLER" else "SEEKER / BUYER",
                                        backgroundColor = if (user.role == "SELLER") DorjaColors.BentoBlueBg else DorjaColors.BentoGreenBg,
                                        textColor = if (user.role == "SELLER") DorjaColors.BentoBlueText else DorjaColors.BentoGreenText
                                    )
                                    DorjaBadge(
                                        text = if (user.countryCode == "BD") "NID VERIFIED" else "IDENTITY VERIFIED",
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
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(18.dp), tint = DorjaColors.Ink950)
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
                                text = if (user.role == "SELLER") "Switch to Buyer Account" else "Switch to Host Account",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Switch to $targetUserLabel to chat & explore as the other party. Each demo account keeps its own country & currency.",
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
                            text = "SECURITY CREDENTIALS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SecurityRow(
                            title = if (user.countryCode == "BD") "NID Verification" else "Identity Verification",
                            status = "PASSED",
                            icon = Icons.Default.Shield
                        )
                        SecurityRow(title = "SafeView GPS Token Registry", status = "ACTIVE", icon = Icons.Default.Lock)
                        SecurityRow(title = "Local Room Persistence", status = "ON-DEVICE", icon = Icons.Default.CheckCircle)
                    }
                }
            }

            // Data Management Bento Card
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DATABASE MANAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DorjaOutlinedButton(
                            text = "Clear Local Data & Re-initialize",
                            onClick = { showResetDialog = true },
                            icon = Icons.Default.DeleteSweep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Evidence Health Bento Card (atlas §3 honesty vocabulary, expiry & staleness)
            item {
                val summary = evidenceSummary
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "EVIDENCE HEALTH",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (summary == null) {
                            Text(
                                text = "Checking your document evidence…",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        } else if (summary.totalDocs == 0) {
                            Text(
                                text = "No legal documents attached to any listing yet. Evidence levels appear here once you add documents in Host Suite.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        } else {
                            EvidenceStatRow(
                                icon = Icons.Default.FactCheck,
                                label = "Documents on record",
                                value = summary.totalDocs.toString(),
                                tone = EvidenceTone.NEUTRAL
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.VerifiedUser,
                                label = "Issuer / government confirmed",
                                value = summary.confirmedDocs.toString(),
                                tone = EvidenceTone.GOOD
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.Info,
                                label = "Self-declared (not verified)",
                                value = summary.selfDeclaredDocs.toString(),
                                tone = EvidenceTone.WARN
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.History,
                                label = "Checks older than 24 months",
                                value = summary.staleDocs.toString(),
                                tone = if (summary.staleDocs > 0) EvidenceTone.WARN else EvidenceTone.GOOD
                            )
                            EvidenceStatRow(
                                icon = Icons.Default.Warning,
                                label = "Marked expired",
                                value = summary.expiredDocs.toString(),
                                tone = if (summary.expiredDocs > 0) EvidenceTone.BAD else EvidenceTone.GOOD
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DorjaOutlinedButton(
                            text = if (isReconfirming) "Re-confirming…" else "Re-confirm My Evidence",
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
                            text = "Re-confirming refreshes your own attestation and evidence-check dates. It never raises an evidence level and is never shown as independent verification.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500
                        )
                    }
                }
            }

            // Privacy & Data Bento Card (GDPR-style right to erasure)
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PRIVACY & DATA",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Your evidence, chats, and viewing history live on this device. You control deletion — no support ticket required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PrivacyActionRow(
                            icon = Icons.Default.PrivacyTip,
                            title = "Delete My Content",
                            subtitle = "Removes your listings, evidence, scans, chats, and viewing passes. Keeps your profile.",
                            onClick = { showDeleteContentDialog = true }
                        )
                        PrivacyActionRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Erase My Account Data",
                            subtitle = "Erases everything above plus your profile rows, then resets the app to clean demo accounts.",
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
                                text = "Consent records and access logs stay with the data they describe — when the data goes, they go too.",
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
