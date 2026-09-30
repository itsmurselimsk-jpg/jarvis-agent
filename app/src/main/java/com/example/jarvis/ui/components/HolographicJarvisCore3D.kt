package com.example.jarvis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jarvis.model.JarvisState
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Holographic JARVIS Core.
 * Premium floating 3D holographic sphere and gyroscopic astrolabe.
 * Features:
 * - Transparent glass-like holographic core with volumetric radial glow
 * - Cyan / electric-blue glowing edges with subtle purple accents
 * - Multiple rotating 3D inclined holographic rings with depth parallax
 * - Front/back depth layering (foreground rings bright, background rings dimmed)
 * - Orbiting floating holographic particles with depth scaling
 * - Audio-reactive pulsation during Listening (driven by voice RMS)
 * - Rapid multi-axis gyroscopic processing during Thinking
 * - Dynamic harmonic waveforms and radiating rings during Speaking
 */
@Composable
fun HolographicJarvisCore3D(
    modifier: Modifier = Modifier,
    size: Dp = 270.dp,
    state: JarvisState = JarvisState.IDLE,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    isThinking: Boolean = false,
    audioRmsDb: Float = 0f,
    onClick: () -> Unit = {}
) {
    val effectiveListening = isListening || state == JarvisState.LISTENING
    val effectiveSpeaking = isSpeaking || state == JarvisState.SPEAKING
    val effectiveThinking = isThinking || state == JarvisState.THINKING

    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_3d_holo_loop")

    // 1. Smooth Floating Translation (Vertical Levitation)
    val floatOffsetProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floating_bob"
    )
    val floatOffsetDp = (sin(floatOffsetProgress) * 7f).dp

    // 2. Multidimensional Ring Rotation Speeds
    val primaryRotationSpeed = when {
        effectiveThinking -> 2400
        effectiveSpeaking -> 4200
        effectiveListening -> 5500
        else -> 11000
    }

    val rotationAngleOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = primaryRotationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_outer"
    )

    val rotationAngleInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (primaryRotationSpeed * 1.4f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_inner"
    )

    val gyroPitchAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (primaryRotationSpeed * 1.8f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gyro_pitch"
    )

    // 3. Smooth Breathing & Reactive Audio Scale
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (effectiveThinking) 600 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breathing"
    )

    // Audio reactive boost during listening or speaking
    val normalizedRms = (audioRmsDb.coerceIn(0f, 60f) / 60f)
    val audioBoost by animateFloatAsState(
        targetValue = if (effectiveListening) {
            1f + (normalizedRms * 0.35f)
        } else if (effectiveSpeaking) {
            1f + (normalizedRms * 0.22f)
        } else {
            1f
        },
        animationSpec = spring(stiffness = 800f),
        label = "audio_reactive_scale"
    )

    // Speaking wave expansion progression
    val waveRadiateProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (effectiveSpeaking) 900 else 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_radiation"
    )

    // Core Colors
    val primaryCyan = JarvisCyanBright
    val electricBlue = JarvisElectricBlue
    val purpleAccent = Color(0xFFA855F7)
    val deepPurple = Color(0xFF6B21A8)

    val coreGlowColor by animateColorAsState(
        targetValue = when {
            effectiveThinking -> purpleAccent
            effectiveSpeaking -> electricBlue
            effectiveListening -> if (normalizedRms > 0.2f) JarvisAmber else primaryCyan
            else -> primaryCyan
        },
        animationSpec = tween(350),
        label = "core_glow_tint"
    )

    // Fixed particle seeds
    val particleCount = 28
    val particleOffsets = remember {
        List(particleCount) { i ->
            val angle = (i * (2 * PI / particleCount)).toFloat()
            val radiusFrac = 0.55f + ((i % 5) * 0.11f)
            val yOffset = ((i % 7) - 3) * 0.12f
            val speedFactor = 0.7f + ((i % 4) * 0.35f)
            val sizeDp = 2f + (i % 3)
            ParticleData(angle, radiusFrac, yOffset, speedFactor, sizeDp)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .offset(y = floatOffsetDp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("jarvis_3d_hologram_core"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f * idleBreathing * audioBoost

            // ==========================================
            // LAYER 0: Soft Volumetric Glow & Background Halo
            // ==========================================
            drawVolumetricGlow(
                center = center,
                radius = baseRadius * 1.55f,
                glowColor = coreGlowColor,
                isThinking = effectiveThinking,
                isSpeaking = effectiveSpeaking
            )

            // ==========================================
            // LAYER 1: Radiating Acoustic Harmonic Waves (When Speaking or Listening)
            // ==========================================
            if (effectiveSpeaking || effectiveListening) {
                drawAcousticShockwaves(
                    center = center,
                    baseRadius = baseRadius,
                    progress = waveRadiateProgress,
                    tint = if (effectiveSpeaking) electricBlue else primaryCyan,
                    isSpeaking = effectiveSpeaking
                )
            }

            // ==========================================
            // LAYER 2: 3D Holographic Gyro Rings (BACK HALF - z < 0)
            // ==========================================
            draw3DGyroRings(
                center = center,
                radius = baseRadius,
                rotOuter = rotationAngleOuter,
                rotInner = rotationAngleInner,
                pitch = gyroPitchAngle,
                tint = primaryCyan,
                purpleTint = purpleAccent,
                isBackLayer = true
            )

            // ==========================================
            // LAYER 3: 3D Orbiting Ambient Holographic Particles (BACK)
            // ==========================================
            drawHolographicParticles(
                center = center,
                baseRadius = baseRadius,
                progress = rotationAngleOuter,
                particles = particleOffsets,
                cyanColor = primaryCyan,
                purpleColor = purpleAccent,
                isBackLayer = true
            )

            // ==========================================
            // LAYER 4: Central Glass-like Holographic Nucleus
            // ==========================================
            drawGlassNucleus(
                center = center,
                radius = baseRadius * 0.58f,
                glowColor = coreGlowColor,
                isThinking = effectiveThinking,
                isSpeaking = effectiveSpeaking,
                isListening = effectiveListening,
                rotAngle = rotationAngleOuter,
                rms = normalizedRms
            )

            // ==========================================
            // LAYER 5: 3D Holographic Gyro Rings (FRONT HALF - z > 0)
            // ==========================================
            draw3DGyroRings(
                center = center,
                radius = baseRadius,
                rotOuter = rotationAngleOuter,
                rotInner = rotationAngleInner,
                pitch = gyroPitchAngle,
                tint = primaryCyan,
                purpleTint = purpleAccent,
                isBackLayer = false
            )

            // ==========================================
            // LAYER 6: 3D Orbiting Ambient Holographic Particles (FRONT)
            // ==========================================
            drawHolographicParticles(
                center = center,
                baseRadius = baseRadius,
                progress = rotationAngleOuter,
                particles = particleOffsets,
                cyanColor = primaryCyan,
                purpleColor = purpleAccent,
                isBackLayer = false
            )

            // ==========================================
            // LAYER 7: Dynamic Holographic Waveform Equator & HUD Reticle
            // ==========================================
            drawEquatorialTelemetry(
                center = center,
                radius = baseRadius * 0.95f,
                rotAngle = rotationAngleOuter,
                counterRot = rotationAngleInner,
                tint = primaryCyan,
                purpleTint = purpleAccent,
                isSpeaking = effectiveSpeaking,
                isListening = effectiveListening,
                rms = normalizedRms
            )
        }
    }
}

private data class ParticleData(
    val initialAngle: Float,
    val radiusFactor: Float,
    val yTilt: Float,
    val speedFactor: Float,
    val sizeDp: Float
)

/**
 * Draws soft volumetric 3D glow falloff behind the core.
 */
private fun DrawScope.drawVolumetricGlow(
    center: Offset,
    radius: Float,
    glowColor: Color,
    isThinking: Boolean,
    isSpeaking: Boolean
) {
    // Outer ambient diffusion
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                glowColor.copy(alpha = if (isSpeaking) 0.38f else 0.22f),
                glowColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )

    // Inner concentrated energy core
    val coreRadius = radius * 0.55f
    val innerColor = if (isThinking) Color(0xFFA855F7) else Color(0xFF00E5FF)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                innerColor.copy(alpha = 0.45f),
                glowColor.copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = coreRadius
        ),
        center = center,
        radius = coreRadius
    )
}

