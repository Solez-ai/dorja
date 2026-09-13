package com.example.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.DorjaApp
import com.example.ui.theme.DorjaColors
import java.io.File
import kotlinx.coroutines.launch

/**
 * Guided Optical Capture — anti-distortion wide-angle photo sequencing for
 * structural verification. Photos are captured with the device camera and
 * persisted onto the listing's gallery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedCaptureScreen(
    listingId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()

    val listing by repository.observeListingById(listingId).collectAsState(initial = null)
    val galleryUris = remember(listing?.galleryUris) {
        listing?.galleryUris?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingPhotoFile
        if (success && file != null) {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )
            scope.launch {
                repository.addCapturedPhoto(listingId, uri.toString())
            }
        } else {
            // Discard the empty temp file if capture was cancelled
            file?.takeIf { it.length() == 0L }?.delete()
        }
        pendingPhotoFile = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            pendingPhotoFile?.let { file ->
                cameraLauncher.launch(
                    FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                )
            }
        }
    }

    fun launchCamera() {
        val photoFile = File(context.filesDir, "captures").apply { mkdirs() }
            .let { dir -> File(dir, "capture_${System.currentTimeMillis()}.jpg") }
        pendingPhotoFile = photoFile
        if (hasCameraPermission) {
            cameraLauncher.launch(
                FileProvider.getUriForFile(context, context.packageName + ".fileprovider", photoFile)
            )
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = DorjaColors.Paper50,
        topBar = {
            TopAppBar(
                title = { Text("Guided Optical Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "${galleryUris.size} captured",
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray500,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Guidance card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF00BCD4).copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Capture each room corner-to-corner from chest height. " +
                            "Avoid glass reflections and backlit windows — anti-distortion " +
                            "sequencing builds the structural verification record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Guided checklist
            val checklist = listOf(
                "Standing view of every room from the doorway",
                "Wide-angle shot of the full living area",
                "Kitchen and bathroom fixtures close-up",
                "Windows and natural light sources"
            )
            checklist.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (index < galleryUris.size) Icons.Default.CheckCircle
                        else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = if (index < galleryUris.size) DorjaColors.Jol600 else DorjaColors.Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DorjaColors.Ink950
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capture CTA
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = DorjaColors.Ink950,
                onClick = { launchCamera() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = DorjaColors.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Capture Photo",
                        style = MaterialTheme.typography.titleSmall,
                        color = DorjaColors.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gallery
            Text(
                text = "CAPTURED PHOTOS",
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (galleryUris.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DorjaColors.White
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = DorjaColors.Gray500,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No photos captured yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DorjaColors.Gray700
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(galleryUris, key = { it }) { uriString ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.aspectRatio(1f)
                        ) {
                            Box {
                                AsyncImage(
                                    model = uriString,
                                    contentDescription = "Captured photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(DorjaColors.Jol600),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.White
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
