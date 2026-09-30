package com.example.jarvis.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// JARVIS Deep Space & Cyber Palette
val JarvisBackground = Color(0xFF050811)
val JarvisSurface = Color(0xFF0A0F1D)
val JarvisSurfaceElevated = Color(0xFF10172A)
val JarvisSurfaceGlass = Color(0xCC0C1425)
val JarvisGlassBorder = Color(0x2B00E5FF)
val JarvisBorder = Color(0x3300E5FF)
val JarvisBorderSubtle = Color(0x1A00E5FF)

// Cyan, Electric Blue & Subtle Purple Futuristic Glows
val JarvisCyan = Color(0xFF00E5FF)
val JarvisCyanBright = Color(0xFF80F3FF)
val JarvisCyanDim = Color(0xFF0091A3)
val JarvisElectricBlue = Color(0xFF00B0FF)
val JarvisIceBlue = Color(0xFFE0F7FA)

// Purple Glow Highlights
val JarvisPurple = Color(0xFFD500F9)
val JarvisPurpleHighlight = Color(0xFF7C4DFF)
val JarvisPurpleSubtle = Color(0x409C27B0)

// Functional & State Accents
val JarvisGreen = Color(0xFF00E676)
val JarvisAmber = Color(0xFFFFAB00)
val JarvisRed = Color(0xFFFF1744)

// Text
val JarvisTextPrimary = Color(0xFFF1F5F9)
val JarvisTextSecondary = Color(0xFF94A3B8)
val JarvisTextDim = Color(0xFF475569)

// Gradients
val JarvisOrbGlowBrush = Brush.radialGradient(
    colors = listOf(
        JarvisCyanBright.copy(alpha = 0.85f),
        JarvisCyan.copy(alpha = 0.50f),
        JarvisPurpleHighlight.copy(alpha = 0.25f),
        JarvisElectricBlue.copy(alpha = 0.10f),
        Color.Transparent
    )
)

val JarvisPanelBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xDC0C1425),
        Color(0xF0060A14)
    )
)

val JarvisGlassCardBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xEE0E182D),
        Color(0xCC080D1A)
    )
)

