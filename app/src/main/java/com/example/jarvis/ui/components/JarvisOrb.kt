package com.example.jarvis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.jarvis.model.JarvisState
import com.example.jarvis.ui.components.HologramThemeManager
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisPurpleHighlight
import com.example.jarvis.ui.theme.JarvisRed
import com.example.jarvis.voice.CyberneticAudioEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-fidelity 3D Holographic JARVIS Cybernetic Orb.
 * Integrates J.A.R.V.I.S. multi-layered wireframe spherical shells, sweeping scan rings,
 * spiral geodesic inner core, 3D floating telemetry code glyphs, and central icosahedron cage,
 * combined with touch interaction (drag to spin, pinch to zoom) and state-reactive acoustics.
 */
@Composable
fun JarvisOrb(
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    state: JarvisState = JarvisState.IDLE,
    showHudControls: Boolean = false,
    onClick: () -> Unit = {}
) {
    // Holographic Theme Matrix (JARVIS Cyan, Combat Crimson, Arc Gold, Quantum Green, Stealth Violet)
    val currentTheme by HologramThemeManager.currentTheme.collectAsState()

    // Interactive 3D Camera Angles
    var userPitch by remember { mutableFloatStateOf(0.18f) }
    var userYaw by remember { mutableFloatStateOf(0f) }
    var userZoom by remember { mutableFloatStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    // Outer continuous rotation
    val autoSpinOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_spin_outer"
    )

    // Inner counter-rotation
    val autoSpinInner by infiniteTransition.animateFloat(
        initialValue = 2 * PI.toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_spin_inner"
    )

    // Sweeping laser scan rings along Y axis (-0.85 to 0.85)
    val scanY1 by infiniteTransition.animateFloat(
        initialValue = -0.85f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 1600
                    JarvisState.THINKING -> 1200
                    else -> 3200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y1"
    )

    val scanY2 by infiniteTransition.animateFloat(
        initialValue = 0.80f,
        targetValue = -0.80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_y2"
    )

    // State-dependent breathing / pulsing
    val pulseBreathing by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    JarvisState.LISTENING -> 800
                    JarvisState.SPEAKING -> 500
                    JarvisState.THINKING -> 400
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_breathing"
    )

    // Acoustic wave ripple expansion for LISTENING state
    val listeningRipple by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "listening_ripple"
    )

    // Dynamic vocal frequency oscillations for SPEAKING state
    val speakingHarmonic by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_harmonic"
    )

    // State transition spring physics
    val stateScale by animateFloatAsState(
        targetValue = when (state) {
            JarvisState.LISTENING -> 1.15f
            JarvisState.SPEAKING -> 1.12f
            JarvisState.THINKING -> 1.06f
            JarvisState.ERROR -> 0.95f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "state_scale"
    )

    val targetPrimary = when (state) {
        JarvisState.IDLE -> currentTheme.primary
        JarvisState.LISTENING -> currentTheme.accent
        JarvisState.SPEAKING -> currentTheme.secondary
        JarvisState.THINKING -> currentTheme.primary
        JarvisState.ERROR -> JarvisRed
    }

    val targetSecondary = when (state) {
        JarvisState.IDLE -> currentTheme.secondary
        JarvisState.LISTENING -> currentTheme.primary
        JarvisState.SPEAKING -> currentTheme.accent
        JarvisState.THINKING -> currentTheme.accent
        JarvisState.ERROR -> JarvisAmber
    }

    val primaryColor by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "primary_color"
    )

    val secondaryColor by animateColorAsState(
        targetValue = targetSecondary,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "secondary_color"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when (state) {
            JarvisState.LISTENING -> 0.70f
            JarvisState.SPEAKING -> 0.80f
            JarvisState.THINKING -> 0.60f
            else -> 0.40f
        },
        animationSpec = tween(durationMillis = 400),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("jarvis_orb")
            .clickable {
                CyberneticAudioEngine.playOrbBeep(1800f, 40)
                onClick()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        CyberneticAudioEngine.playOrbBeep(1200f, 25)
                    },
                    onDragEnd = { },
                    onDragCancel = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        userYaw += dragAmount.x * 0.009f
                        userPitch = (userPitch - dragAmount.y * 0.009f).coerceIn(-1.2f, 1.2f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    userZoom = (userZoom * zoom).coerceIn(0.65f, 2.2f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("jarvis_orb_canvas")
        ) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() * 0.38f * stateScale
            val effectivePitch = userPitch
            val effectiveYaw = userYaw + autoSpinOuter

            // 1. Ambient Radial Glow Field
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha * pulseBreathing),
                        secondaryColor.copy(alpha = glowAlpha * 0.5f),
                        JarvisPurpleHighlight.copy(alpha = glowAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f * userZoom
                ),
                radius = baseRadius * 1.45f * userZoom,
                center = center
            )

            // 2. LAYER 1: OUTER 3D WIREFRAME SPHERICAL SHELL
            // Latitude Rings
            val latSteps = listOf(-0.9f, -0.6f, -0.3f, 0f, 0.3f, 0.6f, 0.9f)
            for (lat in latSteps) {
                val isEquator = lat == 0f
                HolographicOrb3D.drawLatitudeRing(
                    drawScope = this,
                    center = center,
                    radius = baseRadius,
                    latRad = lat,
                    pitchRad = effectivePitch,
                    yawRad = effectiveYaw,
                    zoom = userZoom,
                    color = if (isEquator) primaryColor else secondaryColor,
                    alphaMultiplier = if (isEquator) 1.2f else 0.7f,
                    strokeWidth = if (isEquator) 2.2f else 1.3f
                )
            }

            // Meridians (Longitudinal Wireframe)
            val meridianCount = 12
            for (m in 0 until meridianCount) {
                val lon = (m.toFloat() / meridianCount) * PI.toFloat() * 2f
                val isCardinal = m % 3 == 0
                HolographicOrb3D.drawMeridian(
                    drawScope = this,
                    center = center,
                    radius = baseRadius,
                    lonRad = lon,
                    pitchRad = effectivePitch,
                    yawRad = effectiveYaw,
                    zoom = userZoom,
                    color = if (isCardinal) primaryColor else secondaryColor,
                    alphaMultiplier = if (isCardinal) 0.9f else 0.45f,
                    strokeWidth = if (isCardinal) 1.8f else 1.1f
                )
            }

            // J.A.R.V.I.S. Cross-Meridian High-Intensity Targeting Bands
            HolographicOrb3D.drawCrossMeridians(
                drawScope = this,
                center = center,
                radius = baseRadius,
                pitchRad = effectivePitch,
                yawRad = effectiveYaw,
                zoom = userZoom,
                color = primaryColor,
                alphaMultiplier = 0.95f
            )

            // 3. LAYER 2: 3D HEXAGONAL SURFACE NODES
            val hexNodes = listOf(
                Point3D(0.6f, 0.4f, 0.69f),
                Point3D(-0.7f, 0.3f, 0.65f),
                Point3D(0.3f, -0.7f, 0.65f),
                Point3D(-0.4f, -0.5f, 0.76f),
                Point3D(0.8f, -0.2f, 0.56f)
            )
            for (node in hexNodes) {
                val rot = HolographicOrb3D.rotate(node, effectivePitch, effectiveYaw)
                val proj = HolographicOrb3D.project(rot, center, baseRadius, userZoom)
                if (proj.isFrontFacing) {
                    val hexSize = 5.dp.toPx() * proj.scale
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.85f),
                        radius = hexSize * 0.5f,
                        center = Offset(proj.screenX, proj.screenY),
                        style = Stroke(width = 1.4f)
                    )
                }
            }

            // 4. LAYER 3: 3D INNER GEODESIC SPIRAL CORE
            val innerCoreRadius = baseRadius * 0.48f * pulseBreathing
            HolographicOrb3D.drawInnerSpiralCore(
                drawScope = this,
                center = center,
                coreRadius = innerCoreRadius,
                pitchRad = effectivePitch,
                yawRad = effectiveYaw + autoSpinInner,
                zoom = userZoom,
                innerSpin = autoSpinInner * 1.5f,
                color = primaryColor,
                alphaMultiplier = 0.85f
            )

            // 5. LAYER 4: CENTRAL WIREFRAME ICOSAHEDRON CAGE & REACTOR
            val icoRadius = innerCoreRadius * 0.42f
            HolographicOrb3D.drawIcosahedronCage(
                drawScope = this,
                center = center,
                radius = icoRadius,
                pitchRad = effectivePitch + autoSpinOuter * 0.8f,
                yawRad = effectiveYaw + autoSpinInner * 0.8f,
                zoom = userZoom,
                color = Color.White,
                alpha = 0.9f
            )

            // Central Pulsating Reactor Glow
            val reactorR = (innerCoreRadius * 0.32f) * pulseBreathing
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        secondaryColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = reactorR * userZoom
                ),
                radius = reactorR * userZoom,
                center = center
            )

            // 6. LAYER 5: SWEEPING 3D LASER SCAN RINGS
            HolographicOrb3D.drawScanRing(
                drawScope = this,
                center = center,
                radius = baseRadius,
                yNorm = scanY1,
                pitchRad = effectivePitch,
                yawRad = effectiveYaw,
                zoom = userZoom,
                color = JarvisCyanBright,
                strokeWidth = 2.4f
            )

            HolographicOrb3D.drawScanRing(
                drawScope = this,
                center = center,
                radius = baseRadius * 0.85f,
                yNorm = scanY2,
                pitchRad = effectivePitch,
                yawRad = effectiveYaw,
                zoom = userZoom,
                color = secondaryColor,
                strokeWidth = 1.8f
            )

            // 7. LAYER 6: FLOATING 3D CYBERNETIC CODE GLYPHS
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (160 * glowAlpha).toInt().coerceIn(40, 240),
                    (primaryColor.red * 255).toInt(),
                    (primaryColor.green * 255).toInt(),
                    (primaryColor.blue * 255).toInt()
                )
                textSize = (9f * density * userZoom).coerceIn(14f, 32f)
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val snippets = HolographicOrb3D.TELEMETRY_SNIPPETS
            for (idx in 0 until 6) {
                val phi = (idx * 0.9f) - 1.2f
                val theta = (idx * 1.05f) + autoSpinOuter * 0.6f
                val r = 1.08f
                val pt = Point3D(r * cos(phi) * cos(theta), r * sin(phi), r * cos(phi) * sin(theta))
                val rot = HolographicOrb3D.rotate(pt, effectivePitch, effectiveYaw)
                val proj = HolographicOrb3D.project(rot, center, baseRadius, userZoom)
                if (proj.isFrontFacing) {
                    drawContext.canvas.nativeCanvas.drawText(
                        snippets[idx % snippets.size],
                        proj.screenX,
                        proj.screenY,
                        paint
                    )
                }
            }

            // 8. STATE-SPECIFIC ACOUSTICS / EQUALIZER
            when (state) {
                JarvisState.LISTENING -> {
                    // Acoustic ripples expanding outward
                    val rippleRadius = baseRadius * listeningRipple * userZoom
                    val rippleAlpha = (1f - (listeningRipple - 0.5f) / 0.85f).coerceIn(0f, 1f) * 0.75f
                    drawCircle(
                        color = JarvisCyanBright.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                JarvisState.SPEAKING -> {
                    // Harmonic Soundwave Equalizer Arcs around perimeter
                    val arcRadius = baseRadius * 1.10f * userZoom
                    val arcCount = 18
                    val step = 360f / arcCount
                    for (i in 0 until arcCount) {
                        val angle = (i * step) + Math.toDegrees(effectiveYaw.toDouble()).toFloat()
                        val rad = Math.toRadians(angle.toDouble())
                        val barHeight = (4.dp.toPx() + (9.dp.toPx() * ((i % 4 + 1) / 4f) * speakingHarmonic))
                        val startR = arcRadius
                        val endR = arcRadius + barHeight

                        val start = Offset(
                            center.x + (startR * cos(rad)).toFloat(),
                            center.y + (startR * sin(rad)).toFloat()
                        )
                        val end = Offset(
                            center.x + (endR * cos(rad)).toFloat(),
                            center.y + (endR * sin(rad)).toFloat()
                        )

                        drawLine(
                            brush = Brush.linearGradient(listOf(primaryColor, secondaryColor)),
                            start = start,
                            end = end,
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                else -> {}
            }

            // 9. Radiant Core Point
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx() * pulseBreathing,
                center = center
            )
        }
    }
}
