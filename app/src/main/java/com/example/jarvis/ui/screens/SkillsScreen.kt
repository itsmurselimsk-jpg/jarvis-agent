package com.example.jarvis.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class JarvisSkillItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val statusText: String = "ACTIVE // READY",
    val action: () -> Unit
)

@Composable
fun SkillsScreen(
    onOpenSearch: () -> Unit,
    onOpenVision: () -> Unit,
    onOpenCodeStudio: () -> Unit,
    onOpenVoiceNotes: () -> Unit,
    onOpenTimers: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenArmorThemes: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAutomation: () -> Unit,
    onOpenMemory: () -> Unit,
    onVoiceCommand: (String) -> Unit
) {
    val skills = listOf(
        JarvisSkillItem(
            id = "search",
            name = "Web Intelligence",
            description = "Real-time web search, facts, wikipedia & knowledge lookup",
            icon = Icons.Default.Public,
            color = JarvisCyanBright,
            action = onOpenSearch
        ),
        JarvisSkillItem(
            id = "vision",
            name = "Vision AI & OCR",
            description = "Neural image inspection, document scanning & object ID",
            icon = Icons.Default.CameraAlt,
            color = JarvisCyan,
            action = onOpenVision
        ),
        JarvisSkillItem(
            id = "code",
            name = "Code Studio REPL",
            description = "Interactive Kotlin & Python programming workbench",
            icon = Icons.Default.Code,
            color = JarvisElectricBlue,
            action = onOpenCodeStudio
        ),
        JarvisSkillItem(
            id = "voice_vault",
            name = "Voice Audio Vault",
            description = "Direct AAC vocal recorder & encrypted memo store",
            icon = Icons.Default.GraphicEq,
            color = JarvisGreen,
            action = onOpenVoiceNotes
        ),
        JarvisSkillItem(
            id = "chronometer",
            name = "Arc Chronometer",
            description = "Tactical countdown timers, multi-lap stopwatches & strobe",
            icon = Icons.Default.Timer,
            color = JarvisAmber,
            action = onOpenTimers
        ),
        JarvisSkillItem(
            id = "tasks",
            name = "Reminders & Agendas",
            description = "Executive priority checklist & deadline synchronizer",
            icon = Icons.Default.AutoMode,
            color = JarvisPurpleHighlight,
            action = onOpenTasks
        ),
        JarvisSkillItem(
            id = "weather",
            name = "Weather & Atmosphere",
            description = "Meteorological forecast & environmental conditions",
            icon = Icons.Default.WbSunny,
            color = Color(0xFFFFD700),
            action = { onVoiceCommand("What's the current weather?") }
        ),
        JarvisSkillItem(
            id = "music",
            name = "Music & Media Control",
            description = "Audio playback orchestration & track modulation",
            icon = Icons.Default.MusicNote,
            color = Color(0xFFFF4081),
            action = { onVoiceCommand("Play music") }
        ),
        JarvisSkillItem(
            id = "expenses",
            name = "Financial Vault",
            description = "Cash flow ledger, expense categorization & analytics",
            icon = Icons.Default.AccountBalanceWallet,
            color = JarvisCyanBright,
            action = onOpenExpenses
        ),
        JarvisSkillItem(
            id = "habits",
            name = "Habit & Discipline",
            description = "Streak counter, daily milestones & routine locking",
            icon = Icons.Default.LocalFireDepartment,
            color = JarvisAmber,
            action = onOpenHabits
        ),
        JarvisSkillItem(
            id = "memory",
            name = "Neural Memory Bank",
            description = "Persistent semantic context & knowledge memory",
            icon = Icons.Default.Psychology,
            color = JarvisCyan,
            action = onOpenMemory
        ),
        JarvisSkillItem(
            id = "privacy",
            name = "Privacy & Security Shield",
            description = "Permission auditing, data sanitization & safety guardrails",
            icon = Icons.Default.Shield,
            color = JarvisGreen,
            action = onOpenPrivacy
        ),
        JarvisSkillItem(
            id = "armor",
            name = "Mark Armor Matrix",
            description = "Dynamic holographic HUD palettes & energy themes",
            icon = Icons.Default.Palette,
            color = JarvisPurpleHighlight,
            action = onOpenArmorThemes
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("skills_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    text = "SKILLS // PROTOCOL MATRIX",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = JarvisCyanBright
                )
                Text(
                    text = "Autonomous executive modules & intelligent sub-routines",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        items(skills) { skill ->
            SkillGridCard(skill = skill)
        }
    }
}

@Composable
private fun SkillGridCard(skill: JarvisSkillItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xDE081124),
                        Color(0xF2040914)
                    )
                )
            )
            .border(1.dp, skill.color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { skill.action() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(skill.color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = skill.icon,
                        contentDescription = skill.name,
                        tint = skill.color,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(skill.color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "READY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = skill.color
                    )
                }
            }

            Text(
                text = skill.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisTextPrimary,
                lineHeight = 15.sp
            )

            Text(
                text = skill.description,
                fontSize = 10.sp,
                color = JarvisTextSecondary,
                lineHeight = 13.sp,
                maxLines = 2
            )
        }
    }
}
