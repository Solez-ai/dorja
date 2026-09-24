package com.example.ui.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.IdentityVerification
import com.example.data.model.User
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaLogo
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.i18n.L
import com.example.ui.i18n.Lf
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Admin console (single admin per device): reviews real identity
 * submissions, records third-party checks, and keeps the agent register.
 * Every action here is audit-logged with reviewer and timestamp.
 */
@Composable
fun AdminScreen() {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()

    val verifications by repository.observeAllVerifications().collectAsState(initial = emptyList())
    val checks by repository.observeAllThirdPartyChecks().collectAsState(initial = emptyList())

    val pending = verifications.filter { it.status == "SUBMITTED" || it.status == "UNDER_REVIEW" }
    // Every account on the device (the admin's own row excluded): the agent
    // register must list buyers and sellers whether or not they have
    // submitted documents yet.
    val allUsers by repository.observeAllUsers().collectAsState(initial = emptyList())
    val loadedUsers: Map<String, User> = remember(allUsers) { allUsers.associateBy { it.id } }
    val agents: List<User> = remember(allUsers) { allUsers.filter { it.role != "ADMIN" } }

    var decideTarget by remember { mutableStateOf<Pair<IdentityVerification, Boolean>?>(null) }
    var reviewNote by remember { mutableStateOf("") }
    var checkTarget by remember { mutableStateOf<IdentityVerification?>(null) }
    var checkType by remember { mutableStateOf("ISSUER_DATABASE") }
    var checkResult by remember { mutableStateOf("PASS") }
    var checkNote by remember { mutableStateOf("") }
    val decided = verifications.filter { it.status == "APPROVED" || it.status == "REJECTED" }


    // ── Decide dialog (approve / reject with note) ──
    decideTarget?.let { (v, approve) ->
        AlertDialog(
            onDismissRequest = { decideTarget = null },
            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (approve) DorjaColors.BentoGreenIcon else DorjaColors.Error) },
            title = { Text(L("admin_decide_title"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "${v.documentKind} · ${v.documentNumberMasked} · ${v.holderName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reviewNote,
                        onValueChange = { reviewNote = it },
                        label = { Text(L("admin_review_note")) },
                        placeholder = { Text(L("admin_review_note_hint")) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = if (approve) L("admin_approve") else L("admin_reject"),
                    containerColor = if (approve) DorjaColors.Jol600 else DorjaColors.Error,
                    onClick = {
                        scope.launch {
                            repository.reviewVerification(
                                verificationId = v.id,
                                approve = approve,
                                note = reviewNote,
                                reviewerId = currentUser?.id ?: ""
                            )
                            reviewNote = ""
                            decideTarget = null
                        }
                    },
                    modifier = Modifier.widthIn(min = 120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { decideTarget = null }) {
                    Text(L("common_cancel"), color = DorjaColors.Gray700)
                }
            }
        )
    }

    // ── Third-party check dialog (atlas §8 audit vocabulary) ──
    checkTarget?.let { v ->
        AlertDialog(
            onDismissRequest = { checkTarget = null },
            icon = { Icon(Icons.Default.FactCheck, contentDescription = null, tint = DorjaColors.BentoBlueIcon) },
            title = { Text(L("admin_check_title"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        L("admin_check_intro"),
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                    Text(L("admin_check_type"), style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DorjaChip(selected = checkType == "ISSUER_DATABASE", label = L("admin_check_issuer_db"), onClick = { checkType = "ISSUER_DATABASE" })
                        DorjaChip(selected = checkType == "DOCUMENT_AUTHENTICITY", label = L("admin_check_doc_auth"), onClick = { checkType = "DOCUMENT_AUTHENTICITY" })
                        DorjaChip(selected = checkType == "SANCTIONS_SCREENING", label = L("admin_check_sanctions"), onClick = { checkType = "SANCTIONS_SCREENING" })
                    }
                    Text(L("admin_check_result"), style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray700)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DorjaChip(selected = checkResult == "PASS", label = L("admin_check_pass"), onClick = { checkResult = "PASS" })
                        DorjaChip(selected = checkResult == "FLAGGED", label = L("admin_check_flagged"), onClick = { checkResult = "FLAGGED" })
                        DorjaChip(selected = checkResult == "INCONCLUSIVE", label = L("admin_check_inconclusive"), onClick = { checkResult = "INCONCLUSIVE" })
                    }
                    OutlinedTextField(
                        value = checkNote,
                        onValueChange = { checkNote = it },
                        label = { Text(L("admin_check_ref")) },
                        placeholder = { Text(L("admin_check_ref_hint")) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = L("admin_check_record_cta"),
                    onClick = {
                        scope.launch {
                            repository.addThirdPartyCheck(
                                verificationId = v.id,
                                subjectUserId = v.userId,
                                checkType = checkType,
                                result = checkResult,
                                note = checkNote.trim(),
                                checkedByUserId = currentUser?.id ?: ""
                            )
                            checkNote = ""
                            checkTarget = null
                        }
                    },
                    modifier = Modifier.widthIn(min = 130.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { checkTarget = null }) {
                    Text(L("common_cancel"), color = DorjaColors.Gray700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("admin_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            DorjaLogo(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L("admin_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = L("admin_subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DorjaColors.BentoPurpleBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.BentoPurpleIcon.copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = DorjaColors.BentoPurpleIcon, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = L("admin_role_admin"),
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.BentoPurpleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Metrics
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BentoMetricTile(
                        value = pending.size.toString(),
                        label = L("admin_awaiting"),
                        icon = Icons.Default.FactCheck,
                        iconBg = DorjaColors.BentoAmberBg,
                        iconTint = DorjaColors.BentoAmberIcon,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        value = decided.count { it.status == "APPROVED" }.toString(),
                        label = L("admin_approved_count"),
                        icon = Icons.Default.VerifiedUser,
                        iconBg = DorjaColors.BentoGreenBg,
                        iconTint = DorjaColors.BentoGreenIcon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Review queue ──
            item {
                Text(
                    text = L("admin_queue"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            if (pending.isEmpty()) {
                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DorjaColors.BentoGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = DorjaColors.BentoGreenIcon, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                L("admin_queue_empty_title"),
                                style = MaterialTheme.typography.titleMedium,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                L("admin_queue_empty_body"),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(pending.size) { index ->
                    val v = pending[index]
                    VerificationCard(
                        v = v,
                        subject = loadedUsers[v.userId],
                        checksForV = checks.filter { it.verificationId == v.id },
                        onDecide = { approve -> decideTarget = v to approve },
                        onRecordCheck = { checkTarget = v }
                    )
                }
            }

            // ── Decision history ──
            if (decided.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = L("admin_history"),
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(decided.size) { index ->
                    val v = decided[index]
                    VerificationCard(
                        v = v,
                        subject = loadedUsers[v.userId],
                        checksForV = checks.filter { it.verificationId == v.id },
                        onDecide = { approve -> decideTarget = v to approve },
                        onRecordCheck = { checkTarget = v }
                    )
                }
            }

            // ── Third-party check log ──
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = L("admin_check_log"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            if (checks.isEmpty()) {
                item {
                    Text(
                        L("admin_no_checks"),
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray500
                    )
                }
            } else {
                items(checks.size) { index ->
                    val c = checks[index]
                    val (bg, fg) = when (c.result) {
                        "PASS" -> DorjaColors.BentoGreenBg to DorjaColors.BentoGreenText
                        "FLAGGED" -> DorjaColors.ErrorContainer to DorjaColors.Error
                        else -> DorjaColors.BentoAmberBg to DorjaColors.BentoAmberText
                    }
                    val noNoteLabel = L("admin_no_note")
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = checkTypeLabel(c.checkType) + " · " + checkResultLabel(c.result),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = c.note.ifBlank { noNoteLabel } + " · " + Formatters.formatDateTime(c.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Gray700
                                )
                            }
                            DorjaBadge(
                                text = checkResultLabel(c.result),
                                backgroundColor = bg,
                                textColor = fg
                            )
                        }
                    }
                }
            }

            // ── Agent register ──
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = L("admin_agent_register"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            if (agents.isEmpty()) {
                item {
                    Text(
                        L("admin_no_agents"),
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray500
                    )
                }
            } else {
                items(agents.size) { index ->
                    val user = agents[index]
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = if (user.role == "SELLER") DorjaColors.BentoBlueIcon else DorjaColors.BentoGreenIcon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = roleLabel(user.role) + " · " + user.phone,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Gray700
                                )
                            }
                            DorjaBadge(
                                text = if (user.isIdentityVerified) L("admin_agent_verified") else L("admin_agent_unverified"),
                                backgroundColor = if (user.isIdentityVerified) DorjaColors.BentoGreenBg else DorjaColors.BentoAmberBg,
                                textColor = if (user.isIdentityVerified) DorjaColors.BentoGreenText else DorjaColors.BentoAmberText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationCard(
    v: IdentityVerification,
    subject: User?,
    checksForV: List<com.example.data.model.ThirdPartyCheck>,
    onDecide: (Boolean) -> Unit,
    onRecordCheck: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val context = LocalContext.current
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }
    val imageFile = remember(v.id) { repository.getVerificationImageFile(v) }
    val imageBitmap = remember(v.id) {
        try {
            imageFile?.let { file ->
                // Downsample large photos so review stays smooth on device
                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
                val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 1024)
                val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
            }
        } catch (_: Exception) { null }
    }

    val (bg, fg) = when (v.status) {
        "APPROVED" -> DorjaColors.BentoGreenBg to DorjaColors.BentoGreenText
        "REJECTED" -> DorjaColors.ErrorContainer to DorjaColors.Error
        else -> DorjaColors.BentoAmberBg to DorjaColors.BentoAmberText
    }

    BentoCard(modifier = Modifier.fillMaxWidth().testTag("admin_verification_${v.id}")) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = v.holderName,
                        style = MaterialTheme.typography.titleSmall,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${v.documentKind} · ${v.documentNumberMasked}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
                DorjaBadge(text = verificationStatusLabel(v.status), backgroundColor = bg, textColor = fg)
            }
            Spacer(modifier = Modifier.height(4.dp))
            val unknownAccountLabel = L("admin_unknown_account")
            val submittedLabel = Lf("admin_submitted_at_fmt", dateFmt.format(Date(v.submittedAt)))
            Text(
                text = (subject?.displayName ?: unknownAccountLabel) + " · " + submittedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500
            )

            imageBitmap?.let { bmp ->
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    bitmap = bmp,
                    contentDescription = L("admin_doc_photo_cd"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            if (checksForV.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                checksForV.forEach { c ->
                    Text(
                        text = "• " + checkTypeLabel(c.checkType) + ": " + checkResultLabel(c.result) + (if (c.note.isNotBlank()) " — " + c.note else ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray700,
                        fontSize = 10.sp
                    )
                }
            }

            if (v.reviewNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = Lf("admin_review_note_fmt", v.reviewNote),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray700
                )
            }

            if (v.status == "SUBMITTED" || v.status == "UNDER_REVIEW") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DorjaButton(
                        text = L("admin_approve"),
                        onClick = { onDecide(true) },
                        icon = Icons.Default.VerifiedUser,
                        modifier = Modifier.weight(1f),
                        testTag = "admin_approve_${v.id}"
                    )
                    DorjaOutlinedButton(
                        text = L("admin_reject"),
                        onClick = { onDecide(false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onRecordCheck) {
                        Text(L("admin_record_check"), color = DorjaColors.BentoBlueIcon, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── Localised labels for stored enum codes ────────────────────────────────
// The database keeps stable codes (APPROVED, ISSUER_DATABASE, …); every
// screen renders them through these helpers so the admin console reads in
// the user's language instead of raw codes.

@Composable
private fun verificationStatusLabel(status: String): String = when (status) {
    "APPROVED" -> L("account_verified")
    "REJECTED" -> L("account_rejected")
    else -> L("account_under_review")
}

@Composable
private fun checkTypeLabel(code: String): String = when (code) {
    "ISSUER_DATABASE" -> L("admin_check_issuer_db")
    "DOCUMENT_AUTHENTICITY" -> L("admin_check_doc_auth")
    "SANCTIONS_SCREENING" -> L("admin_check_sanctions")
    else -> code.replace('_', ' ')
}

@Composable
private fun checkResultLabel(code: String): String = when (code) {
    "PASS" -> L("admin_check_pass")
    "FLAGGED" -> L("admin_check_flagged")
    "INCONCLUSIVE" -> L("admin_check_inconclusive")
    else -> code
}

@Composable
private fun roleLabel(role: String): String = when (role) {
    "SELLER" -> L("account_role_host")
    "BUYER" -> L("account_role_buyer")
    "ADMIN" -> L("admin_role_admin")
    else -> role
}
