package com.example.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.DorjaApp
import com.example.data.country.CountryRegistry
import com.example.ui.components.BentoCard
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaLogo
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.i18n.DorjaLocales
import com.example.ui.i18n.L
import com.example.ui.i18n.LocaleSettings
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.ThemeSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val darkMode by ThemeSettings.darkMode.collectAsState()
    val storedCountry by LocaleSettings.countryCode.collectAsState()
    val countryCode = currentUser?.countryCode ?: storedCountry
    val profile = CountryRegistry.profile(countryCode)
    val languageTag = LocaleSettings.languageTagForCountry(countryCode)
    val languageName = DorjaLocales.byTag(languageTag)?.nativeName ?: languageTag
    val identity = CountryRegistry.identityCredential(countryCode)

    var showCountrySheet by remember { mutableStateOf(false) }
    val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(L("account_reset_title"), fontWeight = FontWeight.Bold) },
            text = { Text(L("account_reset_body")) },
            confirmButton = {
                DorjaButton(
                    text = L("account_reset_all"),
                    onClick = {
                        scope.launch {
                            repository.resetAllData()
                            showResetDialog = false
                        }
                    },
                    modifier = Modifier.width(120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(L("common_cancel"), color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showCountrySheet) {
        CountrySettingsSheet(
            sheetState = countrySheetState,
            selectedIso2 = countryCode,
            onDismiss = { showCountrySheet = false },
            onCountrySelected = { iso ->
                repository.setUserCountryCode(iso)
                LocaleSettings.applyCountry(context, iso)
                showCountrySheet = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            DorjaLogo(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L("settings_title"),
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = L("settings_subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray700,
                    maxLines = 1
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("settings_appearance"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = DorjaColors.Jol600,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = L("settings_dark_mode"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = L("settings_dark_mode_sub"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray700
                                )
                            }
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { ThemeSettings.setDarkMode(context, it) },
                                modifier = Modifier.testTag("settings_dark_mode_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                    checkedTrackColor = DorjaColors.Jol600,
                                    uncheckedThumbColor = DorjaColors.Gray500,
                                    uncheckedTrackColor = DorjaColors.Sand100
                                )
                            )
                        }
                    }
                }
            }

            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showCountrySheet = true }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("settings_country_section"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = DorjaColors.Jol600,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${profile.currencyCode} (${profile.currencySymbol})  •  ${identity.shortName}  •  $languageName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray700
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = L("settings_country_subtitle"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DorjaColors.Gray500
                                )
                            }
                        }
                    }
                }
            }

            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = L("account_db_management"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DorjaOutlinedButton(
                            text = L("account_clear_data"),
                            onClick = { showResetDialog = true },
                            icon = Icons.Default.DeleteSweep,
                            modifier = Modifier.fillMaxWidth().testTag("settings_clear_data")
                        )
                    }
                }
            }
        }
    }
}
