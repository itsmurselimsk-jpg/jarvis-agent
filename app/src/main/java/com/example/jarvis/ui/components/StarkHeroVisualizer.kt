package com.example.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.theme.JarvisAmber
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
 * Next-Gen Stark Arc Reactor HUD Hero Visualizer.
 * Provides high-speed reactive energy rings, orbiting electron nodes,
 * pulse glow wave radiating outwards, and live core metrics.
 */
@Composable
fun StarkArcReactorHero(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    tiltX: Float = 0f,
    tiltY: Float = 0f,
    rmsDb: Float = 0f,
    onClick: () -> Unit
) {
    val armorTheme by ThemeManager.currentTheme.collectAsState()
    val infiniteTransition = rememberInfiniteTransition(label = "stark_arc_reactor")

    val outerSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerSpin"
    )

    val innerSpin by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerSpin"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 650 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val livePulse = corePulse * (1f + (rmsDb.coerceIn(0f, 15f) / 15f) * 0.40f)

    Box(
        modifier = modifier
            .size(240.dp)
            .clickable { onClick() }
            .testTag("stark_arc_reactor_hero"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseCenter = Offset(size.width / 2f, size.height / 2f)
            val center = Offset(
                baseCenter.x + (tiltX.coerceIn(-1f, 1f) * 16.dp.toPx()),
                baseCenter.y + (tiltY.coerceIn(-1f, 1f) * 16.dp.toPx())
            )
            val maxRadius = size.minDimension / 2f - 10.dp.toPx()

            // 1. Ambient Background Radial Flare with Armor Theme
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        armorTheme.accentColor.copy(alpha = 0.32f * livePulse),
                        armorTheme.primaryColor.copy(alpha = 0.16f),
                        armorTheme.glowColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.1f
                ),
                radius = maxRadius * 1.1f,
                center = center
            )

            // 2. Outer Technical Coordinate Ring (Dashed Arc)
            rotate(outerSpin, pivot = center) {
                drawCircle(
                    color = armorTheme.primaryColor.copy(alpha = 0.50f),
                    radius = maxRadius * 0.95f,
                    center = center,
                    style = Stroke(
                        width = 1.8f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 14f, 4f, 14f))
                    )
                )

                // 4 Cardinal Power Pointers
                for (angle in 0 until 360 step 90) {
                    val rad = Math.toRadians(angle.toDouble())
                    val p1 = Offset(
                        (center.x + (maxRadius * 0.90f) * cos(rad)).toFloat(),
                        (center.y + (maxRadius * 0.90f) * sin(rad)).toFloat()
                    )
                    val p2 = Offset(
                        (center.x + (maxRadius * 0.99f) * cos(rad)).toFloat(),
                        (center.y + (maxRadius * 0.99f) * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = armorTheme.accentColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 3. Middle Counter-Rotating Gear Ring
            rotate(innerSpin, pivot = center) {
                drawCircle(
                    color = armorTheme.glowColor.copy(alpha = 0.60f),
                    radius = maxRadius * 0.72f,
                    center = center,
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 16f))
                    )
                )

                // 8 Arc Power Coil Nodes
                for (i in 0 until 8) {
                    val rad = Math.toRadians((i * 45).toDouble())
                    val nodePos = Offset(
                        (center.x + (maxRadius * 0.72f) * cos(rad)).toFloat(),
                        (center.y + (maxRadius * 0.72f) * sin(rad)).toFloat()
                    )
                    drawCircle(
                        color = armorTheme.accentColor,
                        radius = 3.5f * livePulse,
                        center = nodePos
                    )
                }
            }

            // 4. Inner Palladium Core Shell
            drawCircle(
                color = Color(0xCC081224),
                radius = maxRadius * 0.48f,
                center = center
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        armorTheme.accentColor,
                        armorTheme.primaryColor,
                        armorTheme.glowColor.copy(alpha = 0.8f),
                        armorTheme.accentColor
                    ),
                    center = center
                ),
                radius = maxRadius * 0.48f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 5. Central Plasma Core Glow
            val glowRadius = (maxRadius * 0.32f) * livePulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        armorTheme.accentColor.copy(alpha = 0.95f),
                        armorTheme.primaryColor.copy(alpha = 0.70f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
        }

        // Center Voice Icon overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Core",
                tint = Color.Black,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.90f))
                    .padding(6.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isListening) "LISTENING" else "ARC CORE",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = armorTheme.accentColor
            )
        }
    }
}

/**
 * Next-Gen Cybernetic Stark Action Card.
 * Glassmorphic surface, glowing cyber borders, and tactile touch feedback.
 */
@Composable
fun CyberActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xEE0D172A),
                        Color(0xF5080E1A)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Tech pulse indicator dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = JarvisTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = JarvisTextSecondary,
                    lineHeight = 13.sp,
                    maxLines = 2
                )
            }
        }
    }
}
