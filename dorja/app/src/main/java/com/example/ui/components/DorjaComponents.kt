package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.country.CountryRegistry
import com.example.data.model.Listing
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.LiquidGlassDefaults
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.pressScale
import com.example.ui.util.Formatters

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0x0C000000),
    borderWidth: Dp = 0.5.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalBorder = border ?: BorderStroke(borderWidth, borderColor)
    val clickableModifier = if (onClick != null) {
        Modifier.pressScale(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(clickableModifier),
        shape = shape,
        color = backgroundColor,
        border = finalBorder,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun DorjaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0x0C000000),
    borderWidth: Dp = 0.5.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BentoCard(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        borderWidth = borderWidth,
        border = border,
        content = content
    )
}

@Composable
fun BentoMetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    subtext: String? = null,
    icon: ImageVector? = null,
    iconBg: Color = DorjaColors.Jol100,
    iconTint: Color = DorjaColors.Jol600,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.pressScale(onClick = onClick)
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(clickableModifier),
        shape = RoundedCornerShape(14.dp),
        color = DorjaColors.Paper50,
        border = BorderStroke(0.5.dp, Color(0x0C000000))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray600,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (!subtext.isNullOrBlank()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray600,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun BentoHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    BentoCard(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        backgroundColor = backgroundColor,
        content = content
    )
}

