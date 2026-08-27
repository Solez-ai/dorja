package com.example.ui.visits

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.Viewing
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.components.PulseDot
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun VisitsScreen(
    onOpenPass: (String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val isHost = currentUser?.role == "SELLER"
    val userId = currentUser?.id ?: "u1"

    val viewings by (if (isHost) repository.getViewingsForHost(userId) else repository.getViewingsForSeeker(userId))
        .collectAsState(initial = emptyList())

    val activeViewing = viewings.firstOrNull { it.status == "CONFIRMED" || it.status == "CHECKED_IN" }
    val pastViewings = viewings.filter { it != activeViewing }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("visits_screen")
    ) {
        // Top Bento Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.CanvasBg)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isHost) "Visitor Passes" else "My Viewing Passes",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHost) "Scheduled appointments for your listed properties" else "SafeView geofenced access tokens",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DorjaColors.BentoGreenBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = DorjaColors.BentoGreenIcon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS GATED",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.BentoGreenText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Active Visit Pass
            if (activeViewing != null) {
                item {
                    ActiveViewingBentoCard(
                        viewing = activeViewing,
                        isHost = isHost,
                        onOpenPass = { onOpenPass(activeViewing.id) },
                        onCheckIn = {
                            scope.launch {
                                repository.checkInViewing(activeViewing.id)
                            }
                        }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BentoMetricTile(
                            value = "Active",
                            label = "PASS STATUS",
                            icon = Icons.Default.CheckCircle,
                            iconBg = DorjaColors.BentoGreenBg,
                            iconTint = DorjaColors.BentoGreenIcon,
                            modifier = Modifier.weight(1f)
                        )
                        BentoMetricTile(
                            value = activeViewing.passToken.takeLast(4),
                            label = "SECURITY TOKEN",
                            icon = Icons.Default.QrCode,
                            iconBg = DorjaColors.BentoBlueBg,
                            iconTint = DorjaColors.BentoBlueIcon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (viewings.isEmpty()) {
                item {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.BentoBlueBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = DorjaColors.BentoBlueIcon,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (isHost) "No Visitor Requests Yet" else "No Active Viewing Passes",
                                style = MaterialTheme.typography.titleMedium,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHost) "When seekers book a visit for your properties, their encrypted pass requests will appear here for verification." else "When you book a physical inspection on any property, your SafeView QR access pass will be generated here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                if (pastViewings.isNotEmpty()) {
                    item {
                        Text(
                            text = "COMPLETED & PAST PASSES",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(pastViewings, key = { it.id }) { viewing ->
                        PastViewingBentoCard(viewing = viewing)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveViewingBentoCard(
    viewing: Viewing,
    isHost: Boolean,
    onOpenPass: () -> Unit,
    onCheckIn: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_viewing_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulseDot(color = DorjaColors.Success)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (viewing.status == "CHECKED_IN") "VISITOR CHECKED IN" else "CONFIRMED INSPECTION SLOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Success,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                DorjaBadge(
                    text = viewing.passToken,
                    backgroundColor = DorjaColors.BentoBlueBg,
                    textColor = DorjaColors.BentoBlueText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Listing #${viewing.listingId}",
                style = MaterialTheme.typography.titleMedium,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = DorjaColors.Gray500,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Formatters.formatDateTime(viewing.startsAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DorjaButton(
                    text = "Open QR Pass",
                    onClick = onOpenPass,
                    icon = Icons.Default.QrCode,
                    modifier = Modifier.weight(1f),
                    testTag = "open_pass_button"
                )

                if (isHost && viewing.status != "CHECKED_IN") {
                    DorjaOutlinedButton(
                        text = "Verify & Check In",
                        onClick = onCheckIn,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PastViewingBentoCard(viewing: Viewing) {
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DorjaColors.CanvasBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = DorjaColors.Gray500,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Listing #${viewing.listingId}",
                    style = MaterialTheme.typography.titleSmall,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Formatters.formatDateTime(viewing.startsAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700,
                    fontSize = 12.sp
                )
            }
            DorjaBadge(
                text = viewing.status,
                backgroundColor = DorjaColors.CanvasBg,
                textColor = DorjaColors.Gray700
            )
        }
    }
}
