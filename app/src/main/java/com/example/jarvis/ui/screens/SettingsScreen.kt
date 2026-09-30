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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.AIProviderType
import com.example.jarvis.model.ProviderSettings
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
import com.example.jarvis.ui.theme.MarkArmorTheme
import com.example.jarvis.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    currentSettings: ProviderSettings,
    onSaveSettings: (ProviderSettings) -> Unit,
    isDarkTheme: Boolean = true,
    selectedAccentColor: Color = JarvisCyan,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onSelectAccentColor: (Color) -> Unit = {},
    onNavigatePrivacy: () -> Unit = {},
    onNavigateMemory: () -> Unit = {},
    onNavigateBridge: () -> Unit = {},
    onNavigateActivity: () -> Unit = {},
    onNavigateVision: () -> Unit = {},
    onNavigateVoiceSetup: () -> Unit = {},
    onNavigateVoiceProfiles: () -> Unit = {},
    onNavigateAbout: () -> Unit = {},
    onNavigateDiagnostics: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onNavigateSearch: () -> Unit = {},
    onNavigatePlugins: () -> Unit = {},
    onNavigateBackup: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeArmorTheme by ThemeManager.currentTheme.collectAsState()

    var speechRate by remember(currentSettings.speechRate) { mutableFloatStateOf(currentSettings.speechRate) }
    var speechPitch by remember(currentSettings.speechPitch) { mutableFloatStateOf(currentSettings.speechPitch) }
    var continuousWakeEnabled by remember(currentSettings.continuousWakeEnabled) { mutableStateOf(currentSettings.continuousWakeEnabled) }
    var selectedProfile by remember(currentSettings.voiceProfileName) { mutableStateOf(currentSettings.voiceProfileName) }
    var customApiKey by remember(currentSettings.customApiKey) { mutableStateOf(currentSettings.customApiKey) }
    var hologramIntensity by remember { mutableFloatStateOf(1.0f) }

    val personaModes = listOf("JARVIS Natural", "FRIDAY Smart", "EDITH Tactical", "STARK Prime")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SETTINGS // SYSTEM CONSOLE",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = JarvisCyanBright
                    )
                    Text(
                        text = "Core parameters, neural configuration & holographic tuning",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3300E5FF))
                        .border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v2.0.0 STARK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisCyanBright
                    )
                }
            }
        }

        // 1. VOICE SYNTHESIS & ACOUSTICS SECTION
        item {
            SettingsCard(title = "VOICE // ACOUSTIC ENGINE") {
                // Speech Rate
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate (Cadence)", fontSize = 11.sp, color = JarvisTextSecondary)
                        Text("${String.format(java.util.Locale.US, "%.2f", speechRate)}x", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            onSaveSettings(currentSettings.copy(speechRate = it))
                        },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = JarvisCyanBright, activeTrackColor = JarvisCyan)
                    )
                }

                // Speech Pitch
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vocal Pitch (Tone)", fontSize = 11.sp, color = JarvisTextSecondary)
                        Text("${String.format(java.util.Locale.US, "%.2f", speechPitch)}x", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisElectricBlue)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            onSaveSettings(currentSettings.copy(speechPitch = it))
                        },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(thumbColor = JarvisElectricBlue, activeTrackColor = JarvisElectricBlue)
                    )
                }

                // Voice Profile Quick Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateVoiceProfiles,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Voice Profiles", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisCyanBright)
                    }
                }
            }
        }

        // 2. AI INTELLIGENCE & PERSONALITY
        item {
            SettingsCard(title = "AI INTELLIGENCE // PERSONALITY") {
                Text("Select Assistant Persona Mode:", fontSize = 11.sp, color = JarvisTextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    personaModes.forEach { modeName ->
                        val isSel = modeName == selectedProfile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0x4400E5FF) else Color(0x220A1424))
                                .border(1.dp, if (isSel) JarvisCyanBright else JarvisBorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedProfile = modeName
                                    onSaveSettings(currentSettings.copy(voiceProfileName = modeName))
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeName.substringBefore(" "),
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSel) JarvisCyanBright else JarvisTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom Gemini API Key Field
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = {
                        customApiKey = it
                        onSaveSettings(currentSettings.copy(customApiKey = it))
                    },
                    label = { Text("Custom Gemini API Key (Optional)", fontSize = 11.sp, color = JarvisTextDim) },
                    placeholder = { Text("AIzaSy...", fontSize = 11.sp, color = JarvisTextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorderSubtle,
                        focusedContainerColor = Color(0x44060E1C),
                        unfocusedContainerColor = Color(0x44060E1C),
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }
        }

        // 3. WAKE-WORD & VOICE RECOGNITION
        item {
            SettingsCard(title = "WAKE WORD // BACKGROUND LISTENER") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake Phrase Detection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = JarvisTextPrimary)
                        Text("Say 'Hey JARVIS' to activate audio pipeline", fontSize = 10.sp, color = JarvisTextSecondary)
                    }

                    Switch(
                        checked = continuousWakeEnabled,
                        onCheckedChange = {
                            continuousWakeEnabled = it
                            onSaveSettings(currentSettings.copy(continuousWakeEnabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = JarvisCyanBright,
                            uncheckedThumbColor = JarvisTextDim,
                            uncheckedTrackColor = Color(0xFF131E30)
                        )
                    )
                }

                Button(
                    onClick = onNavigateVoiceSetup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calibrate Wake Word Matrix", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisCyanBright)
                }
            }
        }

        // 4. APPEARANCE & HOLOGRAPHIC THEMES
        item {
            SettingsCard(title = "APPEARANCE // MARK ARMOR MATRIX") {
                Text("Select Hologram Armor Palette:", fontSize = 11.sp, color = JarvisTextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarkArmorTheme.entries.take(3).forEach { theme ->
                        val isSel = theme == activeArmorTheme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) theme.primaryColor.copy(alpha = 0.35f) else Color(0x220A1424))
                                .border(1.dp, if (isSel) theme.accentColor else JarvisBorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { ThemeManager.setTheme(theme, context) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSel) theme.accentColor else JarvisTextSecondary
                            )
                        }
                    }
                }

                // Hologram Glow Intensity
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hologram Glow Intensity", fontSize = 11.sp, color = JarvisTextSecondary)
                        Text("${(hologramIntensity * 100).toInt()}%", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                    }
                    Slider(
                        value = hologramIntensity,
                        onValueChange = { hologramIntensity = it },
                        valueRange = 0.4f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = JarvisCyanBright, activeTrackColor = JarvisCyan)
                    )
                }
            }
        }

        // 5. PRIVACY & SECURITY SHIELD
        item {
            SettingsCard(title = "PRIVACY & SECURITY SHIELD") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigatePrivacy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E676)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = JarvisGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privacy Audit", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisGreen)
                    }

                    Button(
                        onClick = onNavigateMemory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memory Bank", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisCyanBright)
                    }
                }
            }
        }

        // 6. SYSTEM & DIAGNOSTICS
        item {
            SettingsCard(title = "SYSTEM & DIAGNOSTICS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateDiagnostics,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300B0FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = JarvisElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hardware Health", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisElectricBlue)
                    }

                    Button(
                        onClick = onNavigateBackup,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFAB00)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = JarvisAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Data Backup", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisAmber)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF1744)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = JarvisRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("De-authorize Session // Log Out", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisRed)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xDE081124),
                        Color(0xF2040914)
                    )
                )
            )
            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            color = JarvisCyan
        )
        content()
    }
}
