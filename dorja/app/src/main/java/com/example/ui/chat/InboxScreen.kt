package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.DorjaApp
import com.example.ui.components.DorjaAvatar
import com.example.ui.components.DorjaBadge
import com.example.ui.components.DorjaCard
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource

@Composable
fun InboxScreen(
    onOpenConversation: (String) -> Unit
) {
    val repository = DorjaApp.instance.repository
    val currentUser by repository.currentUser.collectAsState()
    val userId = currentUser?.id ?: "u1"
    val conversations by repository.getConversationsForUser(userId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Paper50)
            .testTag("inbox_screen")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.White)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dorja_logo),
                        contentDescription = "Dorja Logo",
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Encrypted Inbox",
                            style = MaterialTheme.typography.titleLarge,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zero-leak real estate messaging channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = DorjaColors.Jol600,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SECURED",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Jol600,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = DorjaColors.Sand300,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No inquiries yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Start a chat directly from any property listing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DorjaColors.Gray700
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(conversations, key = { it.id }) { conv ->
                    // Resolve other party from DB
                    val otherId = if (userId == conv.hostUserId) conv.seekerUserId else conv.hostUserId
                    var otherName by remember(conv.id) { mutableStateOf("") }
                    var otherRole by remember(conv.id) { mutableStateOf("") }
                    LaunchedEffect(conv.id) {
                        withContext(Dispatchers.IO) {
                            val otherUser = DorjaApp.instance.repository.getUserById(otherId)
                            withContext(Dispatchers.Main) {
                                otherName = otherUser?.displayName?.ifBlank { otherUser.username } ?: otherId
                                otherRole = otherUser?.role ?: ""
                            }
                        }
                    }

                    DorjaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation_item_${conv.id}"),
                        onClick = { onOpenConversation(conv.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DorjaAvatar(name = otherName.ifBlank { otherId }, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = otherName.ifBlank { otherId },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DorjaColors.Ink950,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = Formatters.formatTimeOnly(conv.lastMessageAt ?: conv.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Gray500
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    DorjaBadge(
                                        text = otherRole.ifBlank { "USER" },
                                        backgroundColor = DorjaColors.Sand100,
                                        textColor = DorjaColors.Ink950
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Listing #${conv.listingId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Gray500,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = conv.lastMessageText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DorjaColors.Gray500
                            )
                        }
                    }
                }
            }
        }
    }
}
