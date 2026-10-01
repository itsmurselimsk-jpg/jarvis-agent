package com.jarvis.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.jarvis.ai.model.ChatMessage
import com.jarvis.ai.model.JarvisState
import com.jarvis.ai.model.MessageSender
import com.jarvis.ai.ui.theme.JarvisAmber
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBorder
import com.jarvis.ai.ui.theme.JarvisBorderSubtle
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisCyanBright
import com.jarvis.ai.ui.theme.JarvisElectricBlue
import com.jarvis.ai.ui.theme.JarvisGreen
import com.jarvis.ai.ui.theme.JarvisRed
import com.jarvis.ai.ui.theme.JarvisTextDim
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    jarvisState: JarvisState,
    isDarkTheme: Boolean = true,
    accentColor: Color = JarvisCyan,
    onSendMessage: (String) -> Unit,
    onCopyMessage: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onRetryMessage: () -> Unit,
    onClearChat: () -> Unit,
    onBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onVisionClick: () -> Unit = {},
    onCodeStudioClick: () -> Unit = {},
    onGetPlusClick: () -> Unit = {},
    isOfflineBrain: Boolean = false,
    onNavigateSettings: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when messages update
    LaunchedEffect(messages.size, messages.lastOrNull()?.text, jarvisState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .imePadding()
            .testTag("chat_screen")
            .testTag("conversation_screen")
    ) {
        // Holographic Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x2200E5FF))
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = JarvisCyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (jarvisState == JarvisState.THINKING) JarvisAmber else JarvisCyanBright)
                        )
                        Text(
                            text = "CONVERSATION // NEURAL LINK",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp,
                            color = JarvisCyanBright
                        )
                    }
                    Text(
                        text = "Multilingual • English, Hindi, Hinglish Context",
                        fontSize = 10.sp,
                        color = JarvisTextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FF1744))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat",
                            tint = JarvisRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x2200E5FF))
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Voice Mode",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Messages List or Holographic Empty State
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x1A00E5FF))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Neural Core",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "J.A.R.V.I.S. READY",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        color = JarvisCyanBright
                    )

                    Text(
                        text = "Awaiting command. Ask questions, translate, automate tasks, or start voice conversation in English, Hindi or Hinglish.",
                        fontSize = 12.sp,
                        color = JarvisTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    // Starter Quick Prompts
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickHoloPrompt(
                            prompt = "JARVIS, run a full system diagnostics check",
                            onClick = { onSendMessage("JARVIS, run a full system diagnostics check") }
                        )
                        QuickHoloPrompt(
                            prompt = "What is the latest news and weather update?",
                            onClick = { onSendMessage("What is the latest news and weather update?") }
                        )
                        QuickHoloPrompt(
                            prompt = "Aaj ka schedule aur tasks summarize karo",
                            onClick = { onSendMessage("Aaj ka schedule aur tasks summarize karo") }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    HolographicMessageBubble(
                        message = msg,
                        onCopy = { onCopyMessage(msg.text) },
                        onSpeak = { onSpeakMessage(msg.text) },
                        onRetry = onRetryMessage
                    )
                }

                // Thinking / Streaming Indicator
                if (jarvisState == JarvisState.THINKING) {
                    item {
                        HolographicThinkingBubble()
                    }
                }
            }
        }

        // Attachment Quick Sheet
        AnimatedVisibility(visible = showAttachmentMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xEE060C18))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AttachmentChip(
                    name = "Vision OCR",
                    icon = Icons.Default.CameraAlt,
                    color = JarvisCyanBright,
                    onClick = {
                        showAttachmentMenu = false
                        onVisionClick()
                    }
                )
                AttachmentChip(
                    name = "Code Studio",
                    icon = Icons.Default.Code,
                    color = JarvisElectricBlue,
                    onClick = {
                        showAttachmentMenu = false
                        onCodeStudioClick()
                    }
                )
                AttachmentChip(
                    name = "Diagnostics",
                    icon = Icons.Default.Refresh,
                    color = JarvisGreen,
                    onClick = {
                        showAttachmentMenu = false
                        onSendMessage("JARVIS, diagnostics scan")
                    }
                )
            }
        }

        // Floating Futuristic Holographic Input Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xEB07101F))
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                JarvisCyan.copy(alpha = 0.5f),
                                JarvisElectricBlue.copy(alpha = 0.2f),
                                JarvisCyan.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentMenu = !showAttachmentMenu },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach",
                            tint = JarvisCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Ask JARVIS or give command...",
                                fontSize = 13.sp,
                                color = JarvisTextDim
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        maxLines = 4
                    )

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = inputText.trim()
                                if (text.isNotBlank()) {
                                    onSendMessage(text)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(JarvisCyan)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onVoiceClick,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x2B00E5FF))
                                .testTag("chat_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = JarvisCyanBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolographicMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRetry: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) Color(0xD9081830)
                    else Color(0xE6050E1C)
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) JarvisElectricBlue.copy(alpha = 0.45f) else JarvisCyan.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Header: Role Tag + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "// OPERATOR" else "// J.A.R.V.I.S. 2.0",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = if (isUser) JarvisElectricBlue else JarvisCyanBright
                    )

                    Text(
                        text = timeStr,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextDim
                    )
                }

                // Message Text
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = JarvisTextPrimary,
                    lineHeight = 18.sp
                )

                // Action Row on AI Messages
                if (!isUser) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak",
                                tint = JarvisCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolographicThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_anim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD9060E1C))
            .border(1.dp, JarvisAmber.copy(alpha = alpha), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = JarvisAmber,
                strokeWidth = 1.5.dp
            )
            Text(
                text = "Processing neural query...",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisAmber
            )
        }
    }
}

@Composable
private fun QuickHoloPrompt(
    prompt: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x99060F1E))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "› $prompt",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = JarvisCyan
        )
    }
}

@Composable
private fun AttachmentChip(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = color, modifier = Modifier.size(14.dp))
            Text(text = name, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = color)
        }
    }
}
