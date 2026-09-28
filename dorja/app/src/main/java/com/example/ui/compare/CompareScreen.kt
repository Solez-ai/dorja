@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.data.model.LegalDocument
import com.example.data.model.Listing
import com.example.data.model.RoomItem
import com.example.ui.components.DorjaBadge
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import kotlinx.coroutines.flow.first

// ═══════════════════════════════════════════════════════════════════════
// Decision Board compare. Portrait, no orientation lock, no virtual
// scroll: rows are attributes, columns are the two properties, cells show
// ✓ / ✕ / ? plus an honest Unknown-Data panel and an auto-generated
// viewing checklist built from what the listings do NOT say.
// ═══════════════════════════════════════════════════════════════════════

private enum class CellState { GOOD, BAD, NEUTRAL, UNKNOWN }

private data class CellValue(val text: String, val state: CellState)

private data class BoardRow(val labelKey: String, val left: CellValue, val right: CellValue)

private val ROW_LABELS = listOf(
    "compare_row_price", "compare_row_beds", "compare_row_baths", "compare_row_sqft",
    "compare_row_area", "compare_row_type", "compare_row_intent", "compare_row_docs",
    "compare_row_rooms", "compare_row_rooms_doc", "compare_row_price_sqft",
    "compare_row_sqft_bed", "compare_row_evidence"
)

private val boardIntFormat = java.text.DecimalFormat("#,##0")

/**
 * Derived metrics — computed from raw facts so the board can compare what
 * neither listing states directly. Unknown inputs produce honest "?" cells
 * that flow into the viewing checklist.
 */

/** Price per square foot — lower is better; unknown when area is missing. */
private fun derivedPricePerSqftRow(l: Listing, r: Listing): BoardRow {
    val lRatio = if (l.sqft > 0) l.priceAmount.toDouble() / l.sqft else null
    val rRatio = if (r.sqft > 0) r.priceAmount.toDouble() / r.sqft else null
    val leftCell = when {
        lRatio == null -> CellValue("?", CellState.UNKNOWN)
        rRatio == null -> CellValue(fmtRatio(lRatio), CellState.NEUTRAL)
        lRatio <= rRatio -> CellValue(fmtRatio(lRatio), CellState.GOOD)
        else -> CellValue(fmtRatio(lRatio), CellState.BAD)
    }
    val rightCell = when {
        rRatio == null -> CellValue("?", CellState.UNKNOWN)
        lRatio == null -> CellValue(fmtRatio(rRatio), CellState.NEUTRAL)
        rRatio <= lRatio -> CellValue(fmtRatio(rRatio), CellState.GOOD)
        else -> CellValue(fmtRatio(rRatio), CellState.BAD)
    }
    return boardRow("compare_row_price_sqft", leftCell, rightCell)
}

private fun fmtRatio(v: Double): String = boardIntFormat.format(v) + " / ft²"

/** Floor area per bedroom — higher is better; unknown without bedrooms. */
private fun derivedSpacePerBedRow(l: Listing, r: Listing): BoardRow {
    val lVal = if (l.bedrooms > 0) l.sqft / l.bedrooms else null
    val rVal = if (r.bedrooms > 0) r.sqft / r.bedrooms else null
    val leftCell = when {
        lVal == null -> CellValue("?", CellState.UNKNOWN)
        rVal == null -> CellValue(boardIntFormat.format(lVal) + " ft² / bed", CellState.NEUTRAL)
        lVal >= rVal -> CellValue(boardIntFormat.format(lVal) + " ft² / bed", CellState.GOOD)
        else -> CellValue(boardIntFormat.format(lVal) + " ft² / bed", CellState.BAD)
    }
    val rightCell = when {
        rVal == null -> CellValue("?", CellState.UNKNOWN)
        lVal == null -> CellValue(boardIntFormat.format(rVal) + " ft² / bed", CellState.NEUTRAL)
        rVal >= lVal -> CellValue(boardIntFormat.format(rVal) + " ft² / bed", CellState.GOOD)
        else -> CellValue(boardIntFormat.format(rVal) + " ft² / bed", CellState.BAD)
    }
    return boardRow("compare_row_sqft_bed", leftCell, rightCell)
}

/** Evidence completeness: how many of the 6 core facts this listing states. */
private fun evidenceScore(l: Listing): Int {
    var score = 0
    if (l.floodRisk?.isNotBlank() == true) score++
    if (l.powerBackup?.isNotBlank() == true) score++
    if (l.waterSupply?.isNotBlank() == true) score++
    if (l.buildingAgeYears != null) score++
    if (l.galleryUris.isNotBlank() || !l.coverPhotoUrl.isNullOrBlank()) score++
    if (!l.virtualTourUrl.isNullOrBlank() || l.hasScan) score++
    return score
}

