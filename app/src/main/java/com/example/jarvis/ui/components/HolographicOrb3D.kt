package com.example.jarvis.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D Holographic Geometry and Projection Engine for JARVIS Cybernetic Core.
 */
data class Point3D(val x: Float, val y: Float, val z: Float)

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val scale: Float,
    val isFrontFacing: Boolean
)

object HolographicOrb3D {

    const val CAMERA_DISTANCE = 3.2f

    fun rotate(point: Point3D, pitchRad: Float, yawRad: Float): Point3D {
        // Yaw rotation around Y-axis
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val x1 = point.x * cosY + point.z * sinY
        val y1 = point.y
        val z1 = -point.x * sinY + point.z * cosY

        // Pitch rotation around X-axis
        val cosX = cos(pitchRad)
        val sinX = sin(pitchRad)
        val x2 = x1
        val y2 = y1 * cosX - z1 * sinX
        val z2 = y1 * sinX + z1 * cosX

        return Point3D(x2, y2, z2)
    }

    fun project(
        point: Point3D,
        center: Offset,
        radius: Float,
        zoom: Float
    ): ProjectedPoint {
        val depth = (CAMERA_DISTANCE + point.z).coerceAtLeast(0.6f)
        val scale = (CAMERA_DISTANCE / depth) * zoom
        val sx = center.x + point.x * radius * scale
        val sy = center.y - point.y * radius * scale
        return ProjectedPoint(
            screenX = sx,
            screenY = sy,
            depth = depth,
            scale = scale,
            isFrontFacing = point.z >= -0.05f
        )
    }

