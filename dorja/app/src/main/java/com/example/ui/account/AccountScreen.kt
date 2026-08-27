package com.example.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoMetricTile
import com.example.ui.components.DorjaAvatar
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaChip
import com.example.ui.components.DorjaOutlinedButton
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    onNavigateToSellerSuite: () -> Unit = {}
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("SELLER") }

    var showResetDialog by remember { mutableStateOf(false) }

    // Edit Profile Modal
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text("Edit Profile & Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Select Active Account Mode", style = MaterialTheme.typography.labelSmall, color = DorjaColors.Gray500)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DorjaChip(
                            selected = editRole == "SELLER",
                            label = "Host / Seller",
                            onClick = { editRole = "SELLER" }
                        )
                        DorjaChip(
                            selected = editRole == "BUYER",
                            label = "Seeker / Buyer",
                            onClick = { editRole = "BUYER" }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("City / Area") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio / Tagline") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DorjaColors.White,
                            unfocusedContainerColor = DorjaColors.White,
                            focusedBorderColor = DorjaColors.BentoBlueIcon,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder
                        )
                    )
                }
            },
            confirmButton = {
                DorjaButton(
                    text = "Save Changes",
                    onClick = {
                        scope.launch {
                            repository.updateUserProfile(
                                displayName = editName,
                                phone = editPhone,
                                email = editEmail,
                                location = editLocation,
                                bio = editBio,
                                role = editRole
                            )
                            showEditProfileDialog = false
                        }
                    },
                    modifier = Modifier.width(140.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Local Storage", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear all local listings, chats, and viewing passes.") },
            confirmButton = {
                DorjaButton(
                    text = "Reset All",
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
                    Text("Cancel", color = DorjaColors.Gray700)
                }
            }
        )
    }

    val user = currentUser ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("account_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.CanvasBg)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = "My Dorja Account",
                style = MaterialTheme.typography.titleLarge,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Identity & Real Estate Credentials • Local Persistence",
                style = MaterialTheme.typography.bodySmall,
                color = DorjaColors.Gray700
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card Bento
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DorjaAvatar(name = user.displayName, size = 56.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DorjaColors.Ink950,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DorjaBadge(
                                        text = if (user.role == "SELLER") "HOST / SELLER" else "SEEKER / BUYER",
                                        backgroundColor = if (user.role == "SELLER") DorjaColors.BentoBlueBg else DorjaColors.BentoGreenBg,
                                        textColor = if (user.role == "SELLER") DorjaColors.BentoBlueText else DorjaColors.BentoGreenText
                                    )
                                    DorjaBadge(
                                        text = "NID VERIFIED",
                                        icon = Icons.Default.VerifiedUser,
                                        backgroundColor = DorjaColors.BentoGreenBg,
                                        textColor = DorjaColors.BentoGreenText
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    editName = user.displayName
                                    editPhone = user.phone
                                    editEmail = user.email
                                    editLocation = user.location
                                    editBio = user.bio
                                    editRole = user.role
                                    showEditProfileDialog = true
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DorjaColors.CanvasBg)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", modifier = Modifier.size(18.dp), tint = DorjaColors.Ink950)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // User Details Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = DorjaColors.Gray500, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(user.phone, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                        }

                        if (user.location.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = DorjaColors.Gray500, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(user.location, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700)
                            }
                        }

                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(user.bio, style = MaterialTheme.typography.bodySmall, color = DorjaColors.Gray700, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                }
            }

            // Quick Role Switch Bento Card
            item {
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val newRole = if (user.role == "SELLER") "BUYER" else "SELLER"
                        scope.launch {
                            repository.updateUserProfile(
                                displayName = user.displayName,
                                phone = user.phone,
                                email = user.email,
                                location = user.location,
                                bio = user.bio,
                                role = newRole
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DorjaColors.BentoPurpleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = DorjaColors.BentoPurpleIcon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (user.role == "SELLER") "Switch to Buyer Mode" else "Switch to Host Mode",
                                style = MaterialTheme.typography.titleSmall,
                                color = DorjaColors.Ink950,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (user.role == "SELLER") "Explore & visit listings as a property seeker" else "Manage your properties and review visitor passes as a host",
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = DorjaColors.Gray500
                        )
                    }
                }
            }

            // Trust & Verification Credentials Bento Card
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SECURITY CREDENTIALS",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SecurityRow(title = "NID Verification", status = "PASSED", icon = Icons.Default.Shield)
                        SecurityRow(title = "SafeView GPS Token Registry", status = "ACTIVE", icon = Icons.Default.Lock)
                        SecurityRow(title = "Local Room Persistence", status = "ON-DEVICE", icon = Icons.Default.CheckCircle)
                    }
                }
            }

            // Data Management Bento Card
            item {
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DATABASE MANAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DorjaOutlinedButton(
                            text = "Clear Local Data & Re-initialize",
                            onClick = { showResetDialog = true },
                            icon = Icons.Default.DeleteSweep,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityRow(title: String, status: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = DorjaColors.BentoGreenIcon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = DorjaColors.Ink950)
        }
        DorjaBadge(text = status, backgroundColor = DorjaColors.BentoGreenBg, textColor = DorjaColors.BentoGreenText)
    }
}