private fun derivedEvidenceRow(l: Listing, r: Listing): BoardRow {
    val ls = evidenceScore(l)
    val rs = evidenceScore(r)
    val leftCell = when {
        ls > rs -> CellValue("$ls/6 facts", CellState.GOOD)
        ls < rs -> CellValue("$ls/6 facts", CellState.BAD)
        else -> CellValue("$ls/6 facts", CellState.NEUTRAL)
    }
    val rightCell = when {
        rs > ls -> CellValue("$rs/6 facts", CellState.GOOD)
        rs < ls -> CellValue("$rs/6 facts", CellState.BAD)
        else -> CellValue("$rs/6 facts", CellState.NEUTRAL)
    }
    return boardRow("compare_row_evidence", leftCell, rightCell)
}

/** Rooms documented on the case-file — higher is better. */
private fun derivedRoomsDocRow(leftRooms: List<RoomItem>, rightRooms: List<RoomItem>): BoardRow {
    val l = leftRooms.size
    val r = rightRooms.size
    val leftCell = when {
        l > r -> CellValue("$l rooms", CellState.GOOD)
        l < r -> CellValue("$l rooms", CellState.BAD)
        else -> CellValue("$l rooms", CellState.NEUTRAL)
    }
    val rightCell = when {
        r > l -> CellValue("$r rooms", CellState.GOOD)
        r < l -> CellValue("$r rooms", CellState.BAD)
        else -> CellValue("$r rooms", CellState.NEUTRAL)
    }
    return boardRow("compare_row_rooms_doc", leftCell, rightCell)
}

/** Listing facts DORJA does not track — surfaced honestly, never faked. */
private val UNKNOWN_DATA_KEYS = listOf(
    "flood history", "parking allocation", "water supply schedule",
    "power backup capacity", "service charge", "building age",
    "ownership documents", "noise levels", "internet availability"
)

private fun unknownKeySentence(raw: String): String =
    raw.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }

private fun boardPrice(amount: Int): Double = amount.toDouble()

private fun buildBoard(
    left: Listing,
    right: Listing,
    leftRooms: List<RoomItem>,
    rightRooms: List<RoomItem>,
    leftDocs: List<LegalDocument>,
    rightDocs: List<LegalDocument>
): List<BoardRow> {
    fun cell(better: Boolean, yours: String, theirs: String, tie: CellState = CellState.NEUTRAL) =
        if (better) CellValue(yours, CellState.GOOD) else CellValue(theirs, CellState.BAD)

    fun priceRow(a: Int, b: Int) = boardRow(
        "compare_row_price",
        when {
            a < b -> CellValue(Formatters.formatPriceShort(a, "BDT"), CellState.GOOD)
            a > b -> CellValue(Formatters.formatPriceShort(a, "BDT"), CellState.BAD)
            else -> CellValue(Formatters.formatPriceShort(a, "BDT"), CellState.NEUTRAL)
        },
        when {
            b < a -> CellValue(Formatters.formatPriceShort(b, "BDT"), CellState.GOOD)
            b > a -> CellValue(Formatters.formatPriceShort(b, "BDT"), CellState.BAD)
            else -> CellValue(Formatters.formatPriceShort(b, "BDT"), CellState.NEUTRAL)
        }
    )

    return listOf(
        priceRow(left.priceAmount, right.priceAmount),
        boardRow(
            "compare_row_beds",
            cell(left.bedrooms >= right.bedrooms, left.bedrooms.toString() + "+", right.bedrooms.toString() + "+")
        ),
        boardRow(
            "compare_row_baths",
            cell(left.bathrooms >= right.bathrooms, left.bathrooms.toString(), right.bathrooms.toString())
        ),
        boardRow(
            "compare_row_sqft",
            cell(left.sqft >= right.sqft, left.sqft.toString() + " ft²", right.sqft.toString() + " ft²")
        ),
        boardRow(
            "compare_row_area",
            cell(
                left.publicArea.isNotBlank() && left.publicArea == right.publicArea,
                left.publicArea, right.publicArea
            )
        ),
        boardRow(
            "compare_row_type",
            CellValue(left.propertyType.lowercase().replaceFirstChar { it.uppercase() }, CellState.NEUTRAL),
            CellValue(right.propertyType.lowercase().replaceFirstChar { it.uppercase() }, CellState.NEUTRAL)
        ),
        boardRow(
            "compare_row_intent",
            CellValue(left.intent, CellState.NEUTRAL),
            CellValue(right.intent, CellState.NEUTRAL)
        ),
        boardRow(
            "compare_row_docs",
            cell(
                leftDocs.size >= rightDocs.size,
                leftDocs.size.toString() + " documents",
                rightDocs.size.toString() + " documents"
            )
        ),
        boardRow(
            "compare_row_rooms",
            cell(
                leftRooms.count { it.has3DScan } >= rightRooms.count { it.has3DScan },
                leftRooms.count { it.has3DScan }.toString() + " 3D scans",
                rightRooms.count { it.has3DScan }.toString() + " 3D scans"
            )
        ),
        derivedRoomsDocRow(leftRooms, rightRooms),
        derivedPricePerSqftRow(left, right),
        derivedSpacePerBedRow(left, right),
        derivedEvidenceRow(left, right)
    )
}

