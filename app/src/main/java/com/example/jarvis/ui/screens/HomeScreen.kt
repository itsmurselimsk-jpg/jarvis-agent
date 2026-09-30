package com.example.jarvis.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.ActivityLog
import com.example.jarvis.model.ChatMessage
import com.example.jarvis.model.DeviceTelemetry
import com.example.jarvis.model.JarvisState
import com.example.jarvis.model.JarvisTask
import com.example.jarvis.model.MemoryItem
import com.example.jarvis.model.MessageSender
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.storage.db.ExpenseEntity
import com.example.jarvis.storage.db.HabitEntity
import com.example.jarvis.ui.components.HolographicCoreHero
import com.example.jarvis.ui.components.VoiceWaveform
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisPurpleHighlight
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium 3D Holographic JARVIS Main Interface.
 * Pure, futuristic, uncluttered AI assistant interface centered strictly around
 * the 3D Holographic AI Core, interactive neural matrix, dynamic vocal waveforms,
 * and high-speed protocol dispatch.
 */
@Composable
fun HomeScreen(
    jarvisState: JarvisState,
    telemetry: DeviceTelemetry,
    isListening: Boolean,
    isSpeaking: Boolean,
    liveTranscript: String,
    lastResponse: String,
    messages: List<ChatMessage> = emptyList(),
    tasks: List<JarvisTask> = emptyList(),
    memories: List<MemoryItem> = emptyList(),
    logs: List<ActivityLog> = emptyList(),
    settings: ProviderSettings = ProviderSettings(),
    expenses: List<ExpenseEntity> = emptyList(),
    habits: List<HabitEntity> = emptyList(),
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    rmsDb: Float = 0f,
    onVoiceClick: () -> Unit,
    onChatClick: () -> Unit,
    onToolsClick: () -> Unit,
    onMemoryClick: () -> Unit,
    onActivityClick: () -> Unit,
    onVisionClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onBridgeClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onArmorClick: () -> Unit = {},
    onExpenseClick: () -> Unit = {},
    onHabitClick: () -> Unit = {},
    onCodeStudioClick: () -> Unit = {},
    onTimerClick: () -> Unit = {},
    onVoiceNotesClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onDiagnosticsClick: () -> Unit = {},
    onQuickCommand: (String) -> Unit
) {
    var quickInputText by remember { mutableStateOf("") }
    var isConversationExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_main_hologram")

    // Ambient floating particle shift
    val particleShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hologram_particle_shift"
    )

    // Holographic title shimmer & breathing
    val titleGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_glow_pulse"
    )

    val currentGlowColor by animateColorAsState(
        targetValue = when (jarvisState) {
            JarvisState.IDLE -> JarvisCyanBright
            JarvisState.LISTENING -> JarvisCyan
            JarvisState.THINKING -> JarvisAmber
            JarvisState.SPEAKING -> JarvisGreen
            JarvisState.ERROR -> JarvisRed
        },
        animationSpec = tween(400),
        label = "main_glow_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("home_screen")
    ) {
        // 1. Background 3D Holographic Particle Grid & Ambient Projection Light
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("holographic_ambient_canvas")
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height * 0.38f

            // Holographic Projection Base Cone Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentGlowColor.copy(alpha = 0.12f * titleGlowPulse),
                        JarvisCyan.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(centerX, centerY)
            )

            // Lightweight 3D Floating Particles
            val particleCount = 18
            for (i in 0 until particleCount) {
                val angle = (particleShift + i * (360f / particleCount)) * (Math.PI.toFloat() / 180f)
                val dist = (width * 0.22f) + (i % 5) * 26f
                val px = centerX + cos(angle) * dist
                val py = centerY + sin(angle) * (dist * 0.65f)

                val alpha = (0.2f + 0.6f * ((sin(particleShift * 0.05f + i).toFloat() + 1f) / 2f)).coerceIn(0.1f, 0.9f)
                drawCircle(
                    color = currentGlowColor.copy(alpha = alpha),
                    radius = (1.5f + (i % 3) * 0.8f),
                    center = Offset(px, py)
                )
            }

            // Subtle Horizon Holographic Perspective Grid Lines at Base
            val baseY = height * 0.58f
            for (row in 0..3) {
                val lineY = baseY + row * 22f
                val lineAlpha = (0.15f - row * 0.035f).coerceAtLeast(0.02f)
                drawLine(
                    color = JarvisCyan.copy(alpha = lineAlpha),
                    start = Offset(width * 0.1f - row * 15f, lineY),
                    end = Offset(width * 0.9f + row * 15f, lineY),
                    strokeWidth = 1f
                )
            }
        }

        // 2. Main Scrollable Content Container
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP HEADER: Pure 3D Holographic "JARVIS" Title Only
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 3D Holographic Text with multi-layered depth & glow
                        Box(contentAlignment = Alignment.Center) {
                            // Deep background glow shadow (Layer 1)
                            Text(
                                text = "JARVIS",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 8.sp,
                                color = currentGlowColor.copy(alpha = 0.35f * titleGlowPulse),
                                modifier = Modifier.offset(y = 2.dp)
                            )

                            // Foreground crisp 3D holographic title (Layer 2)
                            Text(
                                text = "JARVIS",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 8.sp,
                                color = Color.White
                            )
                        }

                        // Futuristic Holographic Projection Beam Accents
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(1.5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, currentGlowColor)
                                        )
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(currentGlowColor)
                            )
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(1.5.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(currentGlowColor, Color.Transparent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // 3. MAIN 3D HOLOGRAPHIC JARVIS AI CORE CENTERPIECE
            item {
                HolographicCoreHero(
                    state = jarvisState,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    liveTranscript = liveTranscript,
                    lastResponse = lastResponse,
                    rmsDb = rmsDb,
                    coreSize = 250.dp,
                    onCoreClick = onVoiceClick
                )
            }

            // 4. HOLOGRAPHIC VOICE TRIGGER & QUICK NEURAL INPUT
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Floating Holographic Mic Trigger Button
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isListening) listOf(JarvisRed, JarvisAmber, JarvisCyan)
                                    else listOf(JarvisCyanBright, JarvisCyan, JarvisElectricBlue)
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(JarvisCyanBright, JarvisElectricBlue, JarvisCyanBright)
                                ),
                                shape = CircleShape
                            )
                            .clickable { onVoiceClick() }
                            .testTag("home_voice_trigger_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Activation",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = if (isListening) "LISTENING • TAP TO CANCEL" else "TAP CORE OR MIC TO SPEAK",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = if (isListening) JarvisAmber else JarvisCyan.copy(alpha = 0.85f)
                    )

                    // Translucent Holographic Command Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickInputText,
                            onValueChange = { quickInputText = it },
                            placeholder = {
                                Text(
                                    "Ask JARVIS or give a command...",
                                    fontSize = 13.sp,
                                    color = JarvisTextDim
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_quick_command_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderSubtle,
                                focusedContainerColor = Color(0xB3050E1E),
                                unfocusedContainerColor = Color(0x99050E1E),
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            ),
                            shape = RoundedCornerShape(26.dp),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (quickInputText.isNotBlank()) {
                                    onQuickCommand(quickInputText)
                                    quickInputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(JarvisCyanBright)
                                .testTag("home_quick_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Command",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 5. CONVERSATION STREAM PREVIEW (Collapsible Holographic Pod)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xCC071120))
                        .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isConversationExpanded = !isConversationExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Conversation",
                                tint = JarvisCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "CONVERSATION STREAM (${messages.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyan
                            )
                        }

                        Icon(
                            imageVector = if (isConversationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Expand",
                            tint = JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Recent message preview when collapsed
                    if (!isConversationExpanded) {
                        val lastMsg = messages.lastOrNull()
                        if (lastMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${if (lastMsg.sender == MessageSender.USER) "OPERATOR:" else "JARVIS:"} ${lastMsg.text}",
                                fontSize = 11.sp,
                                color = if (lastMsg.sender == MessageSender.USER) JarvisCyanBright else JarvisTextPrimary,
                                maxLines = 2
                            )
                        }
                    }

                    // Expanded conversation view
                    AnimatedVisibility(
                        visible = isConversationExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val recentMessages = messages.takeLast(4)
                            if (recentMessages.isEmpty()) {
                                Text(
                                    text = "No recent messages. Speak or type to begin.",
                                    fontSize = 11.sp,
                                    color = JarvisTextDim
                                )
                            } else {
                                recentMessages.forEach { msg ->
                                    val isUser = msg.sender == MessageSender.USER
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isUser) Color(0x2E00B0FF) else Color(0x2E00E5FF))
                                            .border(0.5.dp, if (isUser) JarvisElectricBlue else JarvisCyan, RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isUser) "// OPERATOR" else "// JARVIS",
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isUser) JarvisElectricBlue else JarvisCyanBright
                                            )
                                            Text(
                                                text = msg.text,
                                                fontSize = 12.sp,
                                                color = JarvisTextPrimary
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "OPEN FULL CONVERSATION →",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyanBright,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clickable { onChatClick() }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. HOLOGRAPHIC QUICK PROTOCOLS
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "NEURAL PROTOCOLS // QUICK DISPATCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        color = JarvisCyan
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            ProtocolChip(
                                title = "Protocol Zero",
                                desc = "System Sync",
                                icon = Icons.Default.Bolt,
                                color = JarvisCyanBright,
                                onClick = { onQuickCommand("JARVIS, protocol zero") }
                            )
                        }
                        item {
                            ProtocolChip(
                                title = "Morning Protocol",
                                desc = "Briefing",
                                icon = Icons.Default.WbSunny,
                                color = Color(0xFFFFD700),
                                onClick = { onQuickCommand("JARVIS, morning protocol") }
                            )
                        }
                        item {
                            ProtocolChip(
                                title = "Focus Mode",
                                desc = "Cognitive Lock",
                                icon = Icons.Default.Adjust,
                                color = JarvisPurpleHighlight,
                                onClick = { onQuickCommand("JARVIS, focus mode") }
                            )
                        }
                        item {
                            ProtocolChip(
                                title = "Diagnostics",
                                desc = "Health Scan",
                                icon = Icons.Default.Speed,
                                color = JarvisElectricBlue,
                                onClick = { onQuickCommand("JARVIS, diagnostic scan") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolChip(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xDE081326))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextPrimary
                )
                Text(
                    text = desc,
                    fontSize = 9.sp,
                    color = JarvisTextSecondary
                )
            }
        }
    }
}
