package com.example.jarvis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.JarvisState
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * JARVIS 2.0 Holographic Central AI Core.
 * Combines multi-layer rotating HUD telemetry, reactive particle glow,
 * dynamic audio waveform visualizer, and real-time state transitions.
 */
@Composable
fun HolographicCoreHero(
    state: JarvisState,
    isListening: Boolean,
    isSpeaking: Boolean,
    liveTranscript: String,
    lastResponse: String,
    rmsDb: Float = 0f,
    coreSize: Dp = 260.dp,
    onCoreClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "holo_core_infinite")

    // Rotation speeds based on state
    val rotationSpeed = when (state) {
        JarvisState.THINKING -> 3500
        JarvisState.LISTENING -> 6000
        JarvisState.SPEAKING -> 5000
        else -> 12000
    }

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_outer_rotation"
    )

    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (rotationSpeed * 1.5).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_counter_rotation"
    )

    // Breathing pulse scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 700
                    JarvisState.THINKING -> 450
                    JarvisState.SPEAKING -> 600
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holo_pulse_scale"
    )

    // Outer glow color
    val glowColor by animateColorAsState(
        targetValue = when (state) {
            JarvisState.IDLE -> JarvisCyan
            JarvisState.LISTENING -> JarvisCyanBright
            JarvisState.THINKING -> JarvisAmber
            JarvisState.SPEAKING -> JarvisGreen
            JarvisState.ERROR -> JarvisRed
        },
        animationSpec = tween(durationMillis = 350),
        label = "holo_glow_color"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Holographic State Status Banner
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xE607101F))
                .border(1.dp, glowColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(glowColor)
                )

                Text(
                    text = when (state) {
                        JarvisState.IDLE -> "JARVIS // ONLINE"
                        JarvisState.LISTENING -> "AUDIO MATRIX // LISTENING..."
                        JarvisState.THINKING -> "NEURAL CORE // PROCESSING..."
                        JarvisState.SPEAKING -> "VOCAL SYNTH // TRANSMITTING"
                        JarvisState.ERROR -> "SYSTEM ALERT // RE-ENGAGING"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = glowColor
                )
            }
        }

        // Central Hologram Viewport
        Box(
            modifier = Modifier
                .size(coreSize)
                .scale(pulseScale)
                .testTag("holographic_core_container"),
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas: Circular HUD geometry & orbital rings
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("holographic_hud_canvas")
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = (size.minDimension / 2f) - 6f
                val midRadius = outerRadius * 0.85f
                val innerRadius = outerRadius * 0.70f

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.18f),
                            glowColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = outerRadius * 1.15f
                    ),
                    radius = outerRadius * 1.15f,
                    center = center
                )

                // Outer rotating segmented HUD ring
                rotate(degrees = rotationAngle, pivot = center) {
                    drawArc(
                        color = glowColor.copy(alpha = 0.45f),
                        startAngle = 10f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = glowColor.copy(alpha = 0.45f),
                        startAngle = 100f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = glowColor.copy(alpha = 0.45f),
                        startAngle = 190f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = glowColor.copy(alpha = 0.45f),
                        startAngle = 280f,
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                    )

                    // 12 Outer degree tick nodes
                    for (i in 0 until 12) {
                        val angleRad = (i * 30f) * (Math.PI.toFloat() / 180f)
                        val p1 = Offset(center.x + cos(angleRad) * (outerRadius - 4f), center.y + sin(angleRad) * (outerRadius - 4f))
                        val p2 = Offset(center.x + cos(angleRad) * (outerRadius + 4f), center.y + sin(angleRad) * (outerRadius + 4f))
                        drawLine(
                            color = glowColor.copy(alpha = 0.6f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.5f
                        )
                    }
                }

                // Counter-rotating middle orbital ring
                rotate(degrees = counterRotationAngle, pivot = center) {
                    drawArc(
                        color = JarvisElectricBlue.copy(alpha = 0.35f),
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = Size(midRadius * 2, midRadius * 2),
                        style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = JarvisElectricBlue.copy(alpha = 0.35f),
                        startAngle = 180f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(center.x - midRadius, center.y - midRadius),
                        size = Size(midRadius * 2, midRadius * 2),
                        style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                    )
                }

                // Inner fine grid circle
                drawCircle(
                    color = glowColor.copy(alpha = 0.25f),
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = 1f)
                )
            }

            // 3D Interactive Hologram Orb Core
            JarvisOrb(
                state = state,
                size = coreSize * 0.82f,
                onClick = onCoreClick
            )
        }

        // Live Audio Waveform bar synchronized with speech & listening
        VoiceWaveform(
            modifier = Modifier
                .padding(vertical = 2.dp),
            barCount = 20,
            height = 36.dp,
            isActive = isListening || isSpeaking || state == JarvisState.THINKING,
            rmsDb = rmsDb,
            accentColor = glowColor
        )

        // Real-time Recognized Speech Live Ticker / Assistant Response Glass Panel
        AnimatedVisibility(
            visible = liveTranscript.isNotBlank() || isListening,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD9060E1C))
                    .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Live Transcription",
                        tint = JarvisCyanBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (liveTranscript.isNotBlank()) liveTranscript else "Listening to your voice...",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = if (liveTranscript.isNotBlank()) JarvisTextPrimary else JarvisTextDim,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
