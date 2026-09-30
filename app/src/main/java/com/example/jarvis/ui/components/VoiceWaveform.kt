package com.example.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue

@Composable
fun VoiceWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    height: Dp = 48.dp,
    isActive: Boolean = true,
    rmsDb: Float = 0f,
    accentColor: Color = JarvisCyan
) {
    val transition = rememberInfiniteTransition(label = "waveform_anim")

    // Generate staggered heights for high-tech audio spectrum look
    val multipliers = listOf(0.3f, 0.6f, 0.9f, 0.5f, 0.8f, 1.0f, 0.7f, 0.4f, 0.85f, 0.65f, 0.95f, 0.5f, 0.75f, 0.35f, 0.6f, 0.4f)
    val rmsScale = (rmsDb.coerceIn(0f, 15f) / 15f) * 0.5f

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val mult = (multipliers[i % multipliers.size] + rmsScale).coerceAtMost(1f)
            val duration = 400 + (i * 45)

            val animatedFraction by transition.animateFloat(
                initialValue = if (isActive) 0.15f + (rmsScale * 0.3f) else 0.08f,
                targetValue = if (isActive) mult else 0.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(fraction = animatedFraction.coerceIn(0.08f, 1.0f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i % 2 == 0) accentColor else JarvisCyanBright
                    )
            )
        }
    }
}
