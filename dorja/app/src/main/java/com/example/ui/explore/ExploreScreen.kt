@file:OptIn(ExperimentalCupertinoApi::class)

package com.example.ui.explore

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.LegalDocument
import com.example.data.model.Listing
import com.example.data.repository.DorjaRepository
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.slapps.cupertino.CupertinoSearchTextField
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.i18n.L
import com.example.ui.i18n.Lf
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import com.example.ui.components.DorjaLogo
import com.example.ui.util.Formatters.formatPriceShort

import kotlinx.coroutines.launch

/** Price ceiling options (BDT) for the granular filter sheet. */
private val PRICE_CEILINGS = listOf(5_000_000, 20_000_000, 50_000_000, 100_000_000, 500_000_000)

/** Feed ordering options for the Explore screen. */
private enum class ListingSort(val key: String) {
    NEWEST("sort_newest"),
    PRICE_LOW("sort_price_low"),
    PRICE_HIGH("sort_price_high"),
    LARGEST("sort_largest"),
    MOST_BEDS("sort_beds")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    onSelectListing: (String) -> Unit,
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val allListings by repository.getAllListings().collectAsState(initial = emptyList())
    // Evidence-gated status per listing (atlas §3 honesty rule)
    val docsByListing by produceState(
        initialValue = emptyMap<String, List<LegalDocument>>(),
        key1 = allListings
    ) {
        value = repository.getDocsForListings(allListings.map { it.id })
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedIntent by remember { mutableStateOf("ALL") }
    var selectedPropertyType by remember { mutableStateOf("ALL") }

    // Feed ordering + live match counter for the current search query.
    var sort by remember { mutableStateOf(ListingSort.NEWEST) }
    var searchMatchCount by remember { mutableStateOf(0) }
    LaunchedEffect(searchQuery) { searchMatchCount = countMatches(allListings, searchQuery) }

    // Extra quality filters surfaced as quick chips.
    var verifiedOnly by remember { mutableStateOf(false) }
    var hasPhotosOnly by remember { mutableStateOf(false) }
    var minPrice by remember { mutableStateOf<Int?>(null) }

    // Granular filter sheet state (price ceiling, bedrooms, baths, sqft, class)
    var showFilterSheet by remember { mutableStateOf(false) }
    var maxPrice by remember { mutableStateOf<Int?>(null) }
    var minBedrooms by remember { mutableStateOf<Int?>(null) }
    var minBathrooms by remember { mutableStateOf<Int?>(null) }
    var minSqft by remember { mutableStateOf<Int?>(null) }
    var propertyClass by remember { mutableStateOf<String?>(null) }

    // Trust signal: the feed shows EVERY active listing, but each card marks
    // whether the owner's identity is admin-approved yet. Hiding unverified
    // listings entirely made new hosts' properties invisible to buyers.
    val verifiedOwnerIds by produceState(initialValue = emptySet<String>(), key1 = allListings) {
        val verified = mutableSetOf<String>()
        allListings.map { it.ownerId }.distinct().forEach { ownerId ->
            repository.getUserById(ownerId)?.let { user ->
                if (user.isIdentityVerified) verified.add(ownerId)
            }
        }
        value = verified
    }

    fun sortedListings(listings: List<Listing>): List<Listing> = when (sort) {
        ListingSort.NEWEST -> listings.sortedByDescending { it.createdAt }
        ListingSort.PRICE_LOW -> listings.sortedBy { it.priceAmount }
        ListingSort.PRICE_HIGH -> listings.sortedByDescending { it.priceAmount }
        ListingSort.LARGEST -> listings.sortedByDescending { it.sqft }
        ListingSort.MOST_BEDS -> listings.sortedByDescending { it.bedrooms }
    }

    val filteredListings = sortedListings(allListings.filter { listing ->
        // Weighted relevance: title hit beats area hit beats tag/address hit.
        val matchesQuery = searchQuery.isBlank() ||
                listing.title.contains(searchQuery, ignoreCase = true) ||
                listing.publicArea.contains(searchQuery, ignoreCase = true) ||
                listing.exactAddress.contains(searchQuery, ignoreCase = true) ||
                listing.tags.contains(searchQuery, ignoreCase = true)

        val matchesIntent = selectedIntent == "ALL" || listing.intent.equals(selectedIntent, ignoreCase = true)
        val matchesType = selectedPropertyType == "ALL" || listing.propertyType.equals(selectedPropertyType, ignoreCase = true)

        // Granular filters — null means "no ceiling/floor"
        val matchesPrice = maxPrice == null || listing.priceAmount <= maxPrice!!
        val matchesBeds = minBedrooms == null || listing.bedrooms >= minBedrooms!!
        val matchesBaths = minBathrooms == null || listing.bathrooms >= minBathrooms!!
        val matchesSqft = minSqft == null || listing.sqft >= minSqft!!

        // Commercial = OFFICE/SHOP/LAND; everything else is residential
        val listingClass = when (listing.propertyType) {
            "OFFICE", "SHOP", "LAND" -> "COMMERCIAL"
            else -> "RESIDENTIAL"
        }
        val matchesClass = propertyClass == null || listingClass == propertyClass

        // Quick quality filters + price floor (ceilings live in the sheet).
        val matchesVerified = !verifiedOnly || verifiedOwnerIds.contains(listing.ownerId)
        val matchesPhotos = !hasPhotosOnly || listing.galleryUris.isNotBlank() ||
                !listing.coverPhotoUrl.isNullOrBlank()
        val matchesMinPrice = minPrice == null || listing.priceAmount >= minPrice!!

        matchesQuery && matchesIntent && matchesType &&
                matchesPrice && matchesBeds && matchesBaths && matchesSqft && matchesClass &&
                matchesVerified && matchesPhotos && matchesMinPrice
    }
    )

    val activeFilterCount = listOfNotNull(
        maxPrice, minBedrooms, minBathrooms, minSqft, propertyClass, minPrice
    ).size + (if (verifiedOnly) 1 else 0) + (if (hasPhotosOnly) 1 else 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("explore_screen")
    ) {
        // Top header — warm white glass panel with hairline border.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DorjaColors.White,
            border = BorderStroke(width = 1.dp, color = DorjaColors.BentoCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        DorjaLogo(
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = L("explore_header"),
                                style = MaterialTheme.typography.titleMedium,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = L("explore_subtitle"),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray600,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DorjaColors.BentoGreenBg,
                    border = BorderStroke(1.dp, DorjaColors.BentoGreenIcon.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = DorjaColors.BentoGreenIcon,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = L("explore_anti_scam"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.BentoGreenText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
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
            // iOS search field: CupertinoSearchTextField with native magnifier
            // icon, clear (x) button and iOS fill/border styling.
            CupertinoSearchTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_field"),
                cancelButton = null,
                placeholder = { Text(L("explore_search_city"), color = DorjaColors.Gray500) },
                keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
            )

            if (searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Lf("explore_match_count", searchMatchCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray600
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    DorjaChip(
                        selected = selectedIntent == "ALL",
                        label = L("explore_all_listings"),
                        onClick = { selectedIntent = "ALL" },
                        modifier = Modifier.testTag("filter_all")
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedIntent == "RENT",
                        label = L("detail_for_rent"),
                        onClick = { selectedIntent = "RENT" },
                        modifier = Modifier.testTag("filter_rent")
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedIntent == "SALE",
                        label = L("detail_for_sale"),
                        onClick = { selectedIntent = "SALE" },
                        modifier = Modifier.testTag("filter_sale")
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedPropertyType == "APARTMENT",
                        label = L("explore_apartments"),
                        onClick = {
                            selectedPropertyType = if (selectedPropertyType == "APARTMENT") "ALL" else "APARTMENT"
                        }
                    )
                }
                item {
                    DorjaChip(
                        selected = selectedPropertyType == "HOUSE",
                        label = L("explore_houses"),
                        onClick = {
                            selectedPropertyType = if (selectedPropertyType == "HOUSE") "ALL" else "HOUSE"
                        }
                    )
                }
                item {
                    DorjaChip(
                        selected = verifiedOnly,
                        label = L("filter_verified_only"),
                        onClick = { verifiedOnly = !verifiedOnly }
                    )
                }
                item {
                    DorjaChip(
                        selected = hasPhotosOnly,
                        label = L("filter_photos_only"),
                        onClick = { hasPhotosOnly = !hasPhotosOnly }
                    )
                }
                item {
                    DorjaChip(
                        selected = minPrice != null,
                        label = if (minPrice != null) "≥ " + formatPriceShort(minPrice!!, "BDT") else L("filter_min_price"),
                        onClick = {
                            minPrice = when (minPrice) {
                                null -> PRICE_CEILINGS.first() / 2
                                PRICE_CEILINGS.first() / 2 -> PRICE_CEILINGS.first()
                                PRICE_CEILINGS.first() -> PRICE_CEILINGS[1]
                                PRICE_CEILINGS[1] -> PRICE_CEILINGS[2]
                                PRICE_CEILINGS[2] -> PRICE_CEILINGS[3]
                                else -> null
                            }
                        }
                    )
                }
                item {
                    DorjaChip(
                        selected = activeFilterCount > 0,
                        label = if (activeFilterCount > 0) {
                            Lf("filter_active_summary_fmt", activeFilterCount)
                        } else {
                            L("filter_title")
                        },
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.testTag("filter_advanced")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sort row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ListingSort.entries.forEach { option ->
                    DorjaChip(
                        selected = sort == option,
                        label = L(option.key),
                        onClick = { sort = option }
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
                        label = L("explore_properties_available"),
                        icon = Icons.Default.Apartment,
                        iconBg = DorjaColors.BentoBlueBg,
                        iconTint = DorjaColors.BentoBlueIcon,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        value = "100%",
                        label = L("explore_safeview_gated"),
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
                                text = L("explore_empty_title"),
                                style = MaterialTheme.typography.titleMedium,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) Lf("explore_empty_match", searchQuery) else L("explore_empty_none"),
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
                        onClick = { onSelectListing(listing.id) },
                        repository = repository,
                        docsByListing = docsByListing,
                        ownerVerified = verifiedOwnerIds.contains(listing.ownerId)
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        ExploreFilterSheet(
            maxPrice = maxPrice,
            minBedrooms = minBedrooms,
            minBathrooms = minBathrooms,
            minSqft = minSqft,
            propertyClass = propertyClass,
            minPrice = minPrice,
            onDismiss = { showFilterSheet = false },
            onReset = {
                maxPrice = null
                minBedrooms = null
                minBathrooms = null
                minSqft = null
                propertyClass = null
            },
            onApply = { maxP, minBeds, minBaths, minSq, cls, minP ->
                maxPrice = maxP
                minBedrooms = minBeds
                minBathrooms = minBaths
                minSqft = minSq
                propertyClass = cls
                minPrice = minP
                showFilterSheet = false
            }
        )
    }
}

/**
 * Granular multi-parametric filter sheet: price ceiling, bed/bath minimums,
 * minimum area and commercial-vs-residential classification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreFilterSheet(
    maxPrice: Int?,
    minBedrooms: Int?,
    minBathrooms: Int?,
    minSqft: Int?,
    propertyClass: String?,
    minPrice: Int?,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (Int?, Int?, Int?, Int?, String?, Int?) -> Unit
) {
    var draftMinPriceText by remember { mutableStateOf(minPrice?.toString() ?: "") }
    var draftPrice by remember { mutableStateOf<Int?>(maxPrice) }
    var draftBeds by remember { mutableStateOf<Int?>(minBedrooms) }
    var draftBaths by remember { mutableStateOf<Int?>(minBathrooms) }
    var draftSqft by remember { mutableStateOf<Int?>(minSqft) }
    var draftClass by remember { mutableStateOf<String?>(propertyClass) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DorjaColors.DrawerSidebar,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = L("filter_title"),
                style = MaterialTheme.typography.titleLarge,
                color = DorjaColors.DrawerCream,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = L("filter_subtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = DorjaColors.DrawerMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = L("filter_price_ceiling"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.DrawerMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                DorjaChip(
                    selected = draftPrice == null,
                    label = L("filter_any_price"),
                    onClick = { draftPrice = null }
                )
                PRICE_CEILINGS.forEach { value ->
                    DorjaChip(
                        selected = draftPrice == value,
                        label = formatPriceShort(value, "BDT"),
                        onClick = { draftPrice = value }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = L("filter_bedrooms_min"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.DrawerMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DorjaChip(
                    selected = draftBeds == null,
                    label = L("common_any"),
                    onClick = { draftBeds = null }
                )
                listOf(1, 2, 3, 4, 5).forEach { n ->
                    DorjaChip(
                        selected = draftBeds == n,
                        label = "$n+",
                        onClick = { draftBeds = n }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = L("filter_bathrooms_min"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.DrawerMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DorjaChip(
                    selected = draftBaths == null,
                    label = L("common_any"),
                    onClick = { draftBaths = null }
                )
                listOf(1, 2, 3, 4).forEach { n ->
                    DorjaChip(
                        selected = draftBaths == n,
                        label = "$n+",
                        onClick = { draftBaths = n }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = L("filter_sqft_min"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.DrawerMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                DorjaChip(
                    selected = draftSqft == null,
                    label = L("common_any"),
                    onClick = { draftSqft = null }
                )
                listOf(500, 750, 1000, 1500, 2500).forEach { n ->
                    DorjaChip(
                        selected = draftSqft == n,
                        label = "$n+",
                        onClick = { draftSqft = n }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = L("filter_min_price"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.DrawerMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = draftMinPriceText,
                onValueChange = { draftMinPriceText = it.filter { ch -> ch.isDigit() } },
                label = { Text(L("filter_min_price")) },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DorjaColors.DrawerSidebar,
                    unfocusedContainerColor = DorjaColors.DrawerSidebar,
                    focusedBorderColor = DorjaColors.DrawerAccent,
                    unfocusedBorderColor = DorjaColors.DrawerMuted.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = L("filter_class"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.DrawerMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DorjaChip(
                    selected = draftClass == null,
                    label = L("common_any"),
                    onClick = { draftClass = null }
                )
                DorjaChip(
                    selected = draftClass == "RESIDENTIAL",
                    label = L("filter_residential"),
                    onClick = { draftClass = "RESIDENTIAL" }
                )
                DorjaChip(
                    selected = draftClass == "COMMERCIAL",
                    label = L("filter_commercial"),
                    onClick = { draftClass = "COMMERCIAL" }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DorjaButton(
                    text = L("filter_reset"),
                    onClick = {
                        draftPrice = null
                        draftBeds = null
                        draftBaths = null
                        draftSqft = null
                        draftClass = null
                        draftMinPriceText = ""
                        onReset()
                    },
                    modifier = Modifier.weight(1f),
                    // Gray700 inverts to a light gray in dark mode; explicit
                    // dark label keeps the white-text assumption true.
                    containerColor = DorjaColors.Gray700,
                    contentColor = DorjaColors.InverseFg
                )
                DorjaButton(
                    text = L("filter_apply"),
                    onClick = {
                        onApply(
                            draftPrice,
                            draftBeds,
                            draftBaths,
                            draftSqft,
                            draftClass,
                            draftMinPriceText.toIntOrNull()
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreListingCard(
    listing: Listing,
    onClick: () -> Unit,
    repository: DorjaRepository,
    docsByListing: Map<String, List<LegalDocument>>,
    ownerVerified: Boolean
) {
    BentoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("explore_listing_card_${listing.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: intent badge (left) + evidence status (right, compact icon pill)
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
                // Evidence status — compact icon-led pill, never competes with title
                if (repository.hasVerifiedEvidence(docsByListing[listing.id].orEmpty())) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DorjaColors.Teal100
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Evidence verified",
                                tint = DorjaColors.Teal900,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Teal900,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (!ownerVerified) {
                    // Owner identity not yet admin-approved — honest signal on
                    // the card instead of hiding the listing entirely.
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DorjaColors.BentoAmberBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = DorjaColors.BentoAmberText,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = L("explore_owner_unverified"),
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.BentoAmberText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = listing.title,
                style = MaterialTheme.typography.titleMedium,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Public Area
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DorjaColors.Gray500,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = listing.publicArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs strip — evenly divided icon+value tiles, never truncated
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecChip(modifier = Modifier.weight(1f), icon = Icons.Default.Bed, label = "${listing.bedrooms} Beds")
                SpecChip(modifier = Modifier.weight(1f), icon = Icons.Default.Bathtub, label = "${listing.bathrooms} Baths")
                SpecChip(modifier = Modifier.weight(1f), icon = Icons.Default.SquareFoot, label = "${listing.sqft} sqft")
                if (listing.hasScan || !listing.virtualTourUrl.isNullOrBlank()) {
                    SpecChip(
                        modifier = Modifier.weight(1.35f),
                        icon = Icons.Default.ViewInAr,
                        label = "3D Tour",
                        highlighted = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price row — always its own full-width line, no competition
            Text(
                text = Formatters.formatPrice(listing.priceAmount, listing.currency, listing.intent),
                style = MaterialTheme.typography.titleMedium,
                color = DorjaColors.Jol600,
                fontWeight = FontWeight.Bold
            )

            // Tag chips wrap instead of clipping
            if (listing.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Compact icon+label spec tile used in listing cards. */
@Composable
private fun SpecChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (highlighted) DorjaColors.Jol600.copy(alpha = 0.12f) else DorjaColors.Paper50,
        border = BorderStroke(1.dp, if (highlighted) DorjaColors.Jol600.copy(alpha = 0.4f) else DorjaColors.Sand300.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) DorjaColors.Jol600 else DorjaColors.Gray500,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlighted) DorjaColors.Jol600 else DorjaColors.Gray700,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Counts listings matching the free-text query across title, area, address
 * and tags — the number shown live under the search field.
 */
private fun countMatches(listings: List<Listing>, query: String): Int {
    if (query.isBlank()) return 0
    return listings.count { listing ->
        listing.title.contains(query, ignoreCase = true) ||
                listing.publicArea.contains(query, ignoreCase = true) ||
                listing.exactAddress.contains(query, ignoreCase = true) ||
                listing.tags.contains(query, ignoreCase = true)
    }
}
