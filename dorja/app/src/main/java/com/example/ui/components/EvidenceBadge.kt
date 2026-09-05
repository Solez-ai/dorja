package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EvidenceLevel
import com.example.ui.theme.DorjaColors

/**
 * Renders the atlas §3 evidence vocabulary for a document/claim.
 *
 * Honesty rule: green (confirmed) visuals appear ONLY for
 * ISSUER_CONFIRMED, GOVERNMENT_SOURCE_LINKED, or INDEPENDENTLY_INSPECTED.
 * An upload alone never renders green.
 */
@Composable
fun EvidenceBadge(
    level: EvidenceLevel,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (level) {
        EvidenceLevel.SELF_DECLARED ->
            Triple(DorjaColors.BentoAmberBg, DorjaColors.BentoAmberText, Icons.Default.Info)
        EvidenceLevel.COUNTERPARTY_CONFIRMED ->
            Triple(DorjaColors.BentoBlueBg, DorjaColors.BentoBlueText, Icons.Default.Info)
        EvidenceLevel.ISSUER_CONFIRMED,
        EvidenceLevel.GOVERNMENT_SOURCE_LINKED,
        EvidenceLevel.INDEPENDENTLY_INSPECTED ->
            Triple(DorjaColors.BentoGreenBg, DorjaColors.BentoGreenText, Icons.Default.VerifiedUser)
        EvidenceLevel.EXPIRED, EvidenceLevel.DISPUTED ->
            Triple(DorjaColors.ErrorContainer, DorjaColors.Error, Icons.Default.Info)
        EvidenceLevel.NOT_PROVIDED ->
            Triple(DorjaColors.Sand300, DorjaColors.Gray700, Icons.Default.Info)
    }
    BadgeChip(bg = bg, fg = fg, icon = icon, label = level.label, modifier = modifier)
}

@Composable
private fun BadgeChip(
    bg: Color,
    fg: Color,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontSize = 9.sp
        )
    }
}