    /**
     * Draw a 3D latitude ring on the sphere.
     */
    fun drawLatitudeRing(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        latRad: Float,
        pitchRad: Float,
        yawRad: Float,
        zoom: Float,
        color: Color,
        alphaMultiplier: Float = 1f,
        strokeWidth: Float = 1.5f,
        segments: Int = 40
    ) {
        val rLat = cos(latRad)
        val yLat = sin(latRad)
        val pathFront = Path()
        val pathBack = Path()
        var hasFrontStart = false
        var hasBackStart = false

        for (i in 0..segments) {
            val lon = (i.toFloat() / segments) * (2 * PI.toFloat())
            val raw = Point3D(rLat * cos(lon), yLat, rLat * sin(lon))
            val rot = rotate(raw, pitchRad, yawRad)
            val proj = project(rot, center, radius, zoom)

            if (rot.z >= 0f) {
                if (!hasFrontStart) {
                    pathFront.moveTo(proj.screenX, proj.screenY)
                    hasFrontStart = true
                } else {
                    pathFront.lineTo(proj.screenX, proj.screenY)
                }
            } else {
                if (!hasBackStart) {
                    pathBack.moveTo(proj.screenX, proj.screenY)
                    hasBackStart = true
                } else {
                    pathBack.lineTo(proj.screenX, proj.screenY)
                }
            }
        }

        // Draw back hemisphere (fainter for 3D depth cueing)
        drawScope.drawPath(
            path = pathBack,
            color = color.copy(alpha = (0.15f * alphaMultiplier).coerceIn(0f, 1f)),
            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
        )
        // Draw front hemisphere (brighter)
        drawScope.drawPath(
            path = pathFront,
            color = color.copy(alpha = (0.75f * alphaMultiplier).coerceIn(0f, 1f)),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

    /**
     * Draw a 3D meridian on the sphere.
     */
    fun drawMeridian(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        lonRad: Float,
        pitchRad: Float,
        yawRad: Float,
        zoom: Float,
        color: Color,
        alphaMultiplier: Float = 1f,
        strokeWidth: Float = 1.5f,
        segments: Int = 36
    ) {
        val pathFront = Path()
        val pathBack = Path()
        var hasFrontStart = false
        var hasBackStart = false

        for (i in 0..segments) {
            val lat = (i.toFloat() / segments) * PI.toFloat() - (PI.toFloat() / 2f)
            val raw = Point3D(cos(lat) * cos(lonRad), sin(lat), cos(lat) * sin(lonRad))
            val rot = rotate(raw, pitchRad, yawRad)
            val proj = project(rot, center, radius, zoom)

            if (rot.z >= 0f) {
                if (!hasFrontStart) {
                    pathFront.moveTo(proj.screenX, proj.screenY)
                    hasFrontStart = true
                } else {
                    pathFront.lineTo(proj.screenX, proj.screenY)
                }
            } else {
                if (!hasBackStart) {
                    pathBack.moveTo(proj.screenX, proj.screenY)
                    hasBackStart = true
                } else {
                    pathBack.lineTo(proj.screenX, proj.screenY)
                }
            }
        }

        drawScope.drawPath(
            path = pathBack,
            color = color.copy(alpha = (0.15f * alphaMultiplier).coerceIn(0f, 1f)),
            style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round)
        )
        drawScope.drawPath(
            path = pathFront,
            color = color.copy(alpha = (0.75f * alphaMultiplier).coerceIn(0f, 1f)),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

    /**
     * Draw J.A.R.V.I.S. 4 Cross-Meridian High-Intensity Bands (targeting crosshairs).
     */
    fun drawCrossMeridians(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        pitchRad: Float,
        yawRad: Float,
        zoom: Float,
        color: Color,
        alphaMultiplier: Float = 1f
    ) {
        val crossAngles = listOf(0f, PI.toFloat() / 2f, PI.toFloat(), 3 * PI.toFloat() / 2f)
        val offsets = listOf(-0.04f, 0f, 0.04f)

        for (baseAngle in crossAngles) {
            for (off in offsets) {
                val isCenter = off == 0f
                drawMeridian(
                    drawScope = drawScope,
                    center = center,
                    radius = radius,
                    lonRad = baseAngle + off,
                    pitchRad = pitchRad,
                    yawRad = yawRad,
                    zoom = zoom,
                    color = color,
                    alphaMultiplier = (if (isCenter) 1.2f else 0.7f) * alphaMultiplier,
                    strokeWidth = if (isCenter) 2.2f else 1.2f,
                    segments = 30
                )
            }
        }
    }

    /**
     * Draw 8-strand Geodesic Helical Spirals for Inner Core (J.A.R.V.I.S. inner core layer).
     */
    fun drawInnerSpiralCore(
        drawScope: DrawScope,
        center: Offset,
        coreRadius: Float,
        pitchRad: Float,
        yawRad: Float,
        zoom: Float,
        innerSpin: Float,
        color: Color,
        alphaMultiplier: Float = 1f
    ) {
        val strands = 6
        val turns = 2.5f
        val segments = 40

        for (s in 0 until strands) {
            val phase = (s.toFloat() / strands) * (2 * PI.toFloat())
            val path = Path()
            var started = false

            for (i in 0..segments) {
                val t = i.toFloat() / segments
                val lat = t * PI.toFloat() - (PI.toFloat() / 2f)
                val lon = t * turns * (2 * PI.toFloat()) + phase + innerSpin
                val raw = Point3D(cos(lat) * cos(lon), sin(lat), cos(lat) * sin(lon))
                val rot = rotate(raw, pitchRad, yawRad)
                val proj = project(rot, center, coreRadius, zoom)

                if (!started) {
                    path.moveTo(proj.screenX, proj.screenY)
                    started = true
                } else {
                    path.lineTo(proj.screenX, proj.screenY)
                }
            }

            drawScope.drawPath(
                path = path,
                color = color.copy(alpha = (0.55f * alphaMultiplier).coerceIn(0f, 1f)),
                style = Stroke(width = 1.8f, cap = StrokeCap.Round)
            )
        }
    }

    /**
     * Sweeping 3D Laser Scan Rings moving along the Y-axis.
     */
    fun drawScanRing(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        yNorm: Float, // -0.9 to 0.9
        pitchRad: Float,
        yawRad: Float,
        zoom: Float,
        color: Color,
        strokeWidth: Float = 2.2f
    ) {
        val clampedY = yNorm.coerceIn(-0.95f, 0.95f)
        val sliceR = sqrt(max(0.01f, 1f - clampedY * clampedY))
        val segments = 48
        val path = Path()
        var started = false

        for (i in 0..segments) {
            val a = (i.toFloat() / segments) * (2 * PI.toFloat())
            val raw = Point3D(sliceR * cos(a), clampedY, sliceR * sin(a))
            val rot = rotate(raw, pitchRad, yawRad)
            val proj = project(rot, center, radius, zoom)

            if (!started) {
                path.moveTo(proj.screenX, proj.screenY)
                started = true
            } else {
                path.lineTo(proj.screenX, proj.screenY)
            }
        }

        val ringAlpha = (1f - (clampedY * clampedY)) * 0.75f
        drawScope.drawPath(
            path = path,
            color = color.copy(alpha = ringAlpha.coerceIn(0.1f, 0.85f)),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

    /**
     * Draw central 3D wireframe Icosahedron cage.
     */
    fun drawIcosahedronCage(
        drawScope: DrawScope,
        center: Offset,
        radius: Float,
        pitchRad: Float,
        yawRad: Float,
        zoom: Float,
        color: Color,
        alpha: Float = 0.85f
    ) {
        // Golden ratio
        val phi = (1f + sqrt(5f)) / 2f
        val norm = sqrt(1f + phi * phi)
        val a = 1f / norm
        val b = phi / norm

        val vertices = listOf(
            Point3D(-a, b, 0f), Point3D(a, b, 0f), Point3D(-a, -b, 0f), Point3D(a, -b, 0f),
            Point3D(0f, -a, b), Point3D(0f, a, b), Point3D(0f, -a, -b), Point3D(0f, a, -b),
            Point3D(b, 0f, -a), Point3D(b, 0f, a), Point3D(-b, 0f, -a), Point3D(-b, 0f, a)
        )

        val edges = listOf(
            0 to 1, 0 to 5, 0 to 7, 0 to 10, 0 to 11,
            1 to 5, 1 to 7, 1 to 8, 1 to 9,
            2 to 3, 2 to 4, 2 to 6, 2 to 10, 2 to 11,
            3 to 4, 3 to 6, 3 to 8, 3 to 9,
            4 to 5, 4 to 9, 4 to 11,
            5 to 9, 5 to 11,
            6 to 7, 6 to 8, 6 to 10,
            7 to 8, 7 to 10,
            8 to 9, 10 to 11
        )

        for ((i1, i2) in edges) {
            val v1 = project(rotate(vertices[i1], pitchRad, yawRad), center, radius, zoom)
            val v2 = project(rotate(vertices[i2], pitchRad, yawRad), center, radius, zoom)
            drawScope.drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.1f, 1f)),
                start = Offset(v1.screenX, v1.screenY),
                end = Offset(v2.screenX, v2.screenY),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round
            )
        }
    }

    /**
     * Floating Cybernetic Code Glyphs / Telemetry Tokens.
     */
    val TELEMETRY_SNIPPETS = listOf(
        "SYS.INIT", "0xFF3A", "CORE.0", "01101001", "TCP/SYN",
        "TLS 1.3", "DMA xfer", "AES-256", "200 OK", "ARC.NEW",
        "IRQ 0x7", "SIGTERM", "KERNEL.D", "SHA-256"
    )
}
