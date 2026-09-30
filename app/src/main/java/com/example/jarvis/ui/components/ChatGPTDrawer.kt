package com.example.jarvis.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue

@Composable
fun ChatGPTDrawer(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    accentColor: Color = JarvisCyan,
    userName: String = "OPERATOR",
    recentConversations: List<String> = emptyList(),
    onImagesClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onProjectsClick: () -> Unit = {},
    onScheduledClick: () -> Unit = {},
    onPluginsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSelectConversation: (String) -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onVoiceModeClick: () -> Unit = {}
) {
    val bg = Color(0xFF070F1E)
    val textPrimary = Color(0xFFE6F1FF)
    val textSecondary = Color(0xFF88A0C0)
    val dividerColor = Color(0x3300E5FF)
    val searchBg = Color(0x2600E5FF)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(bg)
            .border(1.dp, dividerColor, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .padding(top = 24.dp, bottom = 24.dp, start = 18.dp, end = 18.dp)
    ) {
        // 1. Top Header: Futuristic 3D Holographic "JARVIS" Title and Search Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.strix_logo),
                        contentDescription = "STRIX Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, JarvisCyan.copy(alpha = 0.5f), CircleShape)
                    )
                    Text(
                        text = "STRIX",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp,
                        color = JarvisCyanBright
                    )
                }
                Text(
                    text = "NEURAL AI SYSTEM",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(start = 18.dp, top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(searchBg)
                    .border(1.dp, JarvisCyan.copy(alpha = 0.4f), CircleShape)
                    .clickable { onSearchClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = JarvisCyanBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Clean, Focused JARVIS System Navigation Hub
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DrawerNavItem(
                icon = Icons.Default.Image,
                title = "Vision Matrix",
                subTitle = "Visual Neural Scanner",
                accentColor = JarvisCyan,
                textColor = textPrimary,
                onClick = onImagesClick
            )
            DrawerNavItem(
                icon = Icons.Default.Psychology,
                title = "Neural Memory",
                subTitle = "Adaptive Memory Vault",
                accentColor = JarvisElectricBlue,
                textColor = textPrimary,
                onClick = onLibraryClick
            )
            DrawerNavItem(
                icon = Icons.Default.Code,
                title = "Code Studio",
                subTitle = "Autonomous REPL & Sandbox",
                accentColor = JarvisCyanBright,
                textColor = textPrimary,
                onClick = onProjectsClick
            )
            DrawerNavItem(
                icon = Icons.Default.AccessTime,
                title = "Chronometer",
                subTitle = "Stark Precision Timers",
                accentColor = Color(0xFFFFB300),
                textColor = textPrimary,
                onClick = onScheduledClick
            )
            DrawerNavItem(
                icon = Icons.Default.Search,
                title = "Knowledge Engine",
                subTitle = "Web Research Matrix",
                accentColor = Color(0xFF00E676),
                textColor = textPrimary,
                onClick = onSearchClick
            )
        }

        // 3. Clean Footer Hologram Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x1A00E5FF))
                .border(0.8.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(JarvisCyanBright)
                    )
                    Text(
                        text = "CORE ONLINE",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = JarvisCyanBright
                    )
                }

                Text(
                    text = "READY",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    title: String,
    subTitle: String,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14061224))
            .border(0.6.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(0.5.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                color = textColor
            )
            Text(
                text = subTitle,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF7E97B8)
            )
        }
    }
}