private fun boardRow(labelKey: String, left: CellValue, right: CellValue = left) =
    BoardRow(labelKey, left, right)

private fun buildUnknowns(left: Listing, right: Listing): Map<String, List<String>> {
    fun unknownsOf(l: Listing): List<String> = UNKNOWN_DATA_KEYS.filter { key ->
        when (key) {
            "building age" -> l.buildingAgeYears == null
            "flood history" -> l.floodRisk.isNullOrBlank()
            "power backup capacity" -> l.powerBackup.isNullOrBlank()
            "water supply schedule" -> l.waterSupply.isNullOrBlank()
            else -> true
        }
    }
    return mapOf("compare_unknown_yours" to unknownsOf(left), "compare_unknown_theirs" to unknownsOf(right))
}

private fun buildChecklist(board: List<BoardRow>): List<String> =
    ROW_LABELS.filter { key ->
        val row = board.firstOrNull { it.labelKey == key }
        row != null && (row.left.state == CellState.UNKNOWN || row.right.state == CellState.UNKNOWN)
    }.ifEmpty {
        // Even a fully-populated board leaves physical checks open.
        listOf("compare_row_docs", "compare_row_rooms")
    }

@Composable
fun CompareScreen(
    listingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository

    val leftListing by repository.observeListingById(listingId).collectAsState(initial = null)
    val leftRooms by repository.getRoomsByListing(listingId).collectAsState(initial = emptyList())
    val leftDocs by repository.getLegalDocumentsByListing(listingId).collectAsState(initial = emptyList())

    var compareTargetId by remember { mutableStateOf<String?>(null) }

    val rightListing by produceState<Listing?>(initialValue = null, key1 = compareTargetId) {
        value = compareTargetId?.let { repository.getListingById(it) }
    }
    val rightRooms by produceState<List<RoomItem>>(initialValue = emptyList(), key1 = compareTargetId) {
        value = compareTargetId?.let { repository.getRoomsByListingSync(it) } ?: emptyList()
    }
    val rightDocs by produceState<List<LegalDocument>>(initialValue = emptyList(), key1 = compareTargetId) {
        value = compareTargetId?.let { repository.getLegalDocumentsByListing(it).first() } ?: emptyList()
    }

    if (leftListing == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DorjaColors.CanvasBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = DorjaColors.Jol600)
        }
        return
    }

    val board = if (rightListing != null && compareTargetId != null) {
        buildBoard(leftListing!!, rightListing!!, leftRooms, rightRooms, leftDocs, rightDocs)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .statusBarsPadding()
    ) {
        // ── Top bar — iOS plain style ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("compare_back")) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DorjaColors.Jol600
                )
            }
            Text(
                text = L("compare_title"),
                style = MaterialTheme.typography.titleLarge,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (board != null) {
                IconButton(
                    onClick = { compareTargetId = null },
                    modifier = Modifier.testTag("compare_switch_button")
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = L("compare_swap"),
                        tint = DorjaColors.Jol600
                    )
                }
            }
        }

        if (board == null) {
            ComparePickStage(
                excludeId = listingId,
                onPick = { compareTargetId = it }
            )
        } else {
            DecisionBoard(
                board = board,
                unknowns = buildUnknowns(leftListing!!, rightListing!!),
                checklist = buildChecklist(board),
                left = leftListing!!,
                right = rightListing!!
            )
        }
    }
}

// ─── Stage 1: pick the comparison target ──────────────────────────────

