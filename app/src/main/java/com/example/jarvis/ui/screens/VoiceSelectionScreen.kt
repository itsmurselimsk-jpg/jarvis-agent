package com.example.jarvis.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.ProviderSettings
import com.example.jarvis.model.VoiceSynthesisEngine
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisSurfaceElevated
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.voice.HumanVoiceEngine
import com.example.jarvis.voice.SupportedLanguage
import com.example.jarvis.voice.VoiceProfileType

@Composable
fun VoiceSelectionScreen(
    currentSettings: ProviderSettings,
    onUpdateSettings: (ProviderSettings) -> Unit,
    onTestSpeak: (String, Float, Float) -> Unit
) {
    val profiles = VoiceProfileType.values().toList()
    var languageDropdownOpen by remember { mutableStateOf(false) }
    var geminiVoiceDropdownOpen by remember { mutableStateOf(false) }
    val isSpeaking by HumanVoiceEngine.isSpeaking.collectAsState()
    val lastVoiceUsed by HumanVoiceEngine.lastVoiceUsed.collectAsState()

    val geminiVoices = listOf(
        Pair("Puck", "British Refined (Paul Bettany style)"),
        Pair("Charon", "Deep Resonant Baritone"),
        Pair("Fenrir", "Assertive Operational Command"),
        Pair("Aoede", "Warm & Melodic Conversational"),
        Pair("Kore", "Crisp Intelligent Synthetic Female")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    text = "HUMAN VOICE & SYNTHESIS CALIBRATION",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Text(
                    text = "Dual-Engine: Gemini Studio Cloud Realism + On-Device Neural WaveNet",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        // Live Voice Engine Status
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D1726))
                    .border(0.5.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) JarvisGreen else JarvisCyan)
                        )
                        Column {
                            Text(
                                text = if (isSpeaking) "ACTIVE SYNTHESIS IN PROGRESS" else "VOICE ENGINE ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSpeaking) JarvisGreen else JarvisCyan
                            )
                            Text(
                                text = lastVoiceUsed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = JarvisTextPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF14243C))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentSettings.voiceSynthesisEngine.title.take(18),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyanBright
                        )
                    }
                }
            }
        }

        // Voice Engine Selector Card
        item {
            Text(
                text = "SYNTHESIS ENGINE ARCHITECTURE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceSynthesisEngine.values().forEach { engine ->
                    val isEngineSelected = currentSettings.voiceSynthesisEngine == engine
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEngineSelected) Color(0xFF0B2138) else Color(0xFF090E1A))
                            .border(
                                if (isEngineSelected) 1.2.dp else 0.5.dp,
                                if (isEngineSelected) JarvisCyan else JarvisBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onUpdateSettings(currentSettings.copy(voiceSynthesisEngine = engine))
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isEngineSelected) JarvisCyan else Color(0xFF14243C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (engine) {
                                            VoiceSynthesisEngine.HYBRID_AUTO -> Icons.Default.AutoAwesome
                                            VoiceSynthesisEngine.GEMINI_STUDIO -> Icons.Default.Cloud
                                            VoiceSynthesisEngine.NEURAL_DEVICE -> Icons.Default.Speed
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isEngineSelected) Color.Black else JarvisCyan
                                    )
                                }
                                Column {
                                    Text(
                                        text = engine.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEngineSelected) JarvisCyanBright else JarvisTextPrimary
                                    )
                                    Text(
                                        text = engine.description,
                                        fontSize = 10.sp,
                                        color = JarvisTextSecondary,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            if (isEngineSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = JarvisCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gemini Voice Persona Picker (when Studio or Hybrid is selected)
        if (currentSettings.voiceSynthesisEngine != VoiceSynthesisEngine.NEURAL_DEVICE) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF090E1A))
                        .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
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
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = JarvisAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Gemini Studio Voice Persona",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisTextPrimary
                                    )
                                    Text(
                                        text = "Ultra-realistic human vocal timbre & breathing",
                                        fontSize = 10.sp,
                                        color = JarvisTextDim
                                    )
                                }
                            }

                            Box {
                                OutlinedButton(
                                    onClick = { geminiVoiceDropdownOpen = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, JarvisAmber),
                                    modifier = Modifier.testTag("gemini_voice_picker_button")
                                ) {
                                    Text(
                                        text = currentSettings.geminiVoiceName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = JarvisAmber
                                    )
                                }

                                DropdownMenu(
                                    expanded = geminiVoiceDropdownOpen,
                                    onDismissRequest = { geminiVoiceDropdownOpen = false }
                                ) {
                                    geminiVoices.forEach { (vName, vDesc) ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(text = vName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(text = vDesc, fontSize = 10.sp, color = JarvisTextSecondary)
                                                }
                                            },
                                            onClick = {
                                                onUpdateSettings(currentSettings.copy(geminiVoiceName = vName))
                                                geminiVoiceDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language Mode Dropdown Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
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
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Multilingual Recognition & Speech",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisTextPrimary
                                )
                                Text(
                                    text = "English, Bengali, Hindi & Hinglish supported",
                                    fontSize = 10.sp,
                                    color = JarvisTextDim
                                )
                            }
                        }

                        Box {
                            val activeLang = SupportedLanguage.fromCode(currentSettings.languageCode)
                            OutlinedButton(
                                onClick = { languageDropdownOpen = true },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, JarvisCyan),
                                modifier = Modifier.testTag("language_selector_button")
                            ) {
                                Text(
                                    text = activeLang.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JarvisCyan
                                )
                            }

                            DropdownMenu(
                                expanded = languageDropdownOpen,
                                onDismissRequest = { languageDropdownOpen = false }
                            ) {
                                SupportedLanguage.values().forEach { lang ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${lang.displayName} (${lang.nativeName})",
                                                fontSize = 12.sp
                                            )
                                        },
                                        onClick = {
                                            onUpdateSettings(currentSettings.copy(languageCode = lang.code))
                                            languageDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Profiles Section
        item {
            Text(
                text = "CALIBRATED VOCAL PERSONAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary
            )
        }

        items(profiles) { profile ->
            val isSelected = currentSettings.voiceProfileName == profile.profileName
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color(0xFF0A1E30) else Color(0xFF090E1A))
                    .border(
                        if (isSelected) 1.dp else 0.5.dp,
                        if (isSelected) JarvisCyan else JarvisBorderSubtle,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onUpdateSettings(
                            currentSettings.copy(
                                voiceProfileName = profile.profileName,
                                speechPitch = profile.defaultPitch,
                                speechRate = profile.defaultSpeed,
                                geminiVoiceName = profile.geminiVoiceName
                            )
                        )
                    }
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) JarvisCyan else Color(0xFF14223A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) Color.Black else JarvisCyan
                                )
                            }
                            Column {
                                Text(
                                    text = profile.profileName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) JarvisCyanBright else JarvisTextPrimary
                                )
                                Text(
                                    text = profile.tagline,
                                    fontSize = 10.sp,
                                    color = JarvisTextDim
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = JarvisCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = profile.description,
                        fontSize = 11.sp,
                        color = JarvisTextSecondary,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speed: ${profile.defaultSpeed}x  •  Pitch: ${profile.defaultPitch}x",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextDim
                        )

                        Button(
                            onClick = {
                                onUpdateSettings(
                                    currentSettings.copy(
                                        voiceProfileName = profile.profileName,
                                        speechPitch = profile.defaultPitch,
                                        speechRate = profile.defaultSpeed,
                                        geminiVoiceName = profile.geminiVoiceName
                                    )
                                )
                                val testPhrase = when (SupportedLanguage.fromCode(currentSettings.languageCode)) {
                                    SupportedLanguage.BENGALI -> "আমি জারভিস। সব সিস্টেম স্বাভাবিক এবং কার্যকর।"
                                    SupportedLanguage.HINDI -> "नमस्ते सर, मैं जार्विस हूँ। सभी सिस्टम सामान्य और चालू हैं।"
                                    SupportedLanguage.HINGLISH -> "Haan Sir, main JARVIS hoon. Sabhi systems online hain."
                                    else -> profile.previewPhrase
                                }
                                onTestSpeak(
                                    testPhrase,
                                    profile.defaultSpeed,
                                    profile.defaultPitch
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) JarvisCyan else Color(0xFF1B2A47)
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) Color.Black else JarvisCyan
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else JarvisCyan
                            )
                        }
                    }
                }
            }
        }

        // Custom Fine Tuning
        item {
            Text(
                text = "ACOUSTIC CADENCE & MODULATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextSecondary
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090E1A))
                    .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Speech Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Speech Cadence (Speed)", fontSize = 12.sp, color = JarvisTextPrimary)
                            Text(
                                "${String.format("%.2f", currentSettings.speechRate)}x",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyan
                            )
                        }
                        Slider(
                            value = currentSettings.speechRate,
                            onValueChange = { onUpdateSettings(currentSettings.copy(speechRate = it)) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = JarvisCyan,
                                activeTrackColor = JarvisCyan,
                                inactiveTrackColor = Color(0xFF14243B)
                            )
                        )
                    }

                    // Speech Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Modulation (Pitch)", fontSize = 12.sp, color = JarvisTextPrimary)
                            Text(
                                "${String.format("%.2f", currentSettings.speechPitch)}x",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisCyan
                            )
                        }
                        Slider(
                            value = currentSettings.speechPitch,
                            onValueChange = { onUpdateSettings(currentSettings.copy(speechPitch = it)) },
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = JarvisCyan,
                                activeTrackColor = JarvisCyan,
                                inactiveTrackColor = Color(0xFF14243B)
                            )
                        )
                    }
                }
            }
        }
    }
}

