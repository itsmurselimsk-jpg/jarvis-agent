package com.example.jarvis.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.components.HologramTheme
import com.example.jarvis.ui.components.HologramThemeManager
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.voice.CyberneticAudioEngine

/**
 * Holographic HUD Scanline and Corner Reticles for the J.A.R.V.I.S. Interface.
 */
@Composable
fun HolographicScanlines(
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 4.dp,
    lineColor: Color = Color.Black.copy(alpha = 0.22f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = lineSpacing.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.2f
            )
            y += spacingPx
        }
    }
}

/**
 * Cybernetic HUD Corner Brackets.
 */
@Composable
fun HolographicCornerBrackets(
    modifier: Modifier = Modifier,
    bracketColor: Color = JarvisCyan.copy(alpha = 0.6f),
    bracketLength: Dp = 16.dp,
    strokeWidth: Dp = 1.5.dp
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val len = bracketLength.toPx()
        val sw = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        // Top-Left
        drawLine(bracketColor, Offset(0f, 0f), Offset(len, 0f), sw, StrokeCap.Square)
        drawLine(bracketColor, Offset(0f, 0f), Offset(0f, len), sw, StrokeCap.Square)

        // Top-Right
        drawLine(bracketColor, Offset(w, 0f), Offset(w - len, 0f), sw, StrokeCap.Square)
        drawLine(bracketColor, Offset(w, 0f), Offset(w, len), sw, StrokeCap.Square)

        // Bottom-Left
        drawLine(bracketColor, Offset(0f, h), Offset(len, h), sw, StrokeCap.Square)
        drawLine(bracketColor, Offset(0f, h), Offset(0f, h - len), sw, StrokeCap.Square)

        // Bottom-Right
        drawLine(bracketColor, Offset(w, h), Offset(w - len, h), sw, StrokeCap.Square)
        drawLine(bracketColor, Offset(w, h), Offset(w, h - len), sw, StrokeCap.Square)
    }
}

/**
 * J.A.R.V.I.S. Camera Gesture Preview HUD Panel.
 */
@Composable
fun GestureCameraPanel(
    isVisible: Boolean,
    isGestureActive: Boolean,
    gestureMode: String, // "STANDBY", "SPIN (1 HAND)", "ZOOM (2 HANDS)"
    onToggleGesture: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xEE080C14))
                .border(1.dp, JarvisCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Simulated Camera/Optical Tracking Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF04070D))
                    .border(0.8.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Background scanlines
                HolographicScanlines(
                    lineSpacing = 3.dp,
                    lineColor = JarvisCyan.copy(alpha = 0.12f)
                )

                // Corner reticles
                HolographicCornerBrackets(
                    bracketColor = JarvisCyanBright.copy(alpha = 0.5f),
                    bracketLength = 8.dp
                )

                // Center tracking crosshair
                Canvas(modifier = Modifier.size(32.dp)) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = if (isGestureActive) JarvisCyanBright else JarvisAmber,
                        radius = 12.dp.toPx(),
                        center = c,
                        style = Stroke(width = 1.2f)
                    )
                    drawLine(
                        color = if (isGestureActive) JarvisCyanBright else JarvisAmber,
                        start = Offset(c.x - 14.dp.toPx(), c.y),
                        end = Offset(c.x + 14.dp.toPx(), c.y),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = if (isGestureActive) JarvisCyanBright else JarvisAmber,
                        start = Offset(c.x, c.y - 14.dp.toPx()),
                        end = Offset(c.x, c.y + 14.dp.toPx()),
                        strokeWidth = 1f
                    )
                }

                // Status banner at bottom of camera frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xCC000000))
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gestureMode,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isGestureActive) JarvisCyanBright else JarvisAmber,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom Out
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1A2A))
                        .border(1.dp, JarvisBorderSubtle(), RoundedCornerShape(4.dp))
                        .clickable {
                            CyberneticAudioEngine.playOrbBeep(900f, 30)
                            onZoomOut()
                        }
                        .testTag("orb_zoom_out"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = JarvisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Zoom In
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1A2A))
                        .border(1.dp, JarvisBorderSubtle(), RoundedCornerShape(4.dp))
                        .clickable {
                            CyberneticAudioEngine.playOrbBeep(1500f, 30)
                            onZoomIn()
                        }
                        .testTag("orb_zoom_in"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = JarvisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Reset
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1A2A))
                        .border(1.dp, JarvisBorderSubtle(), RoundedCornerShape(4.dp))
                        .clickable {
                            CyberneticAudioEngine.playOrbBeep(1200f, 40)
                            onReset()
                        }
                        .padding(horizontal = 8.dp)
                        .testTag("orb_reset"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RESET",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyan
                    )
                }
            }

            // Toggle Gestures Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isGestureActive) JarvisCyan.copy(alpha = 0.25f)
                        else Color(0xFF141F30)
                    )
                    .border(
                        1.dp,
                        if (isGestureActive) JarvisCyanBright else JarvisCyan.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        CyberneticAudioEngine.playScanPing()
                        onToggleGesture()
                    }
                    .testTag("toggle_gestures_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isGestureActive) "GESTURES ON" else "GESTURES OFF",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = if (isGestureActive) JarvisCyanBright else JarvisCyan
                )
            }

            // Theme Matrix Quick Switcher
            HolographicThemeSelector(modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Cybernetic Theme Matrix Selector for switching between J.A.R.V.I.S. CLASSIC, COMBAT CRIMSON, ARC GOLD, QUANTUM, and STEALTH cores.
 */
@Composable
fun HolographicThemeSelector(
    modifier: Modifier = Modifier
) {
    val activeTheme by HologramThemeManager.currentTheme.collectAsState()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0A1220))
            .border(0.8.dp, activeTheme.primary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "CORE MATRIX",
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = activeTheme.primary.copy(alpha = 0.8f),
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HologramTheme.entries.forEach { theme ->
                val isSelected = theme == activeTheme
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) theme.primary.copy(alpha = 0.3f) else Color(0xFF101824))
                        .border(
                            width = if (isSelected) 1.2.dp else 0.5.dp,
                            color = if (isSelected) theme.primary else theme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .clickable {
                            HologramThemeManager.setTheme(theme)
                            if (theme == HologramTheme.JARVIS_CRIMSON) {
                                CyberneticAudioEngine.playShieldEngage()
                            } else {
                                CyberneticAudioEngine.playReactorSurge(200)
                            }
                        }
                        .testTag("theme_chip_${theme.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(theme.primary)
                    )
                }
            }
        }
    }
}

private fun JarvisBorderSubtle(): Color = Color(0x3344AAFF)
