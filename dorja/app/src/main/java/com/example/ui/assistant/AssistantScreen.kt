package com.example.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.ai.DorjaAssistant
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import kotlinx.coroutines.launch

/**
 * On-device property assistant. The heavy lifting (model download, init,
 * tool-calling loop) lives in [DorjaAssistant]; this screen is a plain chat
 * surface over it.
 */
@Composable
fun AssistantScreen(onBack: () -> Unit) {
    val repository = DorjaApp.instance.repository
    val scope = rememberCoroutineScope()
    val assistant = remember { DorjaAssistant(repository) }
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<AiMessage>()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var initAttempt by remember { mutableIntStateOf(0) }

    // Model download + init on first entry; retryable via initAttempt.
    LaunchedEffect(initAttempt) {
        failed = false
        ready = assistant.ensureReady()
        if (!ready) failed = true
    }

    DisposableEffect(Unit) {
        onDispose { assistant.unload() }
    }

    // L() is composable — resolve the fallback strings during composition;
    // send() below is a plain function and must not call them itself.
    val unmatchedText = L("assistant_unmatched")
    val errorText = L("assistant_error")

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy || !ready) return
        input = ""
        messages = messages + AiMessage(text, fromUser = true)
        busy = true
        scope.launch {
            val reply = when (val turn = assistant.ask(text)) {
                is DorjaAssistant.Turn.Result -> turn.message
                DorjaAssistant.Turn.Unmatched -> unmatchedText
                is DorjaAssistant.Turn.Error -> {
                    failed = true
                    initAttempt++
                    errorText
                }
            }
            messages = messages + AiMessage(reply, fromUser = false)
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg),
    ) {
        // ── Top bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("assistant_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = L("common_back"),
                    tint = DorjaColors.Ink950,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = L("assistant_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DorjaColors.Ink950,
                )
                Text(
                    text = L("assistant_subtitle"),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Gray500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Bridge status pill: downloading / ready / retry.
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = when {
                    ready -> DorjaColors.BentoGreenBg
                    failed -> DorjaColors.ErrorContainer
                    else -> DorjaColors.BentoAmberBg
                },
                modifier = Modifier
                    .padding(end = 12.dp)
                    .let { if (failed) it.clickableToRetry { initAttempt++ } else it },
            ) {
                Text(
                    text = when {
                        ready -> L("assistant_ready")
                        failed -> L("assistant_failed")
                        else -> L("assistant_downloading")
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        ready -> DorjaColors.BentoGreenText
                        failed -> DorjaColors.Error
                        else -> DorjaColors.BentoAmberText
                    },
                )
            }
        }

        // ── Messages ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DorjaColors.White,
                    ) {
                        Text(
                            text = L("assistant_welcome"),
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = DorjaColors.Gray700,
                        )
                    }
                }
            }
            items(messages) { m ->
                MessageRow(m)
            }
            if (busy) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DorjaColors.White,
                    ) {
                        Text(
                            text = "…",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DorjaColors.Gray500,
                        )
                    }
                }
            }
        }

        // ── Input row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DorjaColors.CanvasBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(L("assistant_input_hint"), style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("assistant_input"),
                singleLine = true,
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DorjaColors.White,
                    unfocusedContainerColor = DorjaColors.White,
                    focusedBorderColor = DorjaColors.Jol600,
                    unfocusedBorderColor = DorjaColors.BentoCardBorder,
                ),
            )
            Spacer(modifier = Modifier.height(0.dp))
            IconButton(
                onClick = { send() },
                enabled = ready && !busy && input.isNotBlank(),
                modifier = Modifier.testTag("assistant_send"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = L("common_send"),
                    tint = if (ready && !busy && input.isNotBlank()) {
                        DorjaColors.Jol600
                    } else {
                        DorjaColors.Gray500
                    },
                )
            }
        }
    }

    // Keep the latest message visible.
    LaunchedEffect(messages.size, busy) {
        val last = messages.lastIndex
        if (last >= 0) listState.animateScrollToItem(last)
    }
}

/** One chat bubble's data. */
private data class AiMessage(
    val text: String,
    val fromUser: Boolean,
    val isError: Boolean = false,
)

@Composable
private fun MessageRow(m: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (m.fromUser) 16.dp else 4.dp,
                bottomEnd = if (m.fromUser) 4.dp else 16.dp,
            ),
            color = when {
                m.fromUser -> DorjaColors.Jol600
                m.isError -> DorjaColors.ErrorContainer
                else -> DorjaColors.White
            },
        ) {
            Text(
                text = m.text,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    m.fromUser -> Color.White
                    m.isError -> DorjaColors.Error
                    else -> DorjaColors.Ink950
                },
            )
        }
    }
}

private fun Modifier.clickableToRetry(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
