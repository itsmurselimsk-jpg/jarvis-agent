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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.brain.Tool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.ui.JarvisViewModel
import com.example.jarvis.ui.SubScreen
import com.example.jarvis.ui.components.NavTab
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette

data class ToolCategoryNav(
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@Composable
fun ToolsScreen(
    tools: List<Tool>,
    toolContext: ToolContext,
    onOpenSubScreen: (SubScreen) -> Unit = {},
    onOpenTab: (NavTab) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    val subNavItems = listOf(
        ToolCategoryNav("Armor Themes", Icons.Default.Palette) { onOpenSubScreen(SubScreen.ARMOR_THEMES) },
        ToolCategoryNav("Expense Vault", Icons.Default.AccountBalanceWallet) { onOpenSubScreen(SubScreen.EXPENSES) },
        ToolCategoryNav("Habit Tracker", Icons.Default.LocalFireDepartment) { onOpenSubScreen(SubScreen.HABITS) },
        ToolCategoryNav("Code REPL", Icons.Default.Code) { onOpenSubScreen(SubScreen.CODE_STUDIO) },
        ToolCategoryNav("Conversation", Icons.Default.ChatBubble) { onOpenTab(NavTab.CONVERSATION) },
        ToolCategoryNav("Core Hub", Icons.Default.GraphicEq) { onOpenTab(NavTab.HOME) },
        ToolCategoryNav("Memory", Icons.Default.Psychology) { onOpenSubScreen(SubScreen.MEMORY) },
        ToolCategoryNav("Research", Icons.Default.Search) { onOpenSubScreen(SubScreen.SEARCH) },
        ToolCategoryNav("Files", Icons.Default.Folder) { onOpenSubScreen(SubScreen.FILES) },
        ToolCategoryNav("Automation", Icons.Default.AutoMode) { onOpenSubScreen(SubScreen.AUTOMATION) },
        ToolCategoryNav("Plugins", Icons.Default.Extension) { onOpenSubScreen(SubScreen.PLUGINS) },
        ToolCategoryNav("Privacy", Icons.Default.Shield) { onOpenSubScreen(SubScreen.PRIVACY) },
        ToolCategoryNav("Settings", Icons.Default.Settings) { onOpenTab(NavTab.SETTINGS) }
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Title
        item {
            Column {
                Text(
                    text = "TOOL CENTER & EXECUTOR MATRIX",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    color = JarvisCyanBright
                )
                Text(
                    text = "${tools.size} registered executive capabilities across system & AI domains",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        // 2. Hub Quick Access Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SYSTEM ACCESS HUB",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(subNavItems) { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xEE0D162A))
                                .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { item.action() }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = JarvisCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = JarvisTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Registered Tool Capability Cards
        items(tools, key = { it.name }) { tool ->
            ToolCard(
                tool = tool,
                onExecute = { input, onResult ->
                    coroutineScope.launch {
                        val res = tool.execute(input, toolContext)
                        onResult(res.output)
                    }
                }
            )
        }
    }
}

@Composable
private fun ToolCard(
    tool: Tool,
    onExecute: (String, (String) -> Unit) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var outputResult by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xEE0B1222))
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisCyan.copy(alpha = 0.15f))
                            .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = tool.name,
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = tool.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextPrimary
                        )
                        Text(
                            text = tool.description,
                            fontSize = 11.sp,
                            color = JarvisTextSecondary
                        )
                    }
                }

                // Risk pill
                val (riskColor, riskText) = when (tool.riskLevel) {
                    RiskLevel.SAFE -> JarvisGreen to "SAFE"
                    RiskLevel.CONFIRMATION -> JarvisAmber to "CONFIRM"
                    RiskLevel.RESTRICTED -> JarvisRed to "RESTRICTED"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(riskColor.copy(alpha = 0.15f))
                        .border(0.5.dp, riskColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = riskText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = riskColor
                    )
                }
            }

            // Execution Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = {
                        Text("Parameter input...", fontSize = 11.sp, color = JarvisTextDim)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tool_input_${tool.name}"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderSubtle,
                        focusedContainerColor = Color(0xFF070B14),
                        unfocusedContainerColor = Color(0xFF070B14),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        isRunning = true
                        onExecute(input) { result ->
                            outputResult = result
                            isRunning = false
                        }
                    },
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("tool_exec_${tool.name}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Execution Output
            outputResult?.let { out ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF050811))
                        .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = out,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyanBright
                    )
                }
            }
        }
    }
}
