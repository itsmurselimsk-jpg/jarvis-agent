package com.jarvis.ai.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.R
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBorder
import com.jarvis.ai.ui.theme.JarvisBorderSubtle
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisCyanBright
import com.jarvis.ai.ui.theme.JarvisGreen
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.ui.theme.JarvisSurfaceElevated
import com.jarvis.ai.ui.theme.JarvisTextDim
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("about_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Official JARVIS Emblem
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .border(2.dp, JarvisCyan.copy(alpha = 0.8f), CircleShape)
                .background(JarvisSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.jarvis_logo),
                contentDescription = "Official JARVIS Logo",
                modifier = Modifier
                    .size(190.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Identity Typography
        Text(
            text = "J . A . R . V . I . S .",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
            color = JarvisCyanBright
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "— PERSONAL AI ASSISTANT —",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            color = JarvisTextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "T H I N K   •   H E L P   •   D O",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            color = JarvisCyan
        )

        Spacer(modifier = Modifier.height(24.dp))

        // System Core Specs Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurface)
                .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "SYSTEM ARCHITECTURE SPECIFICATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = JarvisCyan
                )

                SpecRow(
                    icon = Icons.Default.Speed,
                    label = "Core Version",
                    value = "v5.0.0 (Supreme Omni-OS)"
                )
                SpecRow(
                    icon = Icons.Default.Memory,
                    label = "Cognitive Engine",
                    value = "JARVIS HYPER-COGNITIVE OMNI-BRAIN (Level-Infinity 4-Agent Council & ToT Matrix)"
                )
                SpecRow(
                    icon = Icons.Default.Shield,
                    label = "Air-Gap Guard",
                    value = "Quantum Air-Gap Sentinel & Biometric Armor Level-MAX"
                )
                SpecRow(
                    icon = Icons.Default.Lock,
                    label = "Memory Vault",
                    value = "AES-256 Infinite Vector Memory Nexus (Room SQLite + Zero-Leak)"
                )
                SpecRow(
                    icon = Icons.Default.Code,
                    label = "Hardware Bridge",
                    value = "Omni-Hardware Mesh (CameraX AR, Procedural AudioTrack, SensorManager)"
                )
                SpecRow(
                    icon = Icons.Default.Speed,
                    label = "Neural Reflex",
                    value = "8ms Sub-Reflex Engine (Truth-Verified)"
                )
                SpecRow(
                    icon = Icons.Default.Memory,
                    label = "Tool Matrix",
                    value = "200+ Executive Capabilities Registered"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Integrity Statement
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Integrity",
                    tint = JarvisGreen,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "LOCAL-FIRST INTEGRITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "JARVIS operates directly on your Android hardware. Sensitive personal context, telemetry, and execution logs remain on-device unless explicitly delegated through your chosen encrypted AI provider.",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SpecRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary
            )
        }
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = JarvisTextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.2f)
        )
    }
}
