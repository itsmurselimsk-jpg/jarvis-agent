package com.example

import android.graphics.Bitmap
import com.example.jarvis.vision.HeuristicVisionProvider
import com.example.jarvis.vision.ImageMetadata
import com.example.jarvis.vision.LocalVisionProvider
import com.example.jarvis.vision.SensitiveDataFilter
import com.example.jarvis.vision.VisionActionType
import com.example.jarvis.vision.VisionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisVisionUnitTest {

    @Test
    fun testSensitiveDataFilterDetectsOtpAndCredentials() {
        val otpText = "Your verification code is 849201. Valid for 5 minutes. Do not share."
        assertTrue(SensitiveDataFilter.containsSensitiveData(otpText))
        val entities = SensitiveDataFilter.detectSensitiveEntities(otpText)
        assertTrue("Expected OTP in entities: $entities", entities.any { it.contains("OTP") })

        val cardText = "Payment processed on Visa 4532 0150 1234 5678 expiring 12/28."
        assertTrue(SensitiveDataFilter.containsSensitiveData(cardText))
        val cardEntities = SensitiveDataFilter.detectSensitiveEntities(cardText)
        assertTrue("Expected Card in entities: $cardEntities", cardEntities.any { it.contains("Card") })

        val safeText = "Flight AI-204 to New Delhi is boarding at Gate 14. Departure at 15:30."
        assertFalse(SensitiveDataFilter.containsSensitiveData(safeText))
        assertTrue(SensitiveDataFilter.detectSensitiveEntities(safeText).isEmpty())
    }

    @Test
    fun testSensitiveDataFilterRedaction() {
        val text = "User password is Password#2026. Send OTP 492018 to phone."
        val redacted = SensitiveDataFilter.redactSensitiveData(text)

        assertFalse("Should not contain raw password", redacted.contains("Password#2026"))
        assertFalse("Should not contain raw OTP", redacted.contains("492018"))
        assertTrue("Should contain redacted token placeholder", redacted.contains("REDACTED"))
    }

    @Test
    fun testExtractDerivedActions() {
        val mixedContent = """
            Invoice #9821
            Contact support at support@jarviscore.org or call +1-800-555-0199 for billing.
            Visit customer portal at https://secure.starkindustries.com/portal
            Your one-time authorization code is 582910.
        """.trimIndent()

        val actions = SensitiveDataFilter.extractActions(mixedContent)
        assertFalse("Actions should not be empty", actions.isEmpty())

        // Check phone number actions (Dial and Search)
        val dialAction = actions.find { it.type == VisionActionType.DIAL_PHONE }
        assertNotNull("Dial phone action should be present", dialAction)
        assertTrue(dialAction!!.payload.contains("8005550199") || dialAction.payload.contains("+1-800-555-0199"))

        // Check URL action
        val urlAction = actions.find { it.type == VisionActionType.OPEN_URL }
        assertNotNull("URL action should be present", urlAction)
        assertEquals("https://secure.starkindustries.com/portal", urlAction!!.payload)

        // Check Email action
        val emailAction = actions.find { it.type == VisionActionType.SEND_EMAIL }
        assertNotNull("Email action should be present", emailAction)
        assertEquals("support@jarviscore.org", emailAction!!.payload)

        // Check OTP action with sensitive flag
        val otpAction = actions.find { it.label.contains("OTP") }
        assertNotNull("OTP action should be present", otpAction)
        assertTrue("OTP action should be marked sensitive", otpAction!!.isSensitive)
    }

    @Test
    fun testVisionResultDataModel() {
        val metadata = ImageMetadata(
            width = 1920,
            height = 1080,
            fileName = "receipt.png",
            fileSizeFormatted = "240 KB"
        )
        val result = VisionResult(
            success = true,
            extractedText = "Total Amount: $42.50",
            blocks = emptyList(),
            metadata = metadata,
            processingTimeMs = 45L
        )

        assertTrue(result.success)
        assertEquals("Total Amount: $42.50", result.extractedText)
        assertEquals(1920, result.metadata?.width)
        assertFalse(result.containsSensitiveData)

        val failure = VisionResult.failure("Corrupt image header", metadata)
        assertFalse(failure.success)
        assertEquals("Corrupt image header", failure.errorMessage)
        assertEquals("", failure.extractedText)
    }

    @Test
    fun testLocalVisionProviderFallback() = runBlocking {
        val localProvider = LocalVisionProvider(visionEngine = null)
        assertEquals("Local Optical Analyzer", localProvider.providerName)
        assertFalse(localProvider.isMultimodalCapable)

        // Create test bitmap (Robolectric shadow bitmap)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val analysis = localProvider.analyzeImage(bitmap, "Scan receipt")

        assertTrue("Analysis should contain optical telemetry", analysis.contains("JARVIS OPTICAL TELEMETRY"))
        assertTrue("Analysis should contain resolution", analysis.contains("100x100"))
        assertTrue("Analysis should state fallback notice", analysis.contains("Full image understanding requires a configured vision AI provider"))
    }

    @Test
    fun testHeuristicVisionProviderTelemetry() {
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val telemetry = HeuristicVisionProvider.analyzeLocal(bitmap, "Audit luminance")

        assertTrue(telemetry.contains("200x100 px"))
        assertTrue(telemetry.contains("Luminance Metrics"))
        assertTrue(telemetry.contains("Audit luminance"))
    }
}
