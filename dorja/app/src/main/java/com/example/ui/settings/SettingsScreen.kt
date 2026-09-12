package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import android.Manifest
import com.example.ai.AiEngineState
import com.example.ai.DorjaAiEngine
import androidx.core.content.ContextCompat
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

    val aiEngine = remember { DorjaAiEngine.getInstance(context) }
    val engineState by aiEngine.engineState.collectAsState()
    var showModelDownloadDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = aiEngine.importModelFromUri(uri)
                if (res.isSuccess) {
                    Toast.makeText(context, "Model successfully loaded with hardware acceleration!", Toast.LENGTH_LONG).show()
                    showModelDownloadDialog = false
                } else {
                    Toast.makeText(context, "Failed to load model: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showModelDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showModelDownloadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = DorjaColors.Jol600,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Load Up Dorja AI Model", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "The model file was not detected in your Downloads folder.\n\nYou can download the LiteRT model file from our link, or manually pick the file if you've already downloaded it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Gray700
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DorjaColors.Sand100.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, DorjaColors.BentoCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "Target Acceleration:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DorjaColors.Ink950
                            )
                            Text(
                                "• Neural Processing Units (NPUs via NNAPI)\n• Mobile GPU Acceleration (Vulkan/OpenCL)\n• Multi-Threaded CPU Engine (XNNPACK)",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray700,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Download Model",
                    icon = Icons.Default.OpenInNew,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DorjaAiEngine.MODEL_DOWNLOAD_URL))
                        context.startActivity(intent)
                    }
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showModelDownloadDialog = false }) {
                        Text("Cancel", color = DorjaColors.Gray700)
                    }
                    DorjaOutlinedButton(
                        text = "Pick File Manually",
                        icon = Icons.Default.FolderOpen,
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
            }
        )
    }

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ON-DEVICE AI ENGINE (LITERT)",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray500,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (engineState is AiEngineState.Ready) Color(0x1F00875A) else Color(0x1F888888),
                                border = BorderStroke(0.8.dp, if (engineState is AiEngineState.Ready) Color(0xFF00875A) else Color.Gray.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (engineState is AiEngineState.Ready) "NPU READY" else "NOT LOADED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (engineState is AiEngineState.Ready) Color(0xFF00875A) else DorjaColors.Gray700,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (engineState is AiEngineState.Ready) Icons.Default.CheckCircle else Icons.Default.Memory,
                                contentDescription = null,
                                tint = if (engineState is AiEngineState.Ready) Color(0xFF00875A) else DorjaColors.Jol600,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dorja Neural Assistant",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                when (val state = engineState) {
                                    is AiEngineState.Ready -> {
                                        Text(
                                            text = "${state.modelFileName} • ${(state.fileSizeBytes / (1024 * 1024))} MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DorjaColors.Gray700,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Accelerator: ${state.accelerator}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DorjaColors.Jol600,
                                            fontSize = 11.sp
                                        )
                                    }
                                    is AiEngineState.Loading -> {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DorjaColors.Jol600
                                        )
                                    }
                                    is AiEngineState.Searching -> {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DorjaColors.Gray700
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "Model is not loaded automatically on startup. Tap 'Load Up Model' to initialize NPU/GPU acceleration.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DorjaColors.Gray700
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (engineState) {
                            is AiEngineState.Ready -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
            // Model file picker and load button
            var modelUri by remember { mutableStateOf<android.net.Uri?>(null) }
            var isLoading by remember { mutableStateOf(false) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
                modelUri = uri
                if (uri != null) {
                    isLoading = true
                    // Load model asynchronously
                    LaunchedEffect(uri) {
                        try {
                            DorjaApp.instance.aiEngine.loadModel(uri)
                        } catch (e: Exception) {
                            // Show error via Snackbar (placeholder)
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    // Permission request for external storage
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled on button click
    }
    val hasStoragePermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }
    DorjaOutlinedButton(text = "Select Model File", onClick = {
        if (hasStoragePermission.value) {
            launcher.launch("*/*")
        } else {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    })
    if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
    DorjaOutlinedButton(text = "Download Model", onClick = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DorjaAiEngine.MODEL_DOWNLOAD_URL))
        context.startActivity(intent)
    })
}

                                            scope.launch {
                                                val modelFile = aiEngine.findModelInDownloads()
                                                if (modelFile != null) {
                                                    aiEngine.loadModelFromFile(modelFile)
                                                    Toast.makeText(context, "Model reloaded from Downloads!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    showModelDownloadDialog = true
                                                }
                                            }
                                        },
                                        icon = Icons.Default.Refresh,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DorjaOutlinedButton(
                                        text = "Unload",
                                        onClick = {
                                            aiEngine.unloadModel()
                                            Toast.makeText(context, "Model unloaded", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                            }
                            is AiEngineState.Loading -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = DorjaColors.Jol600
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Loading model into LiteRT...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Gray700
                                    )
                                }
                            }
                            else -> {
                                DorjaButton(
                                    text = "Load Up Model",
                                    onClick = {
                                        scope.launch {
                                            val modelFile = aiEngine.findModelInDownloads()
                                            if (modelFile != null) {
                                                val res = aiEngine.loadModelFromFile(modelFile)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Model loaded from Downloads with NPU/GPU acceleration!", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Failed to initialize: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                showModelDownloadDialog = true
                                            }
                                        }
                                    },
                                    icon = Icons.Default.Download,
                                    modifier = Modifier.fillMaxWidth()
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
