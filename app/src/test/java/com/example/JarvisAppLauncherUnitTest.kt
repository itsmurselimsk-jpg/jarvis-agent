package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.intent.ConversationIntent
import com.example.jarvis.intent.IntentClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisAppLauncherUnitTest {

    private lateinit var context: Context
    private lateinit var bridge: AndroidBridge
    private lateinit var pm: PackageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        pm = context.packageManager
        bridge = AndroidBridge(context)
    }

    @Suppress("DEPRECATION")
    private fun registerApp(packageName: String, label: String, className: String = "$packageName.MainActivity") {
        val shadowPm = Shadows.shadowOf(pm)
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                this.name = className
                this.applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    this.flags = ApplicationInfo.FLAG_INSTALLED
                }
            }
            this.nonLocalizedLabel = label
        }

        // 1. General LAUNCHER category query
        val mainLauncherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        shadowPm.addResolveInfoForIntent(mainLauncherIntent, resolveInfo)

        // 2. Package specific launch intent
        val pkgLauncherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        shadowPm.addResolveInfoForIntent(pkgLauncherIntent, resolveInfo)
    }

    @Test
    fun testIntentClassificationAppLaunch() {
        val testQueries = listOf(
            "Facebook kholo",
            "open Facebook",
            "open WhatsApp",
            "open YouTube",
            "Chrome khol",
            "Instagram launch karo",
            "Spotify kholo",
            "Gmail open karo",
            "Calculator kholo",
            "Camera open karo",
            "Settings kholo",
            "Open Telegram"
        )

        for (query in testQueries) {
            val result = IntentClassifier.classify(query)
            assertEquals("Expected DEVICE_ACTION for '$query'", ConversationIntent.DEVICE_ACTION, result.intent)
            assertTrue("Expected requiresTool=true for '$query'", result.requiresTool)
            assertEquals("Expected AppLauncher tool for '$query'", "AppLauncher", result.suggestedToolName)
        }
    }

    @Test
    fun testIntentClassificationConversationalDoesNotInvokeLauncher() {
        val conversationalQueries = listOf(
            "Kya haal hai?",
            "How are you doing today",
            "Tell me a joke",
            "Ki sob khobor",
            "Bhalo achi"
        )

        for (query in conversationalQueries) {
            val result = IntentClassifier.classify(query)
            assertFalse("Conversational query '$query' should not require tool", result.requiresTool)
            assertFalse("Conversational query '$query' must not trigger AppLauncher", result.suggestedToolName == "AppLauncher")
        }
    }

    @Test
    fun testLaunchInstalledAppFacebook() {
        registerApp("com.facebook.katana", "Facebook")

        val (success, message) = bridge.launchAppByNameOrPackage("Facebook kholo")
        assertTrue("Expected launch success for Facebook", success)
        assertTrue("Expected message to contain Facebook", message.contains("Facebook"))

        val nextStartedIntent = Shadows.shadowOf(context as android.app.Application).nextStartedActivity
        assertNotNull("Expected started activity intent", nextStartedIntent)
        val targetPkg = nextStartedIntent.`package` ?: nextStartedIntent.component?.packageName
        assertEquals("com.facebook.katana", targetPkg)
    }

    @Test
    fun testLaunchInstalledAppOpenFacebook() {
        registerApp("com.facebook.katana", "Facebook")

        val (success, message) = bridge.launchAppByNameOrPackage("open Facebook")
        assertTrue(success)
        assertTrue(message.contains("Facebook"))
    }

    @Test
    fun testLaunchInstalledAppWhatsApp() {
        registerApp("com.whatsapp", "WhatsApp")

        val (success, message) = bridge.launchAppByNameOrPackage("open WhatsApp")
        assertTrue(success)
        assertTrue(message.contains("WhatsApp"))
    }

    @Test
    fun testLaunchInstalledAppYouTube() {
        registerApp("com.google.android.youtube", "YouTube")

        val (success, message) = bridge.launchAppByNameOrPackage("open YouTube")
        assertTrue(success)
        assertTrue(message.contains("YouTube"))
    }

    @Test
    fun testLaunchInstalledAppChrome() {
        registerApp("com.android.chrome", "Chrome")

        val (success, message) = bridge.launchAppByNameOrPackage("Chrome khol")
        assertTrue(success)
        assertTrue(message.contains("Chrome"))
    }

    @Test
    fun testLaunchInstalledAppInstagram() {
        registerApp("com.instagram.android", "Instagram")

        val (success, message) = bridge.launchAppByNameOrPackage("Instagram launch karo")
        assertTrue(success)
        assertTrue(message.contains("Instagram"))
    }

    @Test
    fun testLaunchArbitraryInstalledApp() {
        registerApp("com.custom.myapp", "My Custom Special App")

        val (success, message) = bridge.launchAppByNameOrPackage("Open My Custom Special App")
        assertTrue(success)
        assertTrue(message.contains("My Custom Special App"))
    }

    @Test
    fun testAppNotInstalled() {
        val (success, message) = bridge.launchAppByNameOrPackage("Open RandomNonExistentApp")
        assertFalse(success)
        assertEquals("Ye app installed nahi hai bhai.", message)
    }

    @Test
    fun testMultipleMatchingAppsAsksForClarification() {
        registerApp("com.meta.quest", "Meta Quest")
        registerApp("com.meta.horizon", "Meta Horizon")

        val (success, message) = bridge.launchAppByNameOrPackage("Open Meta")
        assertFalse(success)
        assertEquals("Kaunsa app kholun?", message)
    }

    @Test
    fun testHinglishHindiBanglishVariations() {
        registerApp("com.spotify.music", "Spotify")

        val variations = listOf(
            "Spotify kholo",
            "Spotify khol",
            "Spotify open karo",
            "Spotify launch karo",
            "Spotify khol de",
            "Spotify khol do"
        )

        for (v in variations) {
            val (success, message) = bridge.launchAppByNameOrPackage(v)
            assertTrue("Failed for variation '$v'", success)
            assertTrue("Message '$message' should indicate Spotify", message.contains("Spotify"))
        }
    }
}