/**
 * Draws expanding acoustic shockwaves when JARVIS is speaking or listening.
 */
private fun DrawScope.drawAcousticShockwaves(
    center: Offset,
    baseRadius: Float,
    progress: Float,
    tint: Color,
    isSpeaking: Boolean
) {
    val waveCount = if (isSpeaking) 3 else 2
    for (i in 0 until waveCount) {
        val wavePhase = (progress + (i.toFloat() / waveCount)) % 1f
        val waveRadius = baseRadius * (0.85f + (wavePhase * 0.65f))
        val waveAlpha = ((1f - wavePhase) * (if (isSpeaking) 0.55f else 0.35f)).coerceIn(0f, 1f)

        drawCircle(
            color = tint.copy(alpha = waveAlpha),
            radius = waveRadius,
            center = center,
            style = Stroke(
                width = 1.5.dp.toPx() * (1f - (wavePhase * 0.5f)),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), wavePhase * 20f)
            )
        )
    }
}

/**
 * Draws 3D elliptical gyroscope rings with authentic depth / parallax.
 * Separates rendering into back-layer (z < 0) and front-layer (z > 0).
 */
private fun DrawScope.draw3DGyroRings(
    center: Offset,
    radius: Float,
    rotOuter: Float,
    rotInner: Float,
    pitch: Float,
    tint: Color,
    purpleTint: Color,
    isBackLayer: Boolean
) {
    // Ring 1: Primary 3D Tilted Gyroscope (tilted at ~35 degrees)
    val ring1PitchRad = (pitch * PI / 180.0).toFloat()
    val ring1Flatten = 0.42f + (sin(ring1PitchRad) * 0.18f) // Dynamic 3D tilt oscillation
    draw3DEllipseArc(
        center = center,
        radiusX = radius * 1.08f,
        radiusY = radius * 1.08f * ring1Flatten,
        rotationDeg = 28f + (sin(ring1PitchRad) * 12f),
        tint = tint,
        isBackLayer = isBackLayer,
        strokeWidth = 2.2f,
        segmented = true,
        phase = rotOuter
    )

    // Ring 2: Orthogonal 3D Inclined Polar Ring (tilted opposite, with purple accents)
    val ring2Flatten = 0.38f + (cos(ring1PitchRad * 0.8f) * 0.15f)
    draw3DEllipseArc(
        center = center,
        radiusX = radius * 0.88f,
        radiusY = radius * 0.88f * ring2Flatten,
        rotationDeg = -52f,
        tint = purpleTint,
        isBackLayer = isBackLayer,
        strokeWidth = 1.8f,
        segmented = true,
        phase = rotInner
    )

    // Ring 3: Precessing Equatorial Gyro Ring
    val ring3Flatten = 0.55f
    draw3DEllipseArc(
        center = center,
        radiusX = radius * 1.22f,
        radiusY = radius * 1.22f * ring3Flatten,
        rotationDeg = 75f,
        tint = tint.copy(alpha = 0.85f),
        isBackLayer = isBackLayer,
        strokeWidth = 1.5f,
        segmented = false,
        phase = rotOuter * 0.6f
    )
}

