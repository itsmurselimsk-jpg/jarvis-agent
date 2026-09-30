package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// JARVIS Deep Space & Cyber Palette
val JarvisBackground = Color(0xFF04070F)
val JarvisSurface = Color(0xFF090E1A)
val JarvisSurfaceElevated = Color(0xFF101828)
val JarvisSurfaceGlass = Color(0x990E1626)
val JarvisBorder = Color(0x3300E5FF)
val JarvisBorderSubtle = Color(0x1A00E5FF)

// Cyan & Blue Futuristic Glows
val JarvisCyan = Color(0xFF00E5FF)
val JarvisCyanBright = Color(0xFF80F3FF)
val JarvisCyanDim = Color(0xFF0091A3)
val JarvisElectricBlue = Color(0xFF00B0FF)
val JarvisIceBlue = Color(0xFFE0F7FA)

// Functional & State Accents
val JarvisGreen = Color(0xFF00E676)
val JarvisAmber = Color(0xFFFFAB00)
val JarvisRed = Color(0xFFFF1744)
val JarvisPurple = Color(0xFFD500F9)

// Text
val JarvisTextPrimary = Color(0xFFF1F5F9)
val JarvisTextSecondary = Color(0xFF94A3B8)
val JarvisTextDim = Color(0xFF475569)

// Gradients
val JarvisOrbGlowBrush = Brush.radialGradient(
    colors = listOf(
        JarvisCyanBright.copy(alpha = 0.8f),
        JarvisCyan.copy(alpha = 0.4f),
        JarvisElectricBlue.copy(alpha = 0.15f),
        Color.Transparent
    )
)

val JarvisPanelBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xCC0D1526),
        Color(0xEE060A14)
    )
)