@Composable
private fun ComparePickStage(
    excludeId: String,
    onPick: (String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val allListings by repository.getAllListings().collectAsState(initial = emptyList())
    val candidates = allListings.filter { it.id != excludeId && it.status == "ACTIVE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = L("compare_pick_title"),
            style = MaterialTheme.typography.headlineMedium,
            color = DorjaColors.Ink950,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = L("compare_pick_choose"),
            style = MaterialTheme.typography.bodySmall,
            color = DorjaColors.Gray700
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (candidates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L("compare_pick_empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DorjaColors.Gray700,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(candidates, key = { it.id }) { listing ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(listing.id) }
                            .testTag("compare_pick_${listing.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = DorjaColors.White,
                        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DorjaColors.Sand300)
                            ) {
                                val photo = listing.coverPhotoUrl
                                    ?: listing.galleryUris.lineSequence()
                                        .firstOrNull { it.isNotBlank() }
                                if (photo != null) {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = null,
                                        tint = DorjaColors.Gray500,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = listing.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = listing.publicArea + "  ·  " +
                                            Formatters.formatPriceShort(listing.priceAmount, "BDT"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Gray700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DorjaBadge(
                                text = listing.intent,
                                backgroundColor = DorjaColors.Jol100,
                                contentColor = DorjaColors.Jol700
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Stage 2: the Decision Board ──────────────────────────────────────

@Composable
private fun DecisionBoard(
    board: List<BoardRow>,
    unknowns: Map<String, List<String>>,
    checklist: List<String>,
    left: Listing,
    right: Listing
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("compare_board"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Column headers
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BoardHeaderCard(listing = left, isYours = true, modifier = Modifier.weight(1f))
                BoardHeaderCard(listing = right, isYours = false, modifier = Modifier.weight(1f))
            }
        }

        // Attribute rows: each row = label + two aligned value cells.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    board.forEach { row ->
                        BoardCell(value = row.left)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    board.forEach { row ->
                        BoardCell(value = row.right)
                    }
                }
            }
        }

        // Row labels (rendered between the two columns visually as a list)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            ROW_LABELS.forEach { key ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = L(key),
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Unknown data panel — what DORJA does NOT know
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = L("compare_unknown_title"),
                style = MaterialTheme.typography.titleSmall,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UnknownColumn(
                        labelKey = "compare_unknown_yours",
                        items = unknowns["compare_unknown_yours"].orEmpty()
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UnknownColumn(
                        labelKey = "compare_unknown_theirs",
                        items = unknowns["compare_unknown_theirs"].orEmpty()
                    )
                }
            }
        }

        // Viewing checklist — auto-generated from the unknowns
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = L("compare_checklist_title"),
                style = MaterialTheme.typography.titleSmall,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DorjaColors.White,
                border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    checklist.forEach { key ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = DorjaColors.BentoGreenIcon,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = L(key),
                                style = MaterialTheme.typography.bodyMedium,
                                color = DorjaColors.Ink950
                            )
                        }
                    }
                    if (checklist.isEmpty()) {
                        Text(
                            text = L("compare_checklist_empty"),
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardHeaderCard(listing: Listing, isYours: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DorjaColors.White,
        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(DorjaColors.Sand300)
            ) {
                val photo = listing.coverPhotoUrl
                    ?: listing.galleryUris.lineSequence().firstOrNull { it.isNotBlank() }
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        tint = DorjaColors.Gray500,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
                DorjaBadge(
                    text = if (isYours) L("compare_unknown_yours") else L("compare_unknown_theirs"),
                    backgroundColor = DorjaColors.Jol100,
                    contentColor = DorjaColors.Jol700,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = DorjaColors.Gray500,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = listing.publicArea,
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = Formatters.formatPriceShort(listing.priceAmount, "BDT"),
                    style = MaterialTheme.typography.titleSmall,
                    color = DorjaColors.Jol600,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BoardCell(value: CellValue) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = when (value.state) {
            CellState.GOOD -> DorjaColors.BentoGreenBg
            CellState.BAD -> DorjaColors.BentoAmberBg
            CellState.NEUTRAL -> DorjaColors.Sand100
            CellState.UNKNOWN -> DorjaColors.Sand100
        },
        border = BorderStroke(
            0.5.dp,
            when (value.state) {
                CellState.GOOD -> DorjaColors.BentoGreenIcon.copy(alpha = 0.35f)
                CellState.BAD -> DorjaColors.BentoAmberIcon.copy(alpha = 0.35f)
                else -> DorjaColors.BentoCardBorder
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (value.state) {
                    CellState.GOOD -> Icons.Default.Check
                    CellState.BAD -> Icons.Default.Close
                    CellState.NEUTRAL -> Icons.Default.Check
                    CellState.UNKNOWN -> Icons.Default.QuestionMark
                },
                contentDescription = null,
                tint = when (value.state) {
                    CellState.GOOD -> DorjaColors.BentoGreenIcon
                    CellState.BAD -> DorjaColors.BentoAmberIcon
                    CellState.NEUTRAL -> DorjaColors.Gray600
                    CellState.UNKNOWN -> DorjaColors.Gray500
                },
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value.text,
                style = MaterialTheme.typography.labelMedium,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UnknownColumn(labelKey: String, items: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DorjaColors.Sand100,
        border = BorderStroke(0.5.dp, DorjaColors.BentoCardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = L(labelKey),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray600,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (items.isEmpty()) {
                Text(
                    text = L("compare_unknown_none"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500
                )
            } else {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.QuestionMark,
                            contentDescription = null,
                            tint = DorjaColors.Gray500,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = unknownKeySentence(item),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Ink950
                        )
                    }
                }
            }
        }
    }
}
