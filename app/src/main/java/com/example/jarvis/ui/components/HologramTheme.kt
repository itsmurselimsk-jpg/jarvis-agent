package com.example.jarvis.ui.components

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holographic Color Matrix Themes for J.A.R.V.I.S.
 * Enables dynamic switching between classic Stark blue, Combat crimson, Arc Gold,
 * Quantum emerald, and Stealth violet cybernetic themes.
 */
enum class HologramTheme(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val description: String
) {
    JARVIS_CLASSIC(
        id = "jarvis",
        displayName = "J.A.R.V.I.S. Standard",
        primary = Color(0xFF00E5FF),      // Cyber Cyan
        secondary = Color(0xFF2979FF),    // Electric Blue
        accent = Color(0xFF00B0FF),       // Bright Azure
        description = "Mark 50 Neural Interface (Classic Azure/Cyan)"
    ),
    JARVIS_CRIMSON(
        id = "crimson",
        displayName = "J.A.R.V.I.S. Combat Crimson",
        primary = Color(0xFFFF1744),      // Menacing Crimson
        secondary = Color(0xFFFF9100),    // Molten Amber
        accent = Color(0xFFD50000),       // Deep Core Scarlet
        description = "J.A.R.V.I.S. Combat Protocol (High-Intensity Crimson & Amber)"
    ),
    ARC_GOLD(
        id = "arc_gold",
        displayName = "Arc Reactor Mark 85",
        primary = Color(0xFFFFD700),      // Pure Titanium Gold
        secondary = Color(0xFFFFFFFF),    // Superheated White Core
        accent = Color(0xFFFFA000),       // Deep Gold
        description = "Nanotech Infused Titanium Core (Gold & Bright White)"
    ),
    QUANTUM_EMERALD(
        id = "quantum",
        displayName = "Quantum Matrix",
        primary = Color(0xFF00E676),      // Cyber Green
        secondary = Color(0xFF1DE9B6),    // Teal Pulse
        accent = Color(0xFF00C853),       // Deep Matrix Emerald
        description = "Quantum Encryption Channel (Emerald & Teal)"
    ),
    STEALTH_VIOLET(
        id = "stealth",
        displayName = "Stealth Dark Matter",
        primary = Color(0xFFD500F9),      // Dark Matter Purple
        secondary = Color(0xFF3D5AFE),    // Deep Indigo
        accent = Color(0xFFAA00FF),       // Neon Violet
        description = "Low-Observable Electronic Warfare (Neon Violet & Indigo)"
    );

    companion object {
        fun fromString(value: String): HologramTheme {
            val lower = value.lowercase().trim()
            return when {
                lower.contains("crimson") || lower.contains("red") || lower.contains("combat") || lower.contains("attack") -> JARVIS_CRIMSON
                lower.contains("gold") || lower.contains("arc") || lower.contains("reactor") || lower.contains("mark 85") || lower.contains("iron man") -> ARC_GOLD
                lower.contains("green") || lower.contains("quantum") || lower.contains("emerald") || lower.contains("matrix") -> QUANTUM_EMERALD
                lower.contains("purple") || lower.contains("violet") || lower.contains("stealth") || lower.contains("indigo") -> STEALTH_VIOLET
                else -> JARVIS_CLASSIC
            }
        }
    }
}

/**
 * Singleton state manager for reactive holographic core theme switching.
 */
object HologramThemeManager {
    private val _currentTheme = MutableStateFlow(HologramTheme.JARVIS_CLASSIC)
    val currentTheme: StateFlow<HologramTheme> = _currentTheme.asStateFlow()

    fun setTheme(theme: HologramTheme) {
        _currentTheme.value = theme
    }

    fun setThemeByName(name: String): HologramTheme {
        val theme = HologramTheme.fromString(name)
        _currentTheme.value = theme
        return theme
    }

    fun getActiveTheme(): HologramTheme = _currentTheme.value
}
