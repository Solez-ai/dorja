package com.example.ui.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.AiEngineState
import com.example.ai.DorjaAiEngine
import com.example.ai.PropertyAiContext
import com.example.ai.VoiceAssistantHelper
import com.example.ui.components.DorjaButton
import com.example.ui.components.DorjaLogo
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.LiquidGlassDefaults
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.pressScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeyDorjaAssistantSheet(
    propertyContext: PropertyAiContext?,
    onDismiss: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = LocalDarkTheme.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val aiEngine = remember { DorjaAiEngine.getInstance(context) }
    val engineState by aiEngine.engineState.collectAsState()

    val voiceHelper = remember { VoiceAssistantHelper(context) }
    val isListening by voiceHelper.isListening.collectAsState()
    val speechRms by voiceHelper.speechRms.collectAsState()
    val transcribedText by voiceHelper.transcribedText.collectAsState()
    val voiceError by voiceHelper.errorMessage.collectAsState()

    var userQuery by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var isKeyboardMode by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceHelper.startListening(
                onResult = { result ->
                    userQuery = result
                    isThinking = true
                    scope.launch {
                        aiAnswer = aiEngine.answerQuestion(result, propertyContext)
                        isThinking = false
                    }
                }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.stopListening()
        }
    }

    fun submitQuestion(query: String) {
        if (query.isBlank()) return
        userQuery = query
        isThinking = true
        keyboardController?.hide()
        scope.launch {
            aiAnswer = aiEngine.answerQuestion(query, propertyContext)
            isThinking = false
        }
    }

    fun toggleVoice() {
        if (isListening) {
            voiceHelper.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                voiceHelper.startListening(
                    onResult = { result ->
                        userQuery = result
                        isThinking = true
                        scope.launch {
                            aiAnswer = aiEngine.answerQuestion(result, propertyContext)
                            isThinking = false
                        }
                    }
                )
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Outer Scrim
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glass Bottom Sheet Container (Kyant backdrop style)
        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        val glassTint = if (isDark) Color(0xF215181E) else Color(0xF6F8FAFD)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {} // Prevent dismiss click propagation
                .liquidGlass(
                    shape = sheetShape,
                    tint = glassTint,
                    blurRadius = 28.dp,
                    showSpecularBorder = true,
                    borderWidth = 1.2.dp
                )
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .size(width = 44.dp, height = 4.5.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x40FFFFFF) else Color(0x33000000))
            )

            // Header: Official Dorja Logo & "Dorja AI"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DorjaLogo(modifier = Modifier.size(38.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dorja AI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = DorjaColors.Ink950,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // NPU accelerator badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (engineState is AiEngineState.Ready) Color(0x1F0061A4) else Color(0x1F888888),
                            border = BorderStroke(0.8.dp, if (engineState is AiEngineState.Ready) DorjaColors.Jol600.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (engineState is AiEngineState.Ready) "LITERT • NPU ACTIVE" else "LITERT OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (engineState is AiEngineState.Ready) DorjaColors.Jol600 else DorjaColors.Gray700,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (engineState is AiEngineState.Ready) "On-Device Neural Hardware Accelerated" else "Load model in settings to activate",
                        style = MaterialTheme.typography.labelSmall,
                        color = DorjaColors.Gray600,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DorjaColors.Gray700,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Property Context Strip
            if (propertyContext != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0x22FFFFFF) else Color(0x0C0061A4),
                    border = BorderStroke(0.8.dp, DorjaColors.BentoCardBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = DorjaColors.Jol600,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = propertyContext.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DorjaColors.Ink950,
                                maxLines = 1
                            )
                            Text(
                                text = "${propertyContext.location} • ${propertyContext.priceFormatted} • ${propertyContext.sqft} sqft",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray700,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Context",
                            tint = Color(0xFF00875A),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Body Area (Messages / Suggestions / Warning)
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 340.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                when (engineState) {
                    is AiEngineState.NotLoaded -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) Color(0x332B1E12) else Color(0xFFFFF4E5),
                            border = BorderStroke(1.dp, Color(0xFFFFA000).copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Model Not Loaded Yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DorjaColors.Ink950
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "To preserve your privacy and enable instant on-device responses, Dorja runs locally using LiteRT on your phone's NPU/GPU. Go to Settings and tap 'Load Up Model' to initialize.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DorjaColors.Gray700,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DorjaButton(
                                    text = "Go to Settings",
                                    onClick = {
                                        onDismiss()
                                        onNavigateToSettings?.invoke()
                                    },
                                    icon = Icons.Default.Settings,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    is AiEngineState.Loading -> {
                        val state = engineState as AiEngineState.Loading
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = DorjaColors.Jol600,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = DorjaColors.Gray700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    else -> {
                        // If no question asked yet, display quick suggestion chips
                        if (userQuery.isEmpty()) {
                            Text(
                                text = "Ask anything about this property:",
                                style = MaterialTheme.typography.labelSmall,
                                color = DorjaColors.Gray600,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuggestionPill("🚗 Does this property have a parking lot?") {
                                    submitQuestion("Does this property have a parking lot?")
                                }
                                SuggestionPill("📜 What legal documents are verified?") {
                                    submitQuestion("What legal documents are verified?")
                                }
                                SuggestionPill("🤝 What promises did the seller make?") {
                                    submitQuestion("What promises did the seller make?")
                                }
                                SuggestionPill("📐 Room dimensions & 3D scans") {
                                    submitQuestion("What are the room dimensions and is there a 3D scan?")
                                }
                                SuggestionPill("⚡ Power backup & flood risk status") {
                                    submitQuestion("What is the power backup and flood risk status?")
                                }
                                SuggestionPill("💰 Is the price verified?") {
                                    submitQuestion("Can you explain the price and size breakdown?")
                                }
                            }
                        } else {
                            // User Query Bubble
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(4.dp)),
                                    color = DorjaColors.Jol600,
                                    modifier = Modifier.padding(start = 48.dp, bottom = 12.dp)
                                ) {
                                    Text(
                                        text = userQuery,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            // AI Response Bubble
                            if (isThinking) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DorjaLogo(modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = DorjaColors.Jol600
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Computing on NPU / LiteRT...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DorjaColors.Gray600,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else if (aiAnswer != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(18.dp).copy(bottomStart = androidx.compose.foundation.shape.CornerSize(4.dp)),
                                    color = if (isDark) Color(0x28FFFFFF) else Color(0xFFF0F4FA),
                                    border = BorderStroke(0.8.dp, DorjaColors.BentoCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            DorjaLogo(modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Dorja Verified Intelligence",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DorjaColors.Jol600
                                            )
                                        }
                                        Text(
                                            text = aiAnswer.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DorjaColors.Ink950,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Real-time transcribed text if speaking
            if (isListening && transcribedText.isNotBlank()) {
                Text(
                    text = "“$transcribedText”",
                    style = MaterialTheme.typography.bodySmall,
                    color = DorjaColors.Jol600,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            // Voice Error if any
            if (voiceError != null && !isListening) {
                Text(
                    text = voiceError.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = DorjaColors.Error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Control Area: Mic Orb & Keyboard Toggle (Google Assistant clone)
            if (isKeyboardMode) {
                // Text Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        placeholder = {
                            Text(
                                "Ask e.g. Does this have parking?",
                                fontSize = 13.sp,
                                color = DorjaColors.Gray500
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DorjaColors.Jol600,
                            unfocusedBorderColor = DorjaColors.BentoCardBorder,
                            focusedContainerColor = if (isDark) Color(0x33000000) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0x22000000) else Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                submitQuestion(textInput)
                                textInput = ""
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            submitQuestion(textInput)
                            textInput = ""
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DorjaColors.Jol600)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Switch back to Mic
                    IconButton(
                        onClick = { isKeyboardMode = false },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice mode",
                            tint = DorjaColors.Gray700
                        )
                    }
                }
            } else {
                // Google Assistant style glowing mic and keyboard toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Keyboard toggle on left
                    IconButton(
                        onClick = { isKeyboardMode = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x100061A4))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Switch to keyboard typing",
                            tint = DorjaColors.Ink950,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Center: Glowing & Pulsing Mic Orb
                    PulsingMicButton(
                        isListening = isListening,
                        speechRms = speechRms,
                        onClick = { toggleVoice() }
                    )

                    // Spacer/Balance on right (or reset button)
                    IconButton(
                        onClick = {
                            userQuery = ""
                            aiAnswer = null
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x100061A4))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Dorja Trust Info",
                            tint = DorjaColors.Jol600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isListening) "Listening... Speak your question" else "Tap mic to ask or click keyboard to type",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isListening) DorjaColors.Jol600 else DorjaColors.Gray600,
                    fontSize = 11.sp,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SuggestionPill(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DorjaColors.White.copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, DorjaColors.BentoCardBorder),
        modifier = Modifier.pressScale(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = DorjaColors.Ink950,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun PulsingMicButton(
    isListening: Boolean,
    speechRms: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.35f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 650 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val effectiveScale = if (isListening) (pulseScale + speechRms * 0.25f) else 1f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(76.dp)
            .clickable(onClick = onClick)
    ) {
        // Outer glowing halo
        Box(
            modifier = Modifier
                .size(62.dp)
                .scale(effectiveScale)
                .clip(CircleShape)
                .background(
                    if (isListening) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x8000C896),
                                Color(0x4D0061A4),
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x330061A4),
                                Color.Transparent
                            )
                        )
                    }
                )
        )

        // Core Mic Button
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isListening) {
                            listOf(Color(0xFF00C896), DorjaColors.Jol600)
                        } else {
                            listOf(DorjaColors.Jol600, Color(0xFF003865))
                        }
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.2f))
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening..." else "Start Voice Input",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
