package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DorjaColors

/**
 * Link-out card for verifying a document against the official government
 * portal (atlas §4 authority rails). Also used as an inline chip beside a
 * document that carries an official source link.
 *
 * Honesty rule (atlas §3): DORJA never queries the registry itself. The
 * green "Government source linked" badge means the *uploader* attached an
 * official source reference — the viewer is always given the portal so
 * they can check it independently.
 */
@Composable
fun GovernmentSourceCard(
    url: String,
    label: String,
    modifier: Modifier = Modifier,
    title: String = "Check on the official portal",
    note: String = "DORJA does not query the registry. Opening the portal is a user action; the attached record is a user-supplied upload.",
    compact: Boolean = false
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    if (compact) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(DorjaColors.BentoBlueBg)
                .clickable {
                    try {
                        ctx.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        )
                    } catch (_: Exception) {
                    }
                }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = DorjaColors.BentoBlueIcon,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.BentoBlueIcon,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DorjaColors.BentoBlueBg)
            .clickable {
                try {
                    ctx.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    )
                } catch (_: Exception) {
                }
            }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = DorjaColors.BentoBlueIcon,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = DorjaColors.BentoBlueText,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = DorjaColors.BentoBlueIcon,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = DorjaColors.BentoBlueIcon,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = note,
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.BentoBlueText,
            fontSize = 10.sp
        )
    }
}
