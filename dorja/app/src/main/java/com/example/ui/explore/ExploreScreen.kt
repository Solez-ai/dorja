package com.example.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onSelectListing: (String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val allListings by repository.getAllListings().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedIntent by remember { mutableStateOf("ALL") }
    var selectedPropertyType by remember { mutableStateOf("ALL") }

    val filteredListings = allListings.filter { listing ->
        val matchesQuery = searchQuery.isBlank() ||
                listing.title.contains(searchQuery, ignoreCase = true) ||
                listing.publicArea.contains(searchQuery, ignoreCase = true) ||
                listing.tags.contains(searchQuery, ignoreCase = true)

        val matchesIntent = selectedIntent == "ALL" || listing.intent.equals(selectedIntent, ignoreCase = true)
        val matchesType = selectedPropertyType == "ALL" || listing.propertyType.equals(selectedPropertyType, ignoreCase = true)

        matchesQuery && matchesIntent && matchesType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("explore_screen")
    ) {
        // Top Header with Gradient Background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DorjaColors.Ink950
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)
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
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dorja Properties",
                                style = MaterialTheme.typography.titleLarge,
                                color = DorjaColors.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(id = R.string.explore_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Sand300
                            )
                        }
                    }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DorjaColors.BentoGreenBg,
                    border = BorderStroke(1.dp, DorjaColors.BentoGreenIcon.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = DorjaColors.BentoGreenIcon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "ANTI-SCAM",
                            style = MaterialTheme.typography.labelMedium,
                            color = DorjaColors.BentoGreenText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }

        // Search and Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.CanvasBg)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            // Search Box Bento Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city or area") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = DorjaColors.BentoBlueIcon
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = DorjaColors.Gray500
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DorjaColors.White,
                    unfocusedContainerColor = DorjaColors.White,
                    focusedBorderColor = DorjaColors.BentoBlueIcon,
                    unfocusedBorderColor = DorjaColors.BentoCardBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    DorjaChip(
                        selected = selectedIntent == "ALL",
                        label = "All Listings",
                        onClick = { selectedIntent = "ALL" },
                        modifier = Modifier.testTag("filter_all")
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedIntent == "RENT",
                        label = "For Rent",
                        onClick = { selectedIntent = "RENT" },
                        modifier = Modifier.testTag("filter_rent")
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedIntent == "SALE",
                        label = "For Sale",
                        onClick = { selectedIntent = "SALE" },
                        modifier = Modifier.testTag("filter_sale")
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedPropertyType == "APARTMENT",
                        label = "Apartments",
                        onClick = {
                            selectedPropertyType = if (selectedPropertyType == "APARTMENT") "ALL" else "APARTMENT"
                        }
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedPropertyType == "HOUSE",
                        label = "Houses",
                        onClick = {
                            selectedPropertyType = if (selectedPropertyType == "HOUSE") "ALL" else "HOUSE"
                        }
                    )
                }
            }
        }

        // Listings List
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Bento Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoMetricTile(
                        value = "${filteredListings.size}",
                        label = "PROPERTIES AVAILABLE",
                        icon = Icons.Default.Apartment,
                        iconBg = DorjaColors.BentoBlueBg,
                        iconTint = DorjaColors.BentoBlueIcon,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        value = "100%",
                        label = "SAFEVIEW GATED",
                        icon = Icons.Default.Shield,
                        iconBg = DorjaColors.BentoGreenBg,
                        iconTint = DorjaColors.BentoGreenIcon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (filteredListings.isEmpty()) {
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
                                text = "No Properties Found",
                                style = MaterialTheme.typography.titleMedium,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No properties match '$searchQuery'." else "No active listings published yet. Properties listed by hosts will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredListings, key = { it.id }) { listing ->
                    ExploreListingCard(
                        listing = listing,
                        onClick = { onSelectListing(listing.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreListingCard(
    listing: Listing,
    onClick: () -> Unit
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("explore_listing_card_${listing.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DorjaBadge(
                    text = if (listing.intent == "RENT") "FOR RENT" else "FOR SALE",
                    backgroundColor = if (listing.intent == "RENT") DorjaColors.BentoBlueBg else DorjaColors.BentoPurpleBg,
                    textColor = if (listing.intent == "RENT") DorjaColors.BentoBlueText else DorjaColors.BentoPurpleText
                )

                if (listing.hasScan || !listing.virtualTourUrl.isNullOrBlank()) {
                    DorjaBadge(
                        text = "3D TOUR AVAILABLE",
                        icon = Icons.Default.ViewInAr,
                        backgroundColor = DorjaColors.Jol600,
                        textColor = DorjaColors.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Public Area
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

            Spacer(modifier = Modifier.height(12.dp))

            // Price & Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.formatPrice(listing.priceAmount, listing.currency, listing.intent),
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
