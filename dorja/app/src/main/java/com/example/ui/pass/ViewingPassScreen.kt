package com.example.ui.pass

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.Viewing
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaCard
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import com.example.ui.util.QrCodeGenerator
import kotlinx.coroutines.launch

@Composable
fun ViewingPassScreen(
    viewingId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    var viewing by remember { mutableStateOf<Viewing?>(null) }
    var qrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(viewingId) {
        val loaded = repository.getViewingById(viewingId)
        viewing = loaded
        if (loaded != null) {
            qrBitmap = QrCodeGenerator.generateQrImageBitmap(loaded.passToken, 512)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Paper50)
            .testTag("viewing_pass_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.White)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.Paper50)
                        .testTag("pass_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DorjaColors.Ink950
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SafeView Viewing Pass",
                        style = MaterialTheme.typography.titleLarge,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Encrypted On-Site Verification Token",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (viewing == null) {
                Text("Pass not found", color = DorjaColors.Ink950)
            } else {
                val pass = viewing!!

                // Main Pass Card
                DorjaCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DorjaColors.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, DorjaColors.Jol600)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge Top Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DorjaBadge(
                                text = "DORJA SAFEVIEW PASS",
                                icon = Icons.Default.Shield,
                                backgroundColor = DorjaColors.Teal100,
                                textColor = DorjaColors.Teal900
                            )
                            DorjaBadge(
                                text = pass.status,
                                backgroundColor = if (pass.status == "CHECKED_IN") DorjaColors.Teal100 else DorjaColors.Sand100,
                                textColor = if (pass.status == "CHECKED_IN") DorjaColors.Teal900 else DorjaColors.Ink950
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // QR Code Rendered
                        if (qrBitmap != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, DorjaColors.Ink950),
                                color = DorjaColors.White,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Image(
                                    bitmap = qrBitmap!!,
                                    contentDescription = "SafeView QR Code",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(12.dp)
                                        .testTag("pass_qr_code_image")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Token Code Monospace
                        Text(
                            text = pass.passToken,
                            style = MaterialTheme.typography.titleLarge,
                            color = DorjaColors.Ink950,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DorjaColors.Sand300)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Unlocked Location Banner
                        DorjaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = DorjaColors.Sand100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = DorjaColors.Jol600,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Unlocked Exact Location",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DorjaColors.Ink950,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "House 42, Road 7, Block C, Mirpur 11, Dhaka 1216",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DorjaColors.Gray700
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Details Summary
                        PassDetailRow(label = "Time Slot", value = "${Formatters.formatTimeOnly(pass.startsAt)} - ${Formatters.formatTimeOnly(pass.endsAt)}")
                        PassDetailRow(label = "Date", value = Formatters.formatDateOnly(pass.startsAt))
                        PassDetailRow(label = "GPS Geofence", value = "Active & Verified (Mirpur 11)")

                        if (pass.status != "CHECKED_IN") {
                            Spacer(modifier = Modifier.height(20.dp))
                            DorjaButton(
                                text = "Host Check-In & Validate Pass",
                                onClick = {
                                    scope.launch {
                                        repository.checkInViewing(pass.id)
                                        viewing = repository.getViewingById(pass.id)
                                    }
                                },
                                icon = Icons.Default.CheckCircle,
                                testTag = "validate_pass_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = DorjaColors.Gray500
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = DorjaColors.Ink950,
            fontWeight = FontWeight.SemiBold
        )
    }
}
