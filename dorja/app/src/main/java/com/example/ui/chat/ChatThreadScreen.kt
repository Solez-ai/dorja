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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.DorjaFontFamily
import com.example.ui.util.Formatters
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
private val dayHeaderFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

@Composable
fun ChatThreadScreen(
    conversationId: String,
    onBack: () -> Unit
) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val userId = currentUser?.id ?: ""

    val messages by repository.getMessagesByConversation(conversationId)
        .collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }

    var otherPartyName by remember { mutableStateOf("") }
    var otherPartyPhone by remember { mutableStateOf("") }
    var otherPartyId by remember { mutableStateOf("") }
    LaunchedEffect(conversationId) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val conv = DorjaApp.instance.repository.getConversationById(conversationId)
            if (conv != null) {
                otherPartyId = if (userId == conv.hostUserId) conv.seekerUserId else conv.hostUserId
                val otherUser = DorjaApp.instance.repository.getUserById(otherPartyId)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    otherPartyName = otherUser?.displayName?.ifBlank { otherUser.username } ?: otherPartyId
                    otherPartyPhone = otherUser?.phone ?: ""
                }
            }
        }
    }

    // iOS chat behavior: newest message visible when opening the thread.
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .testTag("chat_thread_screen")
    ) {
        // ── Top bar: iOS plain, hairline separator, centered identity ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DorjaColors.White,
            border = androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = DorjaColors.BentoCardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp, start = 8.dp, end = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DorjaColors.Jol600
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DorjaAvatar(name = otherPartyName, size = 40.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = otherPartyName,
                        style = MaterialTheme.typography.titleSmall,
                        color = DorjaColors.Ink950,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = DorjaColors.Gray500,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SAFE CHANNEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray500,
                            fontSize = 9.sp,
                            fontFamily = DorjaFontFamily
                        )
                    }
                }
                DorjaBadge(
                    text = otherPartyPhone,
                    backgroundColor = DorjaColors.Teal100,
                    textColor = DorjaColors.Teal900
                )
            }
        }

        // ── Messages: day dividers + aligned bubbles, auto-scrolled ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var lastDayKey = ""
            messages.forEach { msg ->
                val dayKey = dayKeyFormat.format(Date(msg.createdAt))
                if (dayKey != lastDayKey) {
                    lastDayKey = dayKey
                    item(key = "day_$dayKey") {
                        DayDivider(label = dayHeaderFormat.format(Date(msg.createdAt)))
                    }
                }
                if (msg.kind == "SYSTEM") {
                    item(key = msg.id) {
                        SystemMessageBubble(message = msg)
                    }
                } else {
                    val isMe = msg.senderUserId == userId
                    item(key = msg.id) {
                        MessageBubble(message = msg, isMe = isMe)
                    }
                }
            }
        }

        // ── Composer: frosted pill field + filled circular send button ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DorjaColors.White,
            border = androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = DorjaColors.BentoCardBorder
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text("Message", color = DorjaColors.Gray500)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DorjaColors.Paper50,
                        unfocusedContainerColor = DorjaColors.Paper50,
                        focusedBorderColor = DorjaColors.Jol600.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val canSend = inputText.isNotBlank()
                IconButton(
                    onClick = {
                        if (canSend) {
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
                    enabled = canSend,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) DorjaColors.Jol600
                            else DorjaColors.Gray300
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) DorjaColors.White else DorjaColors.Gray500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Centered date chip — "Today"-style separator between activity days. */
@Composable
private fun DayDivider(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.Gray500,
            fontFamily = DorjaFontFamily,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DorjaColors.Sand100)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

/**
 * iMessage-style bubble: filled accent for my messages, neutral card for
 * theirs, asymmetric corner radius, inline timestamp, read marker.
 */
@Composable
private fun MessageBubble(message: Message, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            ),
            color = if (isMe) DorjaColors.Jol600 else DorjaColors.White,
            border = if (isMe) null
            else androidx.compose.foundation.BorderStroke(0.5.dp, DorjaColors.BentoCardBorder),
            shadowElevation = if (isMe) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMe) DorjaColors.White else DorjaColors.Ink950,
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = Formatters.formatTimeOnly(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500,
                fontSize = 9.sp
            )
            if (isMe) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = DorjaColors.BentoGreenIcon,
                    modifier = Modifier.size(9.dp)
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
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DorjaColors.Ink950,
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
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodySmall,
                    // Ink950 pill with CanvasBg label — a fixed high-
                    // contrast pair, no theme inversion involved.
                    color = DorjaColors.CanvasBg,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
