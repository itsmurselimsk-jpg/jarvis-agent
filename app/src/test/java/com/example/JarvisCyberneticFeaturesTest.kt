package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.brain.OrbCoreTool
import com.example.jarvis.brain.TacticalTool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.intent.IntentClassifier
import com.example.jarvis.provider.GeminiAIProvider
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.tactical.TacticalAnalysisEngine
import com.example.jarvis.ui.components.HologramTheme
import com.example.jarvis.ui.components.HologramThemeManager
import com.example.jarvis.voice.CyberneticAudioEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisCyberneticFeaturesTest {

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
    fun testHologramThemeParsing() {
        assertEquals(HologramTheme.JARVIS_CRIMSON, HologramTheme.fromString("activate combat mode"))
        assertEquals(HologramTheme.JARVIS_CRIMSON, HologramTheme.fromString("crimson theme"))
        assertEquals(HologramTheme.ARC_GOLD, HologramTheme.fromString("arc reactor core"))
        assertEquals(HologramTheme.ARC_GOLD, HologramTheme.fromString("mark 85 gold"))
        assertEquals(HologramTheme.QUANTUM_EMERALD, HologramTheme.fromString("quantum matrix"))
        assertEquals(HologramTheme.STEALTH_VIOLET, HologramTheme.fromString("stealth violet"))
        assertEquals(HologramTheme.JARVIS_CLASSIC, HologramTheme.fromString("standard jarvis"))
    }

    @Test
    fun testHologramThemeManager() {
        HologramThemeManager.setTheme(HologramTheme.ARC_GOLD)
        assertEquals(HologramTheme.ARC_GOLD, HologramThemeManager.getActiveTheme())

        HologramThemeManager.setThemeByName("combat crimson matrix")
        assertEquals(HologramTheme.JARVIS_CRIMSON, HologramThemeManager.getActiveTheme())

        // Reset to classic
        HologramThemeManager.setTheme(HologramTheme.JARVIS_CLASSIC)
        assertEquals(HologramTheme.JARVIS_CLASSIC, HologramThemeManager.getActiveTheme())
    }

    @Test
    fun testCyberneticAudioEngineExecution() {
        CyberneticAudioEngine.isMuted = true
        CyberneticAudioEngine.playOrbBeep()
        CyberneticAudioEngine.playReactorSurge()
        CyberneticAudioEngine.playScanPing()
        CyberneticAudioEngine.playShieldEngage()
        CyberneticAudioEngine.playWakeChime()
        CyberneticAudioEngine.isMuted = false
        assertTrue(true)
    }

    @Test
    fun testTacticalAnalysisEngine() = runBlocking {
        val assessment = TacticalAnalysisEngine.analyzeTacticalSituation(
            situationQuery = "critical threat perimeter breach",
            bridge = bridge,
            repository = repository
        )
        assertNotNull(assessment)
        assertTrue(assessment.threatLevel.contains("COMBAT PROTOCOL") || assessment.threatLevel.contains("ELEVATED"))
        assertTrue(assessment.phases.size >= 3)
        assertEquals("RECON_MATRIX", assessment.phases[0].codename)
        assertTrue(assessment.contingencyProtocols.isNotEmpty())
        assertTrue(assessment.rawSummary.contains("TACTICAL STRATEGIC MATRIX"))
    }

    @Test
    fun testTacticalToolExecution() = runBlocking {
        val tool = TacticalTool()
        val result = tool.execute("assess tactical combat readiness", toolContext)
        assertTrue(result.success)
        assertTrue(result.output.contains("TACTICAL STRATEGIC MATRIX"))
        assertTrue(result.metadata.containsKey("threatLevel"))
        assertTrue(result.metadata.containsKey("readiness"))
    }

    @Test
    fun testOrbCoreToolThemeSwitch() = runBlocking {
        val tool = OrbCoreTool()
        val result = tool.execute("switch to crimson combat mode", toolContext)
        assertTrue(result.success)
        assertEquals("THEME_SWITCH", result.metadata["action"])
        assertEquals(HologramTheme.JARVIS_CRIMSON, HologramThemeManager.getActiveTheme())
        assertTrue(result.output.contains("J.A.R.V.I.S. Combat Crimson"))
    }

    @Test
    fun testTacticalIntentClassification() {
        val intent = IntentClassifier.classify("JARVIS initiate combat protocol")
        assertEquals(ConversationIntent.DEVICE_ACTION, intent.intent)
        assertTrue(intent.requiresTool)
        assertEquals("Tactical", intent.suggestedToolName)

        val sitrepIntent = IntentClassifier.classify("give me a situation report")
        assertEquals("Tactical", sitrepIntent.suggestedToolName)
    }

    @Test
    fun testAIProviderTacticalRouting() = runBlocking {
        val provider = GeminiAIProvider(repository)
        val decision = provider.decideTool("run tactical analysis now", emptyList(), "")
        assertTrue(decision.useTool)
        assertEquals("Tactical", decision.toolName)

        val themeDecision = provider.decideTool("switch core theme to gold", emptyList(), "")
        assertTrue(themeDecision.useTool)
        assertEquals("OrbCore", themeDecision.toolName)
    }
}