@Composable
fun DorjaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = DorjaColors.Jol600,
    contentColor: Color = Color.White,
    testTag: String? = null
) {
    val finalModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = finalModifier
            .height(50.dp)
            .pressScale(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StainedLiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    stainedColor: Color = DorjaColors.Jol600,
    testTag: String? = null
) {
    val finalModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    Box(
        modifier = finalModifier
            .height(52.dp)
            .liquidGlass(
                blurRadius = LiquidGlassDefaults.BlurMedium,
                glassColor = stainedColor.copy(alpha = 0.85f),
                specularColor = Color(0x60FFFFFF),
                shape = RoundedCornerShape(26.dp)
            )
            .pressScale(onClick = { if (enabled) onClick() }),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DorjaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    borderColor: Color = DorjaColors.Jol600,
    contentColor: Color = DorjaColors.Jol600,
    testTag: String? = null
) {
    val finalModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = finalModifier
            .height(48.dp)
            .pressScale(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DorjaBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DorjaColors.Jol100,
    textColor: Color = DorjaColors.Jol700,
    contentColor: Color = textColor,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun DorjaChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val animatedBg by animateColorAsState(
        targetValue = if (selected) DorjaColors.Jol600 else Color.White,
        label = "chipBg"
    )
    val animatedFg by animateColorAsState(
        targetValue = if (selected) Color.White else DorjaColors.Ink950,
        label = "chipFg"
    )

    Surface(
        modifier = modifier.pressScale(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = animatedBg,
        border = BorderStroke(
            0.5.dp,
            if (selected) Color.Transparent else Color(0x1A000000)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedFg,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = animatedFg,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun DorjaAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    backgroundColor: Color = DorjaColors.Jol600,
    textColor: Color = Color.White
) {
    val initials = name.trim().split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "D" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp
        )
    }
}

@Composable
fun PulseDot(
    modifier: Modifier = Modifier,
    color: Color = DorjaColors.Success,
    size: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun SafeAddressShield(
    publicArea: String,
    modifier: Modifier = Modifier,
    isVerified: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DorjaColors.Paper50,
        border = BorderStroke(0.5.dp, Color(0x0C000000))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isVerified) DorjaColors.BentoGreenBg else DorjaColors.BentoAmberBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isVerified) DorjaColors.BentoGreenIcon else DorjaColors.BentoAmberIcon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SAFEADDRESS™ PRIVACY SHIELD",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isVerified) DorjaColors.BentoGreenText else DorjaColors.BentoAmberText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = publicArea,
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun DorjaInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    testTag: String? = null
) {
    val baseModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        leadingIcon = if (leadingIcon != null) {
            { Icon(leadingIcon, contentDescription = null, tint = DorjaColors.Jol600) }
        } else null,
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = baseModifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = DorjaColors.Jol600,
            unfocusedBorderColor = Color(0x1A000000)
        )
    )
}

@Composable
fun LiquidGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Search location, property, ID..."
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .liquidGlass(
                blurRadius = LiquidGlassDefaults.BlurMedium,
                glassColor = Color(0xECFAFBFD),
                specularColor = Color(0x50FFFFFF),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = DorjaColors.Jol600,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Gray600,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = DorjaColors.Ink950,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = DorjaColors.Gray600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DorjaColors.Jol600.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ListingCard(
    listing: Listing,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val country = CountryRegistry.profile(listing.countryIso2)

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onSelect(listing.id) },
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White
    ) {
        Column {
            // Image Header with Glass Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DorjaColors.Sand300)
            ) {
                if (listing.photos.isNotEmpty()) {
                    AsyncImage(
                        model = listing.photos.first(),
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = DorjaColors.Gray600,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Top-End 3D VR Pill Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .liquidGlass(
                            blurRadius = LiquidGlassDefaults.BlurSmall,
                            glassColor = Color(0xCC1A1C1E),
                            specularColor = Color(0x40FFFFFF),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "3D VR",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3D TOUR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                // Bottom-Start Price Pill Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .liquidGlass(
                            blurRadius = LiquidGlassDefaults.BlurSmall,
                            glassColor = Color(0xDD1A1C1E),
                            specularColor = Color(0x40FFFFFF),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = Formatters.formatPrice(listing.priceAmount, listing.currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Listing Info Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                DorjaBadge(
                    text = "${country.displayName} ${country.currencySymbol}",
                    backgroundColor = DorjaColors.Jol100,
                    contentColor = DorjaColors.Jol700
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DorjaColors.Gray600,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = listing.publicArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray600,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specs Dot-Separated Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${listing.bedrooms} Beds",
                    style = MaterialTheme.typography.labelMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                Text(text = " • ", color = DorjaColors.Gray600)
                Text(
                    text = "${listing.bathrooms} Baths",
                    style = MaterialTheme.typography.labelMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                Text(text = " • ", color = DorjaColors.Gray600)
                Text(
                    text = "${listing.areaSqm} m²",
                    style = MaterialTheme.typography.labelMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
                if (listing.intent.isNotEmpty()) {
                    Text(text = " • ", color = DorjaColors.Gray600)
                    Text(
                        text = listing.intent.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Jol600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPicker(
    selectedIso2: String = "BD",
    selected: String = selectedIso2,
    selectedCountry: String = selected,
    onCountrySelected: (String) -> Unit = {},
    onSelect: (String) -> Unit = onCountrySelected,
    modifier: Modifier = Modifier,
    label: String = "Select Country"
) {
    val activeIso2 = if (selected.isNotBlank() && selected != "BD") selected
    else if (selectedCountry.isNotBlank() && selectedCountry != "BD") selectedCountry
    else selectedIso2

    val activeOnSelect = { code: String ->
        onSelect(code)
        onCountrySelected(code)
    }

    var showSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedProfile = CountryRegistry.profile(activeIso2)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(onClick = { showSheet = true }),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(0.5.dp, Color(0x1A000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray600,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedProfile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DorjaBadge(
                        text = selectedProfile.currencySymbol,
                        backgroundColor = DorjaColors.Jol100,
                        contentColor = DorjaColors.Jol700
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Change country",
                tint = DorjaColors.Gray600
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Grab handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Country",
                    style = MaterialTheme.typography.titleLarge,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose your country to load specific property evidence, legal disclosures, and authority registries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray600
                )
                Spacer(modifier = Modifier.height(16.dp))

                LiquidGlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFilterClick = {},
                    placeholderText = "Search by country name or code..."
                )
                Spacer(modifier = Modifier.height(16.dp))

                val allProfiles = remember { CountryRegistry.profiles }
                val filteredProfiles = allProfiles.filter {
                    searchQuery.isBlank() ||
                            it.displayName.contains(searchQuery, ignoreCase = true) ||
                            it.iso2.contains(searchQuery, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProfiles, key = { it.iso2 }) { profile ->
                        val isSelected = profile.iso2 == activeIso2
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale(onClick = {
                                    activeOnSelect(profile.iso2)
                                    showSheet = false
                                }),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) DorjaColors.Jol100 else DorjaColors.Paper50,
                            border = BorderStroke(
                                0.5.dp,
                                if (isSelected) DorjaColors.Jol600.copy(alpha = 0.3f) else Color(0x0C000000)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) DorjaColors.Jol700 else DorjaColors.Ink950
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = profile.iso2,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DorjaColors.Gray600,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Currency: ${profile.currencyCode} (${profile.currencySymbol}) • Stage ${profile.launchStage}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Gray600,
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = DorjaColors.Jol600,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
