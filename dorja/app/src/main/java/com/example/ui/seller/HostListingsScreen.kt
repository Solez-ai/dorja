package com.example.ui.seller

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.Listing
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

@Composable
fun HostListingsScreen(
    onCreateListing: () -> Unit,
    onOpenListingDetail: (String) -> Unit,

) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val ownerId = currentUser?.id ?: "u1"

    val myListings by repository.getListingsByOwner(ownerId).collectAsState(initial = emptyList())
    var listingToDelete by remember { mutableStateOf<Listing?>(null) }

    if (listingToDelete != null) {
        AlertDialog(
            onDismissRequest = { listingToDelete = null },
            title = { Text("Delete Property Listing", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${listingToDelete!!.title}'? All associated room data will also be removed.") },
            confirmButton = {
                DorjaButton(
                    text = "Delete",
                    onClick = {
                        scope.launch {
                            repository.deleteListing(listingToDelete!!.id)
                            listingToDelete = null
                        }
                    },
                    modifier = Modifier.width(100.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { listingToDelete = null }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("host_listings_screen")
    ) {
        // Top Bento Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.CanvasBg)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dorja_logo),
                        contentDescription = "Dorja Logo",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "My Properties",
                            style = MaterialTheme.typography.titleLarge,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Host Management Suite • Dorja BD",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                    }
                }

                DorjaButton(
                    text = "+ New Listing",
                    onClick = onCreateListing,
                    icon = Icons.Default.Add,
                    modifier = Modifier.height(40.dp),
                    testTag = "host_new_listing_btn"
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Bento Summary Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoMetricTile(
                        value = "${myListings.size}",
                        label = "MY LISTINGS",
                        icon = Icons.Default.Home,
                        iconBg = DorjaColors.BentoBlueBg,
                        iconTint = DorjaColors.BentoBlueIcon,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        value = "${myListings.count { it.intent == "RENT" }}",
                        label = "FOR RENT",
                        icon = Icons.Default.Apartment,
                        iconBg = DorjaColors.BentoGreenBg,
                        iconTint = DorjaColors.BentoGreenIcon,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        value = "${myListings.count { it.intent == "SALE" }}",
                        label = "FOR SALE",
                        icon = Icons.Default.Shield,
                        iconBg = DorjaColors.BentoPurpleBg,
                        iconTint = DorjaColors.BentoPurpleIcon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (myListings.isEmpty()) {
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
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = DorjaColors.BentoBlueIcon,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Properties Listed Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You are currently in Host mode. Tap the button below to add your first verified property listing with custom rooms and amenities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            DorjaButton(
                                text = "Create Property Listing",
                                onClick = onCreateListing,
                                icon = Icons.Default.Add,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "host_empty_create_btn"
                            )
                        }
                    }
                }
            } else {
                items(myListings, key = { it.id }) { listing ->
                    HostListingCard(
                        listing = listing,
                        onClick = { onOpenListingDetail(listing.id) },

                        onDelete = { listingToDelete = listing }
                    )
                }
            }
        }
    }
}

@Composable
private fun HostListingCard(
    listing: Listing,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("host_listing_${listing.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DorjaBadge(
                            text = if (listing.intent == "RENT") "FOR RENT" else "FOR SALE",
                            backgroundColor = if (listing.intent == "RENT") DorjaColors.BentoBlueBg else DorjaColors.BentoPurpleBg,
                            textColor = if (listing.intent == "RENT") DorjaColors.BentoBlueText else DorjaColors.BentoPurpleText
                        )
                        DorjaBadge(
                            text = listing.status,
                            backgroundColor = DorjaColors.BentoGreenBg,
                            textColor = DorjaColors.BentoGreenText
                        )
                        if (listing.hasScan || !listing.virtualTourUrl.isNullOrBlank()) {
                            DorjaBadge(
                                text = "3D TOUR",
                                icon = Icons.Default.ViewInAr,
                                backgroundColor = DorjaColors.Jol600,
                                textColor = DorjaColors.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = DorjaColors.Gray500
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Listing") },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Listing", color = DorjaColors.Error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DorjaColors.Gray500,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = listing.publicArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.formatPrice(listing.priceAmount, listing.intent),
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Jol600,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${listing.bedrooms} Beds • ${listing.bathrooms} Baths • ${listing.sqft} sqft",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray500,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DorjaButton(
                    text = "View Details",
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    testTag = "host_card_view_${listing.id}"
                )
            }

            if (listing.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listing.tags.split(",").take(3).forEach { tag ->
                        if (tag.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DorjaColors.Sand100,
                                border = BorderStroke(1.dp, DorjaColors.Sand300)
                            ) {
                                Text(
                                    text = tag.trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Ink950,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
