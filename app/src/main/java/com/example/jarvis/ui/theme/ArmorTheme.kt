package com.example.jarvis.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MarkArmorTheme(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val accentColor: Color,
    val glowColor: Color,
    val surfaceColor: Color,
    val surfaceElevated: Color,
    val borderColor: Color
) {
    STEALTH_SHADOW(
        title = "STEALTH SHADOW",
        subtitle = "Pitch OLED Black & Electric Neon Cyan",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF80F3FF),
        glowColor = Color(0xFF00B0FF),
        surfaceColor = Color(0xFF0A0F1D),
        surfaceElevated = Color(0xFF10172A),
        borderColor = Color(0x3300E5FF)
    ),
    MARK_85(
        title = "MARK 85 CLASSIC",
        subtitle = "Iron Man Crimson Red, Gold & Cyan Core",
        primaryColor = Color(0xFFFF2A4B),
        accentColor = Color(0xFFFFD700),
        glowColor = Color(0xFF00E5FF),
        surfaceColor = Color(0xFF1A0A0E),
        surfaceElevated = Color(0xFF281119),
        borderColor = Color(0x4DFF2A4B)
    ),
    WAR_MACHINE(
        title = "WAR MACHINE",
        subtitle = "Titanium Gunmetal & Tactical Laser Red",
        primaryColor = Color(0xFFFF1744),
        accentColor = Color(0xFFE2E8F0),
        glowColor = Color(0xFFFF5252),
        surfaceColor = Color(0xFF0F1218),
        surfaceElevated = Color(0xFF181C26),
        borderColor = Color(0x40FF1744)
    ),
    WAKANDA_VIBRANIUM(
        title = "WAKANDA VIBRANIUM",
        subtitle = "Deep Obsidian Purple & Plasma Blue",
        primaryColor = Color(0xFFD500F9),
        accentColor = Color(0xFF00E5FF),
        glowColor = Color(0xFF7C4DFF),
        surfaceColor = Color(0xFF110A1C),
        surfaceElevated = Color(0xFF1C112C),
        borderColor = Color(0x40D500F9)
    )
}

object ThemeManager {
    private val _currentTheme = MutableStateFlow(MarkArmorTheme.STEALTH_SHADOW)
    val currentTheme: StateFlow<MarkArmorTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("jarvis_theme_prefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("armor_theme_name", MarkArmorTheme.STEALTH_SHADOW.name)
        val matched = MarkArmorTheme.entries.find { it.name == savedName } ?: MarkArmorTheme.STEALTH_SHADOW
        _currentTheme.value = matched
    }

    fun setTheme(theme: MarkArmorTheme, context: Context? = null) {
        _currentTheme.value = theme
        context?.let {
            val prefs = it.getSharedPreferences("jarvis_theme_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("armor_theme_name", theme.name).apply()
        }
    }
}
