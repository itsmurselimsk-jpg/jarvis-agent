package com.example.jarvis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextSecondary

enum class NavTab(val label: String, val icon: ImageVector) {
    HOME("HOME", Icons.Default.Adjust),
    CONVERSATION("CHAT", Icons.Default.ChatBubble),
    DEVICES("DEVICES", Icons.Default.Smartphone),
    SKILLS("SKILLS", Icons.Default.AutoAwesome),
    SETTINGS("SETTINGS", Icons.Default.Settings);

    companion object {
        val CHAT: NavTab get() = CONVERSATION
    }
}

@Composable
fun BottomNav(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    // Floating futuristic holographic dock with navigation insets
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Translucent floating holographic pill bar with subtle neon border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF0050B16),
                            Color(0xFA02050B)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            JarvisCyan.copy(alpha = 0.45f),
                            JarvisElectricBlue.copy(alpha = 0.20f),
                            JarvisCyan.copy(alpha = 0.45f)
                        )
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dock_tab_scale"
                    )

                    val pillAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1.0f else 0.0f,
                        animationSpec = tween(durationMillis = 200),
                        label = "dock_pill_alpha"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) JarvisCyanBright else JarvisTextDim,
                        animationSpec = tween(durationMillis = 200),
                        label = "dock_icon_tint"
                    )

                    val textTint by animateColorAsState(
                        targetValue = if (isSelected) JarvisCyan else JarvisTextSecondary.copy(alpha = 0.6f),
                        animationSpec = tween(durationMillis = 200),
                        label = "dock_text_tint"
                    )

                    val isChatTab = tab == NavTab.CONVERSATION

                    // Touch target with minimum 48dp height and width
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(
                                if (isSelected) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            JarvisCyan.copy(alpha = 0.22f * pillAlpha),
                                            Color(0xFF0A1B30).copy(alpha = 0.5f * pillAlpha),
                                            Color.Transparent
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable(
                                interactionSource = remember(tab) { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onTabSelected(tab) }
                            )
                            .testTag(if (isChatTab) "nav_tab_chat" else "nav_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .scale(animatedScale)
                                .then(if (isChatTab) Modifier.testTag("nav_tab_conversation") else Modifier)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) JarvisCyan.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = tab.label,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textTint,
                                letterSpacing = 0.8.sp
                            )

                            // Glowing Indicator Dot
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(JarvisCyanBright)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
