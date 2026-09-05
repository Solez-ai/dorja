package com.example.ui.relocation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.country.CountryRegistry
import com.example.data.country.LiveabilityField
import com.example.ui.components.BentoCard
import com.example.ui.components.CountryPicker
import com.example.ui.components.DorjaBadge
import com.example.ui.theme.DorjaColors

/**
 * Cross-border relocation mode (atlas §8, PLAN.md Phase 3.5).
 *
 * Pick an origin and a destination country and get the destination's
 * disclosure checklist, document rails, language notes, unit conversions and
 * the professional roles the user may need. Data comes entirely from the two
 * [CountryProfile]s — DORJA advises, it never issues legal opinions.
 */
@Composable
fun RelocationModeScreen(
    initialOrigin: String,
    initialDestination: String,
    onBack: () -> Unit = {}
) {
    var origin by remember { mutableStateOf(initialOrigin) }
    var destination by remember { mutableStateOf(initialDestination) }

    val from = CountryRegistry.profile(origin)
    val to = CountryRegistry.profile(destination)

    Scaffold(
        containerColor = DorjaColors.CanvasBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DorjaColors.CanvasBg)
                    .padding(top = 44.dp, start = 8.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DorjaColors.Ink950)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Relocation Mode",
                            style = MaterialTheme.typography.titleLarge,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Move your property evidence across borders",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Country selectors ────────────────────────────────────────────
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MOVING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CountryPicker(selected = origin, onSelect = { origin = it })

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = DorjaColors.BentoPurpleIcon, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MOVING TO",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    CountryPicker(selected = destination, onSelect = { destination = it })
                }
            }

            if (origin == destination) {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Origin and destination are the same market. Pick two different countries to see the cross-border differences.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // ── Market readiness ─────────────────────────────────────────
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DESTINATION MARKET STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DorjaBadge(
                                text = to.confidenceLabel,
                                backgroundColor = if (to.selectable) DorjaColors.BentoGreenBg else DorjaColors.BentoAmberBg,
                                textColor = if (to.selectable) DorjaColors.BentoGreenText else DorjaColors.BentoAmberText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (to.selectable) "Open for transactions on DORJA"
                                else "Research stage — not yet open for transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }
                    }
                }

                // ── Disclosure checklist ─────────────────────────────────────
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "WHAT TO ASK FOR IN ${to.displayName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (to.disclosureChecklist.isEmpty()) {
                            Text(
                                text = "No checklist configured for this market yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        } else {
                            to.disclosureChecklist.forEach { item ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Icon(
                                        Icons.Default.FactCheck,
                                        contentDescription = null,
                                        tint = DorjaColors.BentoBlueIcon,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Ink950)
                                }
                            }
                        }
                    }
                }

                // ── Authority rails ─────────────────────────────────────────
                if (to.govtVerifyUrl != null) {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "OFFICIAL RECORDS",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = DorjaColors.BentoGreenIcon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = to.govtVerifyLabel ?: to.govtVerifyUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You verify records on the official portal yourself; DORJA stores the reference but never queries the registry.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500
                            )
                        }
                    }
                }

                // ── Language & units ─────────────────────────────────────────
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "LANGUAGE & UNITS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = DorjaColors.BentoBlueIcon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = to.primaryLanguages.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Ink950
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Currency: ${to.currencyCode} (${to.currencySymbol}) — from ${from.currencyCode} (${from.currencySymbol})",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Ink950
                        )
                        if (from.liveabilityFields.contains(LiveabilityField.POWER_BACKUP) &&
                            to.liveabilityFields.contains(LiveabilityField.ENERGY_CLASS)
                        ) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Liveability shift: markets like ${to.displayName} emphasise energy classes and heating costs instead of power backup and water supply.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray700
                            )
                        }
                    }
                }

                // ── Professional roles & identity note ───────────────────────
                if (to.professionalRoles.isNotEmpty() || to.identityVerificationNote.isNotBlank()) {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "WHO YOU MAY NEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            to.professionalRoles.forEach { role ->
                                Text("• $role", style = MaterialTheme.typography.bodySmall, color = DorjaColors.Ink950)
                            }
                            if (to.identityVerificationNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = to.identityVerificationNote,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Gray700
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Relocation guidance is general information from DORJA's country profiles, not legal advice. Confirm requirements with a licensed local professional.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }
    }
}
