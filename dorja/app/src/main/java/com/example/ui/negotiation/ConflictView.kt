package com.example.ui.negotiation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppealRecord
import com.example.data.model.Report
import com.example.data.model.ReportReason
import com.example.data.model.ReportResponse
import com.example.ui.components.DorjaBadge
import com.example.ui.theme.DorjaColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evidence-graph conflict view (atlas §8): when two parties provide
 * conflicting claims, DORJA shows both — side by side, with source and
 * date. It never silently chooses one and never shows an opaque score.
 */
@Composable
fun ConflictCard(
    report: Report,
    responses: List<ReportResponse>,
    appeals: List<AppealRecord>,
    modifier: Modifier = Modifier
) {
    val reason = ReportReason.fromCode(report.reason)
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    val stateBg = when (report.state) {
        "RESOLVED" -> DorjaColors.Teal100 to DorjaColors.Teal900
        "WITHDRAWN" -> DorjaColors.Paper50 to DorjaColors.Gray700
        "COUNTERPARTY_RESPONDED" -> DorjaColors.BentoBlueBg to DorjaColors.BentoBlueText
        else -> DorjaColors.BentoAmberBg to DorjaColors.BentoAmberText
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header: reason + state
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Flag,
                contentDescription = null,
                tint = DorjaColors.BentoAmberIcon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = reason.label,
                style = MaterialTheme.typography.titleSmall,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            DorjaBadge(
                text = report.state.replace('_', ' '),
                backgroundColor = stateBg.first,
                textColor = stateBg.second
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Reported ${dateFmt.format(Date(report.createdAt))}",
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.Gray500
        )

        // Side-by-side claims — both parties, equal weight, no winner
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ClaimColumn(
                title = "REPORTING PARTY",
                claim = report.subjectClaim.ifBlank { report.details },
                date = report.createdAt,
                dateFmt = dateFmt,
                modifier = Modifier.weight(1f)
            )
            ClaimColumn(
                title = "COUNTERPARTY",
                claim = responses.lastOrNull()?.counterClaim
                    ?: "No response recorded yet.",
                date = responses.lastOrNull()?.createdAt,
                dateFmt = dateFmt,
                modifier = Modifier.weight(1f)
            )
        }

        // Evidence references supplied by either side
        responses.forEach { response ->
            if (response.evidenceReference.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Balance,
                        contentDescription = null,
                        tint = DorjaColors.Gray500,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Counterparty evidence: ${response.evidenceReference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Resolution note (recorded neutrally, not a verdict)
        if (report.state == "RESOLVED" && report.resolutionNote.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DorjaColors.Teal100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = DorjaColors.Teal900,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resolution: ${report.resolutionNote}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Teal900
                    )
                }
            }
        }

        // Appeals — visible, dated, neutral
        if (appeals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            appeals.forEach { appeal ->
                Text(
                    text = "Appeal (${appeal.state.replace('_', ' ').lowercase()}) ${dateFmt.format(Date(appeal.createdAt))}: ${appeal.grounds}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray700,
                    fontSize = 10.sp
                )
            }
        }

        // Honesty note
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = DorjaColors.Gray500,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Both claims are shown as supplied. DORJA does not judge truth — verify with the listed sources or a licensed professional.",
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ClaimColumn(
    title: String,
    claim: String,
    date: Long?,
    dateFmt: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DorjaColors.Sand100, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.Gray500,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = claim,
            style = MaterialTheme.typography.bodySmall,
            color = DorjaColors.Ink950
        )
        if (date != null) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = dateFmt.format(Date(date)),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500,
                fontSize = 9.sp
            )
        }
    }
}
