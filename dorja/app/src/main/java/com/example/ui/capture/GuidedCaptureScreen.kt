package com.example.ui.capture

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.DorjaApp
import com.example.ui.components.DorjaBadge
import com.example.ui.theme.DorjaColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedCaptureScreen(
    listingId: String,
    onBack: () -> Unit,
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    // Permission request (camera) placeholder
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Handle permission result if needed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guided Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Capture photos for listing",
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    // Placeholder: invoke repository to add captured photo
                    // In a real implementation, launch camera and obtain Uri
                    // Here we use a dummy Uri for compilation purposes
                    scope.launch {
                        repository.addCapturedPhoto(listingId, Uri.parse("content://dummy"))
                    }
                }) {
                    Text("Capture Photo")
                }
            }
        }
    )
}
