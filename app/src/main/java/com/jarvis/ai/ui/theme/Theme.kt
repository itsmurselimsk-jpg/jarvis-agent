package com.jarvis.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jarvis.ai.ui.theme.JarvisBackground
import com.jarvis.ai.ui.theme.JarvisBorder
import com.jarvis.ai.ui.theme.JarvisBorderSubtle
import com.jarvis.ai.ui.theme.JarvisCyan
import com.jarvis.ai.ui.theme.JarvisCyanBright
import com.jarvis.ai.ui.theme.JarvisElectricBlue
import com.jarvis.ai.ui.theme.JarvisIceBlue
import com.jarvis.ai.ui.theme.JarvisRed
import com.jarvis.ai.ui.theme.JarvisSurface
import com.jarvis.ai.ui.theme.JarvisSurfaceElevated
import com.jarvis.ai.ui.theme.JarvisTextPrimary
import com.jarvis.ai.ui.theme.JarvisTextSecondary

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF021018),
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = JarvisCyanBright,
    secondary = JarvisElectricBlue,
    onSecondary = Color(0xFF021018),
    secondaryContainer = Color(0xFF00325B),
    onSecondaryContainer = Color(0xFFCCE5FF),
    tertiary = JarvisIceBlue,
    onTertiary = Color(0xFF021018),
    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceElevated,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorder,
    outlineVariant = JarvisBorderSubtle,
    error = JarvisRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}

