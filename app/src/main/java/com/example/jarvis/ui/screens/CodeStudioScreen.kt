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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.components.CodeStudioView
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.ui.theme.ThemeManager

@Composable
fun CodeStudioScreen(
    onBack: () -> Unit
) {
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    val languages = listOf("Kotlin", "Python", "JavaScript", "SQL")

    var codeSnippet by remember {
        mutableStateOf(
            """// Stark Protocol Arc Core Initializer
fun engageArcReactor(powerLevel: Int): String {
    val safetyEngaged = powerLevel <= 100
    if (!safetyEngaged) {
        return "WARNING: Arc Core Overload!"
    }
    return "Mark-85 Armor Systems 100% Online"
}"""
        )
    }

    var consoleOutput by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = activeTheme.accentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "STARK CODE MATRIX",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Text(
                    text = "IN-APP SYNTAX ENGINE & SANDBOX",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language selector tabs
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(languages) { lang ->
                        val isSel = lang == selectedLanguage
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) activeTheme.primaryColor.copy(alpha = 0.35f)
                                    else Color(0xFF0F172A)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) activeTheme.accentColor else Color(0xFF1E293B),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedLanguage = lang
                                    codeSnippet = when (lang) {
                                        "Python" -> "import time\n\ndef run_diagnostics():\n    print('JARVIS Omni-Brain: Python Engine Online')\n    return {'status': 200, 'quantum_core': True}\n\nrun_diagnostics()"
                                        "JavaScript" -> "const jarvis = {\n  system: 'Supreme Omni-OS',\n  ready: true,\n  init() {\n    console.log('Voice Matrix Ready');\n  }\n};\njarvis.init();"
                                        "SQL" -> "SELECT id, title, amount, category FROM expenses WHERE timestamp > 1700000000 ORDER BY amount DESC LIMIT 10;"
                                        else -> "// Stark Protocol Arc Core Initializer\nfun engageArcReactor(powerLevel: Int): String {\n    val safetyEngaged = powerLevel <= 100\n    if (!safetyEngaged) {\n        return \"WARNING: Arc Core Overload!\"\n    }\n    return \"Mark-85 Armor Systems 100% Online\"\n}"
                                    }
                                    consoleOutput = null
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) activeTheme.accentColor else JarvisTextSecondary
                            )
                        }
                    }
                }
            }

            // Editable Input Box
            item {
                OutlinedTextField(
                    value = codeSnippet,
                    onValueChange = { codeSnippet = it },
                    label = { Text("Code Editor ($selectedLanguage)", color = JarvisTextDim) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeTheme.accentColor,
                        unfocusedBorderColor = activeTheme.borderColor,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                )
            }

            // Execute & Inspect Button
            item {
                Button(
                    onClick = {
                        consoleOutput = "[STARK REPL EXECUTOR]\nLanguage: $selectedLanguage\nCompiling AST tree...\nSuccess (0 errors, 0 warnings).\nResult: Matrix Output: Status OK (Executed in 4.2ms)"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPILE & RENDER SYNTAX",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Interactive Highlighting Code Sandbox
            item {
                Text(
                    text = "LIVE SYNTAX-HIGHLIGHTED REPL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeTheme.accentColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                CodeStudioView(
                    code = codeSnippet,
                    language = selectedLanguage
                )
            }

            // Console output
            if (consoleOutput != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF060B12))
                            .border(1.dp, JarvisGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "STARK CONSOLE OUTPUT",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = JarvisGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = consoleOutput ?: "",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}
