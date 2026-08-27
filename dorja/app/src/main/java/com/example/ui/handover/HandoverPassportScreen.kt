package com.example.ui.handover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SquareFoot
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.Promise
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaCard
import com.example.ui.components.DorjaInput
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch

@Composable
fun HandoverPassportScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()

    val listing by repository.observeListingById(listingId).collectAsState(initial = null)
    val promises by repository.getPromisesByListing(listingId).collectAsState(initial = emptyList())

    var showAddPromiseDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("HANDOVER_DATE") }
    var newTitle by remember { mutableStateOf("") }
    var newOriginalText by remember { mutableStateOf("") }
    var newEvidenceNote by remember { mutableStateOf("") }

    if (showAddPromiseDialog) {
        AlertDialog(
            onDismissRequest = { showAddPromiseDialog = false },
            title = {
                Text("Log Developer Promise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    DorjaInput(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = "Promise Title",
                        placeholder = "e.g. Handover by Q4 2026",
                        testTag = "promise_title_input"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DorjaInput(
                        value = newOriginalText,
                        onValueChange = { newOriginalText = it },
                        label = "Verbatim Deed Guarantee",
                        placeholder = "Formal contract clause text...",
                        testTag = "promise_clause_input"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DorjaInput(
                        value = newEvidenceNote,
                        onValueChange = { newEvidenceNote = it },
                        label = "Evidence / Document Ref",
                        placeholder = "e.g. Schedule B clause 5.1",
                        testTag = "promise_evidence_input"
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Add to Dossier",
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            scope.launch {
                                repository.addPromise(
                                    listingId = listingId,
                                    category = newCategory,
                                    title = newTitle,
                                    originalText = newOriginalText,
                                    evidenceNote = newEvidenceNote
                                )
                                showAddPromiseDialog = false
                                newTitle = ""
                                newOriginalText = ""
                                newEvidenceNote = ""
                            }
                        }
                    },
                    modifier = Modifier.width(150.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddPromiseDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Paper50)
            .testTag("handover_passport_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.White)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.Paper50)
                        .testTag("handover_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DorjaColors.Ink950
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Handover Passport",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Developer Promise Line & Remedy Clock",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Dossier Overview Banner
            item {
                DorjaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DorjaColors.Ink950
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEGAL DOSSIER RECORD",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Sand300,
                                fontFamily = FontFamily.Monospace
                            )
                            DorjaBadge(
                                text = "AUDITED",
                                icon = Icons.Default.Shield,
                                backgroundColor = DorjaColors.Jol600,
                                textColor = DorjaColors.White
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = listing?.title ?: "Property Handover Warranties",
                            style = MaterialTheme.typography.titleMedium,
                            color = DorjaColors.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All pre-handover architectural commitments recorded for statutory dispute protection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Sand300
                        )
                    }
                }
            }

            // Remedy Clock Card
            item {
                DorjaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DorjaColors.Sand100,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DorjaColors.Teal100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = DorjaColors.Jol600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remedy Grace Period Clock",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "90 Days post-possession audit window active for snagging resolution.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }
                    }
                }
            }

            // Promise Line Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECORDED PROMISES (${promises.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(onClick = { showAddPromiseDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = DorjaColors.Jol600)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Promise", color = DorjaColors.Jol600, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Promises List
            items(promises, key = { it.id }) { promise ->
                PromiseCard(promise = promise)
            }
        }
    }
}

@Composable
private fun PromiseCard(promise: Promise) {
    val categoryIcon = when (promise.category) {
        "HANDOVER_DATE" -> Icons.Default.CalendarMonth
        "UNIT_SIZE_OR_LAYOUT" -> Icons.Default.SquareFoot
        "PARKING" -> Icons.Default.DirectionsCar
        "FIXTURES" -> Icons.Default.Plumbing
        else -> Icons.Default.Assignment
    }

    DorjaCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = DorjaColors.Jol600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = promise.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                }

                val (badgeBg, badgeText) = when (promise.status) {
                    "RESOLVED" -> Pair(DorjaColors.Teal100, DorjaColors.Teal900)
                    "ACKNOWLEDGED" -> Pair(DorjaColors.Sand100, DorjaColors.Ink950)
                    else -> Pair(DorjaColors.Paper50, DorjaColors.Gray700)
                }

                DorjaBadge(
                    text = promise.status,
                    backgroundColor = badgeBg,
                    textColor = badgeText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = promise.originalText,
                style = MaterialTheme.typography.bodyMedium,
                color = DorjaColors.Gray700
            )

            if (promise.evidenceNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DorjaColors.Sand100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = DorjaColors.Gray500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Evidence: ${promise.evidenceNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray700,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
