@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalCupertinoApi::class)

package com.example.ui.history

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.DorjaApp
import com.example.data.model.HistoryEvent
import com.example.data.model.HistoryEventTypes
import com.example.ui.components.DorjaBadge
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.DorjaFontFamily
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ────────────────────────────────────────────────────────────────────
// Timeline grouping: newest first, grouped by day under year headers —
// the "2026 / 28 NOV" rhythm.
// ────────────────────────────────────────────────────────────────────
private data class DayGroup(
    val dayHeader: String,
    val year: String,
    val events: List<HistoryEvent>
)

private val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val fullFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())

private fun groupEvents(events: List<HistoryEvent>): List<DayGroup> {
    val sorted = events.sortedByDescending { it.occurredAt }
    val groups = mutableListOf<DayGroup>()
    var currentHeader: String? = null
    var bucket = mutableListOf<HistoryEvent>()
    for (event in sorted) {
        val header = dayFormat.format(Date(event.occurredAt)).uppercase()
        if (currentHeader == null) currentHeader = header
        if (header != currentHeader) {
            groups.add(
                DayGroup(
                    currentHeader!!,
                    yearFormat.format(Date(bucket.first().occurredAt)),
                    bucket
                )
            )
            bucket = mutableListOf()
            currentHeader = header
        }
        bucket.add(event)
    }
    if (bucket.isNotEmpty() && currentHeader != null) {
        groups.add(
            DayGroup(
                currentHeader!!,
                yearFormat.format(Date(bucket.first().occurredAt)),
                bucket
            )
        )
    }
    return groups
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = DorjaApp.instance.repository
    val user by repository.currentUser.collectAsState()
    val events by repository.observeHistoryForUser(user?.id ?: "")
        .collectAsState(initial = emptyList())

    var selected by remember { mutableStateOf<HistoryEvent?>(null) }
    var showContradictionFor by remember { mutableStateOf<HistoryEvent?>(null) }
    var contradictionNote by remember { mutableStateOf("") }
    var exporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // Strings resolved in composition (L() is @Composable).
    val sTitle = L("history_title")
    val sIntro = L("history_intro")
    val sEmpty = L("history_empty")
    val sEmptySub = L("history_empty_sub")
    val sExport = L("history_export")
    val sWho = L("history_field_who")
    val sWhat = L("history_field_what")
    val sWhen = L("history_field_when")
    val sWhere = L("history_field_where")
    val sCaptured = L("history_field_captured")
    val sRelationship = L("history_field_relationship")
    val sIntegrity = L("history_field_integrity")
    val sLock = L("history_lock_action")
    val sLocked = L("history_locked")
    val sContradiction = L("history_contradiction_action")
    val sCopyHash = L("history_copy_fingerprint")
    val sNotRegistry = L("history_not_registry")
    val sUnknown = L("history_unknown_place")
    val sRecordedBy = L("history_recorded_by_you")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
    ) {
        // Top bar — iOS plain style with trailing Export action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< " + L("common_back"),
                style = MaterialTheme.typography.bodyLarge,
                color = DorjaColors.Jol600,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sTitle,
                style = MaterialTheme.typography.titleLarge,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (events.isNotEmpty()) {
                CupertinoButton(
                    onClick = {
                        if (!exporting) {
                            exporting = true
                            scope.launch {
                                val file = HistoryPdfGenerator.generate(context, events, sNotRegistry)
                                exporting = false
                                if (file != null) sharePdf(context, file)
                            }
                        }
                    },
                    colors = CupertinoButtonDefaults.plainButtonColors(
                        contentColor = DorjaColors.Jol600
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (exporting) "…" else sExport,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = sEmpty,
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = sEmptySub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DorjaColors.Gray700
                )
            }
        } else {
            val groups = remember(events) { groupEvents(events) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = sIntro,
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
                groups.forEach { group ->
                    item(key = "header_" + group.dayHeader + "_" + group.events.first().id) {
                        Column {
                            Text(
                                text = group.year,
                                style = MaterialTheme.typography.titleLarge,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = group.dayHeader,
                                style = MaterialTheme.typography.labelMedium,
                                color = DorjaColors.Gray500,
                                fontFamily = DorjaFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    group.events.forEach { event ->
                        item(key = event.id) {
                            HistoryEventCard(event = event, onClick = { selected = event })
                        }
                    }
                }
                item {
                    Text(
                        text = sNotRegistry,
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500
                    )
                }
            }
        }
    }

    // Evidence Record detail — iOS sheet
    val selectedEvent = selected
    if (selectedEvent != null) {
        ModalBottomSheet(
            onDismissRequest = { selected = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DorjaBadge(
                        text = typeLabel(selectedEvent.type),
                        backgroundColor = DorjaColors.Jol100,
                        contentColor = DorjaColors.Jol700
                    )
                    if (selectedEvent.locked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        DorjaBadge(
                            text = sLocked,
                            backgroundColor = DorjaColors.BentoGreenBg,
                            contentColor = DorjaColors.BentoGreenText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedEvent.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                EvidenceRow(sWho, sRecordedBy)
                EvidenceRow(sWhat, typeLabel(selectedEvent.type))
                EvidenceRow(sWhen, fullFormat.format(Date(selectedEvent.occurredAt)))
                EvidenceRow(sWhere, selectedEvent.place.ifBlank { sUnknown })
                EvidenceRow(
                    sCaptured,
                    parseDetailSummary(selectedEvent.detailJson).ifBlank { "—" }
                )
                EvidenceRow(
                    sRelationship,
                    selectedEvent.listingLabel.ifBlank { selectedEvent.listingId ?: "—" }
                )
                EvidenceRow(sIntegrity, selectedEvent.integrityHash.ifBlank { "—" })

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!selectedEvent.locked) {
                        CupertinoButton(
                            onClick = {
                                scope.launch {
                                    val locked = repository.lockHistoryEvent(selectedEvent.id)
                                    locked.getOrNull()?.let { selected = it }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = CupertinoButtonDefaults.filledButtonColors(
                                containerColor = DorjaColors.Jol600
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = sLock,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        CupertinoButton(
                            onClick = { showContradictionFor = selectedEvent },
                            modifier = Modifier.weight(1f),
                            colors = CupertinoButtonDefaults.plainButtonColors(
                                contentColor = DorjaColors.Error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = sContradiction,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    CupertinoButton(
                        onClick = { clipboard.setText(AnnotatedString(selectedEvent.integrityHash)) },
                        colors = CupertinoButtonDefaults.plainButtonColors(
                            contentColor = DorjaColors.Jol600
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = sCopyHash,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }

    // Contradiction Event dialog
    val contradictionTarget = showContradictionFor
    if (contradictionTarget != null) {
        AlertDialog(
            onDismissRequest = { showContradictionFor = null },
            title = {
                Text(
                    text = L("history_contradiction_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = L("history_contradiction_of") + " \"" + contradictionTarget.title + "\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = contradictionNote,
                        onValueChange = { contradictionNote = it },
                        placeholder = { Text(L("history_contradiction_hint")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Text(
                    text = L("common_confirm"),
                    color = DorjaColors.Error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch {
                                repository.recordContradiction(
                                    originalEventId = contradictionTarget.id,
                                    note = contradictionNote
                                )
                                showContradictionFor = null
                                contradictionNote = ""
                                selected = null
                            }
                        }
                        .padding(8.dp)
                )
            },
            dismissButton = {
                Text(
                    text = L("common_cancel"),
                    color = DorjaColors.Gray700,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showContradictionFor = null }
                        .padding(8.dp)
                )
            }
        )
    }
}

@Composable
private fun HistoryEventCard(event: HistoryEvent, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = DorjaColors.White,
        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFormat.format(Date(event.occurredAt)),
                    style = MaterialTheme.typography.labelMedium,
                    color = DorjaColors.Gray500,
                    fontFamily = DorjaFontFamily,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                DorjaBadge(
                    text = typeLabel(event.type),
                    backgroundColor = if (event.type == HistoryEventTypes.CONTRADICTION_REPORTED)
                        DorjaColors.BentoAmberBg else DorjaColors.Jol100,
                    contentColor = if (event.type == HistoryEventTypes.CONTRADICTION_REPORTED)
                        DorjaColors.BentoAmberText else DorjaColors.Jol700
                )
                Spacer(modifier = Modifier.weight(1f))
                if (event.locked) {
                    Text(
                        text = event.integrityHash,
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.BentoGreenText,
                        fontFamily = DorjaFontFamily
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (event.place.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.place,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EvidenceRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.Gray500,
            fontFamily = DorjaFontFamily,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DorjaColors.Ink950
        )
    }
}

private fun typeLabel(type: String): String = when (type) {
    HistoryEventTypes.VIEWING_PASS_ISSUED -> "Viewing pass"
    HistoryEventTypes.VIEWING_CHECKED_IN -> "Checked in"
    HistoryEventTypes.VIEWING_CHECKED_OUT -> "Checked out"
    HistoryEventTypes.DOCUMENT_ADDED -> "Document added"
    HistoryEventTypes.SELLER_CLAIM -> "Seller claim"
    HistoryEventTypes.LISTING_CREATED -> "Listing created"
    HistoryEventTypes.STATEMENT_LOCKED -> "Statement record"
    HistoryEventTypes.CONTRADICTION_REPORTED -> "Contradiction"
    else -> type.lowercase().replace('_', ' ')
}

private fun parseDetailSummary(detailJson: String): String {
    if (detailJson.isBlank() || detailJson == "{}") return ""
    return try {
        val map = com.google.gson.Gson().fromJson(
            detailJson,
            object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
        ) as? Map<String, String> ?: return ""
        map.entries.joinToString("\n") { (key, value) ->
            key.replace('_', ' ').replaceFirstChar { it.uppercase() } + ": " + value
        }
    } catch (_: Exception) {
        ""
    }
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "DORJA Property History"), null)
}

/**
 * Renders the Export Property History PDF: timeline, evidence references,
 * timestamps and fingerprints. Opens with the honesty disclaimer and closes
 * with it — DORJA presents records; it is not a legal certification.
 */
private object HistoryPdfGenerator {
    fun generate(context: Context, events: List<HistoryEvent>, disclaimer: String): File? {
        return try {
            val doc = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 44f
            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textSize = 18f
            }
            val subPaint = Paint().apply {
                typeface = Typeface.SANS_SERIF
                textSize = 10f
                color = 0xFF555555.toInt()
            }
            val bodyPaint = Paint().apply {
                typeface = Typeface.SANS_SERIF
                textSize = 11f
                color = 0xFF111111.toInt()
            }
            val italicPaint = Paint().apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                textSize = 9f
                color = 0xFF666666.toInt()
            }

            var pageNumber = 1
            var page = doc.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas = page.canvas
            var y = margin
            val nowText = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

            fun newPage() {
                doc.finishPage(page)
                pageNumber += 1
                page = doc.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin
            }

            canvas.drawText("DORJA — Property History Report", margin, y, titlePaint)
            y += 16f
            canvas.drawText("Generated $nowText", margin, y, subPaint)
            y += 14f
            canvas.drawText(disclaimer, margin, y, italicPaint)
            y += 24f

            val sorted = events.sortedByDescending { it.occurredAt }
            for (event in sorted) {
                if (y > pageHeight - 90f) newPage()
                val dateLine = fullFormat.format(Date(event.occurredAt)) +
                        "  ·  " + typeLabel(event.type) +
                        (if (event.locked) "  ·  LOCKED " + event.integrityHash else "")
                canvas.drawText(dateLine, margin, y, subPaint)
                y += 14f
                canvas.drawText(event.title, margin, y, bodyPaint)
                y += 13f
                canvas.drawText("Where: " + event.place.ifBlank { "-" }, margin, y, subPaint)
                y += 12f
                if (event.integrityHash.isNotBlank()) {
                    canvas.drawText("Fingerprint: " + event.integrityHash, margin, y, subPaint)
                    y += 12f
                }
                y += 8f
            }

            if (y > pageHeight - 60f) newPage()
            canvas.drawText(disclaimer, margin, pageHeight - 44f, italicPaint)

            doc.finishPage(page)
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val outFile = File(dir, "dorja_property_history_" + System.currentTimeMillis() + ".pdf")
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            outFile
        } catch (_: Exception) {
            null
        }
    }
}
