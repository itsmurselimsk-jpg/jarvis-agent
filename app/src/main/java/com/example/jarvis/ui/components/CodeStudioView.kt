package com.example.jarvis.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.theme.ThemeManager

/**
 * Interactive In-App Code Sandbox Runner & Syntax Viewer.
 * Features line numbers, language keyword coloring, and 1-tap clipboard copy.
 */
@Composable
fun CodeStudioView(
    code: String,
    language: String = "Kotlin",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val armorTheme by ThemeManager.currentTheme.collectAsState()
    val lines = code.lines()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, armorTheme.borderColor, RoundedCornerShape(12.dp))
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Code",
                    tint = armorTheme.accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = language.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = armorTheme.accentColor
                )
            }

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("JARVIS Code", code)
                    clipboard?.setPrimaryClip(clip)
                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Code Body with Line Numbers & Syntax Highlighting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            // Line numbers column
            Column(
                modifier = Modifier.padding(end = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.indices.forEach { index ->
                    Text(
                        text = "${index + 1}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }

            // Code text column
            Column {
                lines.forEach { line ->
                    Text(
                        text = highlightSyntax(line, language),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private fun highlightSyntax(line: String, language: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val keywords = setOf(
            "fun", "val", "var", "class", "object", "interface", "import", "package",
            "return", "if", "else", "when", "for", "while", "override", "suspend",
            "private", "public", "def", "import", "from", "as", "async", "await",
            "function", "const", "let", "SELECT", "FROM", "WHERE", "INSERT", "UPDATE"
        )
        val tokens = line.split(Regex("(?<=[ ,.()={}\\[\\]])|(?=[ ,.()={}\\[\\]])"))

        for (token in tokens) {
            when {
                keywords.contains(token) -> {
                    withStyle(SpanStyle(color = Color(0xFFFF79C6), fontWeight = FontWeight.Bold)) {
                        append(token)
                    }
                }
                token.startsWith("\"") && token.endsWith("\"") -> {
                    withStyle(SpanStyle(color = Color(0xFFF1FA8C))) {
                        append(token)
                    }
                }
                token.startsWith("//") || token.startsWith("#") -> {
                    withStyle(SpanStyle(color = Color(0xFF6272A4))) {
                        append(token)
                    }
                }
                token.toIntOrNull() != null || token.toDoubleOrNull() != null -> {
                    withStyle(SpanStyle(color = Color(0xFFBD93F9))) {
                        append(token)
                    }
                }
                token == "{" || token == "}" || token == "(" || token == ")" -> {
                    withStyle(SpanStyle(color = Color(0xFF50FA7B))) {
                        append(token)
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = Color(0xFFF8F8F2))) {
                        append(token)
                    }
                }
            }
        }
    }
}
