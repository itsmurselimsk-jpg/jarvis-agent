package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.OrbCoreTool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.provider.GeminiAIProvider
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.ui.components.HolographicOrb3D
import com.example.jarvis.ui.components.Point3D
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.PI

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisHologramCyberneticUnitTest {

    private lateinit var context: Context
    private lateinit var bridge: AndroidBridge
    private lateinit var repository: JarvisRepository
    private lateinit var toolContext: ToolContext

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        bridge = AndroidBridge(context)
        toolContext = ToolContext(repository = repository, bridge = bridge)
    }

    @Test
    fun test3DRotationMath() {
        val original = Point3D(1f, 0f, 0f)
        // 90 deg (PI/2) yaw rotation around Y-axis
        val rotated = HolographicOrb3D.rotate(original, pitchRad = 0f, yawRad = PI.toFloat() / 2f)
        assertTrue(kotlin.math.abs(rotated.x) < 0.001f)
        assertEquals(0f, rotated.y, 0.001f)
        assertEquals(-1f, rotated.z, 0.001f)
    }

    @Test
    fun test3DPerspectiveProjection() {
        val center = Offset(100f, 100f)
        val radius = 50f
        val point = Point3D(0f, 0f, 0f)
        val proj = HolographicOrb3D.project(point, center, radius, zoom = 1.0f)

        assertEquals(100f, proj.screenX, 0.001f)
        assertEquals(100f, proj.screenY, 0.001f)
        assertTrue(proj.isFrontFacing)
    }

    @Test
    fun testTelemetrySnippetsAvailability() {
        val snippets = HolographicOrb3D.TELEMETRY_SNIPPETS
        assertTrue(snippets.isNotEmpty())
        assertTrue(snippets.contains("SYS.INIT"))
        assertTrue(snippets.contains("CORE.0"))
        assertTrue(snippets.contains("0xFF3A"))
    }

    @Test
    fun testOrbCoreToolStatus() = runBlocking {
        val tool = OrbCoreTool()
        val result = tool.execute("status", toolContext)
        assertTrue(result.success)
        assertTrue(result.output.contains("J.A.R.V.I.S. HOLOGRAPHIC CORE MATRIX"))
        assertTrue(result.output.contains("Outer Shell"))
        assertTrue(result.output.contains("Inner Core"))
    }

    @Test
    fun testOrbCoreToolReset() = runBlocking {
        val tool = OrbCoreTool()
        val result = tool.execute("reset orb", toolContext)
        assertTrue(result.success)
        assertEquals("RESET_ORIENTATION", result.metadata["action"])
        assertTrue(result.output.contains("Home Position"))
    }

    @Test
    fun testOrbCoreToolOverclock() = runBlocking {
        val tool = OrbCoreTool()
        val result = tool.execute("overclock core boost", toolContext)
        assertTrue(result.success)
        assertEquals("OVERCLOCK_MATRIX", result.metadata["action"])
        assertTrue(result.output.contains("Turbo Matrix Active"))
    }

    @Test
    fun testOrbCoreToolSpin() = runBlocking {
        val tool = OrbCoreTool()
        val result = tool.execute("spin orb", toolContext)
        assertTrue(result.success)
        assertEquals("SPIN_SURGE", result.metadata["action"])
        assertTrue(result.output.contains("Angular Momentum"))
    }

    @Test
    fun testLocalAIProviderOrbRouting() = runBlocking {
        val provider = GeminiAIProvider(repository)
        val decision = provider.decideTool("JARVIS show me the orb status", emptyList(), "")
        assertTrue(decision.useTool)
        assertEquals("OrbCore", decision.toolName)

        val spinDecision = provider.decideTool("spin orb now", emptyList(), "")
        assertTrue(spinDecision.useTool)
        assertEquals("OrbCore", spinDecision.toolName)

        val overclockDecision = provider.decideTool("overclock core", emptyList(), "")
        assertTrue(overclockDecision.useTool)
        assertEquals("OrbCore", overclockDecision.toolName)
    }
}
