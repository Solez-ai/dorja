package com.example.ui.capture

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
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.DorjaApp
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaCard
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters

@Composable
fun CaptureScreen(
    onCreateListing: () -> Unit,
    onOpenListing: (String) -> Unit,
    onScan3DRooms: (String) -> Unit = {},
) {
    val repository = DorjaApp.instance.repository
    val currentUser by repository.currentUser.collectAsState()
    val ownerId = currentUser?.id ?: "u1"
    val myListings by repository.getListingsByOwner(ownerId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Paper50)
            .testTag("capture_screen")
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Trust Verification Hub",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create listings and scan 3D Reality Passports",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }

                DorjaBadge(
                    text = "SELLER SUITE",
                    backgroundColor = DorjaColors.Teal100,
                    textColor = DorjaColors.Teal900
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Action Cards
            item {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500,
                    fontFamily = FontFamily.Monospace
                )
            }

            // 1. Add Property
            item {
                ActionHubCard(
                    title = "Add New Property",
                    description = "Publish apartment, house, or room with scam-protected address verification.",
                    icon = Icons.Default.AddHome,
                    accentColor = DorjaColors.Jol600,
                    onClick = onCreateListing,
                    testTag = "action_add_property"
                )
            }

            // 2. 3D Room Scanner
            item {
                ActionHubCard(
                    title = "3D Room Scanner",
                    description = "Capture 360° cylindrical panoramas with gyroscope-guided alignment.",
                    icon = Icons.Default.ViewInAr,
                    accentColor = Color(0xFF00BCD4),
                    onClick = { /* Navigate from listing detail */ },
                    testTag = "action_3d_scanner"
                )
            }

            // 3. Guided Photo Capture
            item {
                ActionHubCard(
                    title = "Guided Optical Capture",
                    description = "Anti-distortion wide-angle photo sequencing for structural verification.",
                    icon = Icons.Default.CameraAlt,
                    accentColor = DorjaColors.Gray700,
                    onClick = { /* TODO: implement guided photo capture */ },
                    testTag = "action_photo_capture"
                )
            }

            // Section: My Listed Properties
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY VERIFIED LISTINGS (${myListings.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (myListings.isEmpty()) {
                item {
                    DorjaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DorjaColors.White
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No properties published yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DorjaColors.Gray700
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Click 'Add New Property' above to create your first listing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray500
                            )
                        }
                    }
                }
            } else {
                items(myListings, key = { it.id }) { listing ->
                    DorjaCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenListing(listing.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DorjaColors.Ink950),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (listing.hasScan) Icons.Default.ViewInAr else Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (listing.hasScan) DorjaColors.Jol600 else DorjaColors.Sand300,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = listing.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${listing.publicArea} • ${Formatters.formatPriceShort(listing.priceAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray700
                                )
                            }
                            if (listing.hasScan) {
                                DorjaBadge(text = "3D PASS", backgroundColor = DorjaColors.Teal100, textColor = DorjaColors.Teal900)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DorjaColors.Gray500
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionHubCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    DorjaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DorjaColors.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
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
