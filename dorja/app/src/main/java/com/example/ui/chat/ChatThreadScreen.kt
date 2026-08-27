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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.model.Message
import com.example.ui.components.DorjaAvatar
import com.example.ui.components.DorjaBadge
import com.example.ui.theme.DorjaColors
import com.example.ui.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun ChatThreadScreen(
    conversationId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val userId = currentUser?.id ?: "u1"

    val messages by repository.getMessagesByConversation(conversationId).collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }

    val otherPartyName = if (userId == "u1") "Karim Hassan" else "Rahim Ahmed"
    val otherPartyPhone = if (userId == "u1") "+880 1812-***678" else "+880 1712-***678"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.Paper50)
            .testTag("chat_thread_screen")
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DorjaColors.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.Paper50)
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DorjaColors.Ink950
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                DorjaAvatar(name = otherPartyName, size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherPartyName,
                        style = MaterialTheme.typography.titleMedium,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = DorjaColors.Jol600,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$otherPartyPhone (Encrypted)",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
                DorjaBadge(
                    text = "SAFE CHANNEL",
                    backgroundColor = DorjaColors.Teal100,
                    textColor = DorjaColors.Teal900
                )
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.kind == "SYSTEM") {
                    SystemMessageBubble(message = msg)
                } else {
                    val isMe = msg.senderUserId == userId
                    UserMessageBubble(message = msg, isMe = isMe)
                }
            }
        }

        // Bottom Input Row
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DorjaColors.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Type an encrypted message...", color = DorjaColors.Gray500) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DorjaColors.Paper50,
                        unfocusedContainerColor = DorjaColors.Paper50,
                        focusedBorderColor = DorjaColors.Jol600,
                        unfocusedBorderColor = DorjaColors.Sand300
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val textToSend = inputText
                            inputText = ""
                            scope.launch {
                                repository.sendMessage(
                                    conversationId = conversationId,
                                    senderId = userId,
                                    text = textToSend
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DorjaColors.Jol600)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = DorjaColors.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: Message, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 14.dp
            ),
            color = if (isMe) DorjaColors.Jol600 else DorjaColors.White,
            border = if (isMe) null else androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Sand300),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) DorjaColors.White else DorjaColors.Ink950
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.formatTimeOnly(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMe) DorjaColors.Teal100 else DorjaColors.Gray500,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun SystemMessageBubble(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DorjaColors.Ink950,
            border = androidx.compose.foundation.BorderStroke(1.dp, DorjaColors.Jol600),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = DorjaColors.Jol600,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
