package com.example.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.country.CountryRegistry
import com.example.ui.i18n.DorjaLocales
import com.example.ui.i18n.L
import com.example.ui.i18n.LocaleSettings
import com.example.ui.theme.DorjaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySettingsSheet(
    sheetState: androidx.compose.material3.SheetState,
    selectedIso2: String,
    onDismiss: () -> Unit,
    onCountrySelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val profiles = remember { CountryRegistry.profiles.sortedBy { it.displayName } }
    val filtered = profiles.filter {
        searchQuery.isBlank() ||
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.iso2.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DorjaColors.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = L("settings_select_country"),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DorjaColors.Ink950
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = L("settings_country_subtitle"),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = DorjaColors.Gray700
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(L("common_search")) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = DorjaColors.Gray500)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DorjaColors.White,
                    unfocusedContainerColor = DorjaColors.White,
                    focusedBorderColor = DorjaColors.BentoBlueIcon,
                    unfocusedBorderColor = DorjaColors.BentoCardBorder
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = DorjaColors.BentoCardBorder)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filtered, key = { it.iso2 }) { profile ->
                    val isSelected = profile.iso2.equals(selectedIso2, ignoreCase = true)
                    val languageTag = LocaleSettings.languageTagForCountry(profile.iso2)
                    val languageName = DorjaLocales.byTag(languageTag)?.nativeName ?: languageTag
                    val identity = CountryRegistry.identityCredential(profile.iso2)
                    CountrySettingsRow(
                        displayName = profile.displayName,
                        iso2 = profile.iso2,
                        subtitle = "${profile.currencyCode} (${profile.currencySymbol})  •  ${identity.shortName}  •  $languageName",
                        isSelected = isSelected,
                        onClick = { onCountrySelected(profile.iso2) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CountrySettingsRow(
    displayName: String,
    iso2: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) DorjaColors.BentoBlueBg else DorjaColors.White,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) DorjaColors.BentoBlueText else DorjaColors.Ink950
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = iso2,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DorjaColors.Gray500
                    )
                }
                Text(
                    text = subtitle,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Gray500
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = L("common_done"),
                    tint = DorjaColors.BentoBlueIcon,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
