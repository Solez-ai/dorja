package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.country.CountryRegistry
import com.example.data.model.Listing
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = DorjaColors.BentoCardBg,
    borderColor: Color = DorjaColors.BentoCardBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = shape,
            border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = onClick
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            content()
        }
    }
}

@Composable
fun BentoMetricTile(
    value: String,
    label: String = "",
    title: String = "",
    subtitle: String = "",
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = DorjaColors.White,
    iconBg: Color = DorjaColors.BentoBlueBg,
    iconTint: Color = DorjaColors.BentoBlueIcon,
    iconContainerColor: Color = iconBg,
    iconColor: Color = iconTint,
    textColor: Color = DorjaColors.Ink950,
    labelColor: Color = DorjaColors.Gray500,
    onClick: (() -> Unit)? = null
) {
    val displayLabel = if (label.isNotBlank()) label else if (subtitle.isNotBlank() && title.isNotBlank()) "$subtitle • $title" else if (title.isNotBlank()) title else subtitle

    BentoCard(
        modifier = modifier,
        backgroundColor = containerColor,
        borderWidth = 1.dp,
        borderColor = DorjaColors.BentoCardBorder,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BentoHeroCard(
    title: String,
    headline: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    progressFraction: Float? = null,
    progressLabel: String? = null,
    containerColor: Color = DorjaColors.BentoBlueBg,
    textColor: Color = DorjaColors.BentoBlueText,
    primaryColor: Color = DorjaColors.BentoBlueIcon,
    onClick: (() -> Unit)? = null
) {
    BentoCard(
        modifier = modifier,
        backgroundColor = containerColor,
        borderWidth = 0.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = DorjaColors.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }

                if (progressFraction != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(textColor.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(primaryColor)
                            )
                        }
                        if (progressLabel != null) {
                            Text(
                                text = progressLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = DorjaColors.White,
    borderColor: Color = DorjaColors.BentoCardBorder,
    iconBgColor: Color = DorjaColors.Sand100,
    iconColor: Color = DorjaColors.Ink950
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = containerColor,
        borderColor = borderColor,
        borderWidth = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
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
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )
            }
        }
    }
}

@Composable
fun DorjaCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = BorderStroke(1.dp, DorjaColors.BentoCardBorder),
    backgroundColor: Color = DorjaColors.BentoCardBg,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = shape,
            border = border,
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = onClick
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            border = border,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            content()
        }
    }
}

@Composable
fun DorjaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "dorja_button",
    containerColor: Color = DorjaColors.Jol600,
    contentColor: Color = DorjaColors.White
) {
    Button(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = DorjaColors.Sand300,
            disabledContentColor = DorjaColors.Gray500
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor
            )
        }
    }
}

@Composable
fun DorjaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "dorja_outlined_button",
    borderColor: Color = DorjaColors.Jol600,
    textColor: Color = DorjaColors.Jol600
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, if (enabled) borderColor else DorjaColors.Sand300),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = textColor,
            disabledContentColor = DorjaColors.Gray500
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
        }
    }
}

@Composable
fun DorjaBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DorjaColors.Teal100,
    textColor: Color = DorjaColors.Teal900,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DorjaInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isSingleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    testTag: String = "dorja_input"
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            label = { Text(label) },
            placeholder = { Text(placeholder, color = DorjaColors.Gray500) },
            leadingIcon = if (leadingIcon != null) {
                { Icon(leadingIcon, contentDescription = null, tint = DorjaColors.Gray700) }
            } else null,
            trailingIcon = trailingIcon,
            singleLine = isSingleLine,
            isError = isError,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DorjaColors.White,
                unfocusedContainerColor = DorjaColors.Paper50,
                focusedBorderColor = DorjaColors.Jol600,
                unfocusedBorderColor = DorjaColors.Sand300,
                focusedLabelColor = DorjaColors.Jol600,
                unfocusedLabelColor = DorjaColors.Gray700,
                cursorColor = DorjaColors.Jol600
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = DorjaColors.Error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun DorjaChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) DorjaColors.Ink950 else DorjaColors.White,
        border = BorderStroke(1.dp, if (selected) DorjaColors.Ink950 else DorjaColors.Sand300)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) DorjaColors.White else DorjaColors.Gray700,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) DorjaColors.White else DorjaColors.Gray700,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
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
    textColor: Color = DorjaColors.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = Formatters.getInitials(name),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
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
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun SafeAddressShield(
    approximateArea: String,
    modifier: Modifier = Modifier
) {
    DorjaCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = DorjaColors.Sand100,
        border = BorderStroke(1.dp, DorjaColors.Sand300)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DorjaColors.Teal100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Scam Protected Address",
                        style = MaterialTheme.typography.titleSmall,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    DorjaBadge(text = "HIDDEN", backgroundColor = DorjaColors.Teal100, textColor = DorjaColors.Teal900)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Area: $approximateArea. Exact door number revealed upon confirmed in-person visit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700
                )
            }
        }
    }
}

@Composable
fun PropertyCard(
    listing: Listing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DorjaCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("property_card_${listing.id}"),
        onClick = onClick
    ) {
        Column {
            // Stylized Blueprint/Architectural Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(DorjaColors.Ink950),
                contentAlignment = Alignment.Center
            ) {
                // Architectural floorplan grid sketch
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = DorjaColors.Jol600,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REALITY PASSPORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Sand300,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Badges top row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DorjaBadge(
                        text = listing.intent,
                        backgroundColor = if (listing.intent == "RENT") DorjaColors.Teal100 else DorjaColors.Sand100,
                        textColor = if (listing.intent == "RENT") DorjaColors.Teal900 else DorjaColors.Ink950
                    )

                    if (listing.hasScan) {
                        DorjaBadge(
                            text = "3D TOUR",
                            icon = Icons.Default.ViewInAr,
                            backgroundColor = DorjaColors.Jol600,
                            textColor = DorjaColors.White
                        )
                    }
                }
            }

            // Details
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = DorjaColors.Ink950,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.publicArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Price and Specs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Formatters.formatPriceShort(listing.priceAmount, listing.currency),
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Jol600,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${listing.bedrooms} Bed • ${listing.bathrooms} Bath",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray500
                    )
                }
            }
        }
    }
}

/**
 * Country-of-transaction picker built on the [CountryRegistry].
 * Only launchable markets (atlas stage <= 1) are selectable; every other
 * profile is shown with its confidence label and disabled.
 */
@Composable
fun CountryPicker(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val current = CountryRegistry.profile(selected)

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = DorjaColors.White,
            border = BorderStroke(1.dp, DorjaColors.BentoCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Country of Transaction",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray500
                    )
                    Text(
                        text = current.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = DorjaColors.Gray500
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CountryRegistry.profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (profile.selectable) profile.displayName
                            else "${profile.displayName} — ${profile.confidenceLabel}"
                        )
                    },
                    onClick = {
                        if (profile.selectable) {
                            onSelect(profile.iso2)
                            expanded = false
                        }
                    },
                    enabled = profile.selectable,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = if (profile.selectable) DorjaColors.Jol600 else DorjaColors.Gray500
                        )
                    }
                )
            }
        }
    }
}