/**
 * Draws an inclined 3D ellipse arc split into front or back depth layers.
 */
private fun DrawScope.draw3DEllipseArc(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    rotationDeg: Float,
    tint: Color,
    isBackLayer: Boolean,
    strokeWidth: Float,
    segmented: Boolean,
    phase: Float
) {
    rotate(rotationDeg, center) {
        val sweepAngle = 180f
        val startAngle = if (isBackLayer) 180f else 0f
        val alpha = if (isBackLayer) 0.28f else 0.95f
        val currentStroke = if (isBackLayer) strokeWidth * 0.75f else strokeWidth

        val rect = Size(radiusX * 2f, radiusY * 2f)
        val topLeft = Offset(center.x - radiusX, center.y - radiusY)

        if (segmented) {
            val segmentCount = 6
            val segSweep = (sweepAngle / segmentCount) * 0.7f
            val segGap = (sweepAngle / segmentCount) * 0.3f

            for (s in 0 until segmentCount) {
                val segStart = startAngle + (s * (segSweep + segGap))
                drawArc(
                    color = tint.copy(alpha = alpha),
                    startAngle = segStart,
                    sweepAngle = segSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = rect,
                    style = Stroke(
                        width = currentStroke.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        } else {
            drawArc(
                color = tint.copy(alpha = alpha),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(
                    width = currentStroke.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Draws the transparent glass-like central holographic nucleus.
 */
private fun DrawScope.drawGlassNucleus(
    center: Offset,
    radius: Float,
    glowColor: Color,
    isThinking: Boolean,
    isSpeaking: Boolean,
    isListening: Boolean,
    rotAngle: Float,
    rms: Float
) {
    // 1. Concentric Glass Sphere Shell with Fresnel Rim Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x0800E5FF),
                glowColor.copy(alpha = 0.12f),
                glowColor.copy(alpha = 0.38f),
                Color(0xEEFFFFFF) // Glass rim reflection
            ),
            center = Offset(center.x - (radius * 0.2f), center.y - (radius * 0.2f)),
            radius = radius
        ),
        center = center,
        radius = radius
    )

    // Glass shell outer border with neon edge
    drawCircle(
        brush = Brush.sweepGradient(
            listOf(
                glowColor.copy(alpha = 0.95f),
                Color(0xFF8A2BE2).copy(alpha = 0.6f),
                glowColor.copy(alpha = 0.95f)
            ),
            center = center
        ),
        center = center,
        radius = radius,
        style = Stroke(width = 1.8.dp.toPx())
    )

    // 2. Inner Rotating Astrolabe Segments (Hexagonal / Quantum Aperture)
    rotate(rotAngle * 0.8f, center) {
        val innerRadius = radius * 0.62f
        val spokes = if (isThinking) 8 else 6
        for (i in 0 until spokes) {
            val angleRad = (i * (2 * PI / spokes)).toFloat()
            val start = Offset(center.x + cos(angleRad) * (innerRadius * 0.3f), center.y + sin(angleRad) * (innerRadius * 0.3f))
            val end = Offset(center.x + cos(angleRad) * innerRadius, center.y + sin(angleRad) * innerRadius)
            drawLine(
                color = glowColor.copy(alpha = 0.65f),
                start = start,
                end = end,
                strokeWidth = 1.4.dp.toPx()
            )
        }
    }

    // 3. Central Singularity Core
    val singularityRadius = radius * (0.28f + (rms * 0.15f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                glowColor,
                glowColor.copy(alpha = 0.4f),
                Color.Transparent
            ),
            center = center,
            radius = singularityRadius
        ),
        center = center,
        radius = singularityRadius
    )
}

/**
 * Draws 3D Orbiting Ambient Holographic Particles.
 */
private fun DrawScope.drawHolographicParticles(
    center: Offset,
    baseRadius: Float,
    progress: Float,
    particles: List<ParticleData>,
    cyanColor: Color,
    purpleColor: Color,
    isBackLayer: Boolean
) {
    val radStep = (progress * PI / 180.0).toFloat()

    particles.forEachIndexed { index, p ->
        val currentAngle = p.initialAngle + (radStep * p.speedFactor)
        val z = sin(currentAngle) // Depth in 3D: z > 0 is front, z < 0 is back

        val matchesLayer = if (isBackLayer) z <= 0 else z > 0
        if (matchesLayer) {
            val r = baseRadius * p.radiusFactor
            val x = center.x + (cos(currentAngle) * r)
            val y = center.y + (sin(currentAngle) * (r * 0.45f)) + (p.yTilt * baseRadius)

            // Depth cues: scale size and alpha by depth z
            val depthScale = 0.65f + ((z + 1f) * 0.25f)
            val particleAlpha = (0.35f + ((z + 1f) * 0.32f)).coerceIn(0.1f, 1.0f)
            val color = if (index % 3 == 0) purpleColor else cyanColor

            drawCircle(
                color = color.copy(alpha = particleAlpha),
                radius = p.sizeDp.dp.toPx() * depthScale,
                center = Offset(x, y)
            )

            // Sparkle halo for bright foreground particles
            if (!isBackLayer && z > 0.5f) {
                drawCircle(
                    color = Color.White.copy(alpha = particleAlpha * 0.6f),
                    radius = (p.sizeDp.dp.toPx() * depthScale) * 0.45f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Draws the outer equatorial telemetry ring, tick marks, and audio waveform bars.
 */
private fun DrawScope.drawEquatorialTelemetry(
    center: Offset,
    radius: Float,
    rotAngle: Float,
    counterRot: Float,
    tint: Color,
    purpleTint: Color,
    isSpeaking: Boolean,
    isListening: Boolean,
    rms: Float
) {
    rotate(rotAngle * 0.5f, center) {
        val tickCount = 36
        for (i in 0 until tickCount) {
            val tickAngleRad = (i * (2 * PI / tickCount)).toFloat()
            val isMajor = i % 4 == 0
            val tickLen = if (isMajor) 8.dp.toPx() else 4.dp.toPx()

            // Dynamic waveform modulation when speaking or listening
            val waveMod = if (isSpeaking || isListening) {
                sin((tickAngleRad * 4f) + (rotAngle * 0.1f)) * (rms * 16.dp.toPx())
            } else {
                0f
            }

            val r1 = radius + 4.dp.toPx()
            val r2 = r1 + tickLen + waveMod.coerceAtLeast(0f)

            val p1 = Offset(center.x + cos(tickAngleRad) * r1, center.y + sin(tickAngleRad) * r1)
            val p2 = Offset(center.x + cos(tickAngleRad) * r2, center.y + sin(tickAngleRad) * r2)

            val tickColor = if (isMajor) tint else purpleTint.copy(alpha = 0.55f)
            drawLine(
                color = tickColor.copy(alpha = if (isMajor) 0.85f else 0.45f),
                start = p1,
                end = p2,
                strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.0.dp.toPx()
            )
        }
    }
}
