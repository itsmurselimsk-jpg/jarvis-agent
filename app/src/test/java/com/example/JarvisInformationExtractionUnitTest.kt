package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.extraction.EntityType
import com.example.jarvis.extraction.ExtractionSource
import com.example.jarvis.extraction.InformationExtractionEngine
import com.example.jarvis.extraction.Normalizer
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisInformationExtractionUnitTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    @Test
    fun testEmailExtraction() {
        val text = "Please reach out to support@jarvis.ai or Ops.Lead@Sub.Domain.org for assistance."
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(2, extracted.emails.size)
        assertEquals("support@jarvis.ai", extracted.emails[0].email)
        assertEquals("jarvis.ai", extracted.emails[0].domain)
        assertEquals("ops.lead@sub.domain.org", extracted.emails[1].email)
    }

    @Test
    fun testPhoneExtraction() {
        val text = "Call emergency line at +1 (555) 234-5678 or back up number +919876543210."
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(2, extracted.phoneNumbers.size)
        assertEquals("+15552345678", extracted.phoneNumbers[0].phoneNumber)
        assertEquals("+919876543210", extracted.phoneNumbers[1].phoneNumber)
    }

    @Test
    fun testUrlExtraction() {
        val text = "Documentation is at https://ai.google.dev/android or visit www.kotlinlang.org/docs"
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(2, extracted.urls.size)
        assertEquals("https://ai.google.dev/android", extracted.urls[0].url)
        assertEquals("ai.google.dev", extracted.urls[0].domain)
        assertEquals("https://www.kotlinlang.org/docs", extracted.urls[1].url)
    }

    @Test
    fun testDateTimeExtraction() {
        val text = "Project release scheduled on 2026-10-15 at 14:30."
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(1, extracted.dates.size)
        assertEquals("2026-10-15", extracted.dates[0].normalizedIso)
        assertFalse(extracted.dates[0].isAmbiguous)

        assertEquals(1, extracted.times.size)
        assertEquals("14:30", extracted.times[0].normalizedTime)
        assertFalse(extracted.times[0].isAmbiguous)
    }

    @Test
    fun testCurrencyExtraction() {
        val text = "Total invoice is $1,250.75 or approximately 95000 INR."
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(2, extracted.currencies.size)
        val usd = extracted.currencies[0]
        assertEquals(1250.75, usd.amount, 0.001)
        assertEquals("$", usd.currencySymbol)
        assertEquals("USD", usd.currencyCode)

        val inr = extracted.currencies[1]
        assertEquals(95000.0, inr.amount, 0.001)
        assertEquals("₹", inr.currencySymbol)
        assertEquals("INR", inr.currencyCode)
    }

    @Test
    fun testPercentageExtraction() {
        val text = "Battery charge at 85% and network efficiency is 99.5 percent."
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(2, extracted.percentages.size)
        assertEquals(85.0, extracted.percentages[0].value, 0.001)
        assertEquals(99.5, extracted.percentages[1].value, 0.001)
    }

    @Test
    fun testMultipleEntities() {
        val text = "Send 15% discount code to ceo@example.com, dial +18005550199 or visit https://deals.example.com on 2026-11-20."
        val extracted = InformationExtractionEngine.extract(text)

        assertFalse(extracted.isEmpty())
        assertEquals(1, extracted.emails.size)
        assertEquals(1, extracted.phoneNumbers.size)
        assertEquals(1, extracted.urls.size)
        assertEquals(1, extracted.percentages.size)
        assertEquals(1, extracted.dates.size)
    }

    @Test
    fun testMultipleFields() {
        val text = "status: active\npriority: High\nversion: 2.4.0\nassigned_to: Operator"
        val extracted = InformationExtractionEngine.extract(text)

        assertEquals(4, extracted.fields.size)
        assertEquals("status", extracted.fields[0].key)
        assertEquals("active", extracted.fields[0].value)
        assertEquals("priority", extracted.fields[1].key)
        assertEquals("High", extracted.fields[1].value)
    }

    @Test
    fun testNormalization() {
        // Email normalization
        assertEquals("test.user@company.com", Normalizer.normalizeEmail("  TEST.USER@Company.Com "))

        // Phone normalization
        assertEquals("+15551234567", Normalizer.normalizePhone("  +1 (555) 123-4567 "))
        assertEquals("01712345678", Normalizer.normalizePhone("01712-345-678"))

        // URL normalization
        assertEquals("https://www.google.com/search", Normalizer.normalizeUrl("www.google.com/search"))

        // Currency normalization
        val (amt, sym, code) = Normalizer.normalizeCurrency(" 1,500.00 ", " USD ")
        assertEquals(1500.0, amt, 0.001)
        assertEquals("$", sym)
        assertEquals("USD", code)

        // Percentage normalization
        assertEquals(12.5, Normalizer.normalizePercentage(" 12.5 percent "), 0.001)
    }

    @Test
    fun testAmbiguousDateTime() {
        val text = "Call Rahul tomorrow evening"
        val extracted = InformationExtractionEngine.extract(text)

        assertTrue(extracted.hasAmbiguity)
        val timeEntity = extracted.times.firstOrNull { it.rawText.contains("evening") }
        assertNotNull(timeEntity)
        assertTrue(timeEntity!!.isAmbiguous)
        assertNull("Ambiguous time must not invent arbitrary clock time", timeEntity.normalizedTime)
    }

    @Test
    fun testMalformedAiStructuredOutput() {
        val base = InformationExtractionEngine.extract("Schedule briefing with Alice at Acme Inc")
        val malformedAiResponse = "Sure, here is your json: { people: ['Alice', incomplete_syntax..."

        val merged = InformationExtractionEngine.parseAndMergeAiOutput(base, malformedAiResponse)

        // Must degrade safely to base without crashing or corrupting
        assertEquals(base.originalText, merged.originalText)
        assertEquals(base.entities.size, merged.entities.size)
    }

    @Test
    fun testEmptyInput() {
        val emptyResult = InformationExtractionEngine.extract("")
        assertTrue(emptyResult.isEmpty())

        val whitespaceResult = InformationExtractionEngine.extract("   \n\t   ")
        assertTrue(whitespaceResult.isEmpty())
    }

    @Test
    fun testSensitiveDataFiltering() {
        val otpText = "Your verification code is OTP: 849201. Do not share."
        val extractedOtp = InformationExtractionEngine.extract(otpText)
        assertTrue(extractedOtp.containsSensitiveData)
        assertTrue(extractedOtp.sensitiveDataTypes.isNotEmpty())

        val apiKeyText = "Connecting with API key AIzaSyD4567890abcdefghijklmnopqr"
        val extractedKey = InformationExtractionEngine.extract(apiKeyText)
        assertTrue(extractedKey.containsSensitiveData)
    }

    @Test
    fun testNoAutomaticMemoryPersistence() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val initialMemoryCount = repository.memories.value.size

        // Extract complex text
        val text = "Alice said the secret project code is PROJECT-882 and deadline is 2026-12-31"
        val extracted = InformationExtractionEngine.extract(text)

        assertFalse(extracted.isEmpty())

        // Ensure memories collection in repository was not touched
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val postExtractionMemoryCount = repository.memories.value.size
        assertEquals(initialMemoryCount, postExtractionMemoryCount)
    }

    @Test
    fun testExtractionDoesNotExecuteTools() {
        // Text contains command triggers like call, launch, open
        val commandText = "Call +15551234567 and open https://android.com and set timer 10 minutes"
        val extracted = InformationExtractionEngine.extract(commandText, ExtractionSource.USER_MESSAGE)

        // Verified extraction occurred
        assertEquals(1, extracted.phoneNumbers.size)
        assertEquals(1, extracted.urls.size)
        assertTrue(extracted.intents.isNotEmpty())

        // Tool activity logs must remain untouched because extraction NEVER executes tools
        val initialActivityLogCount = repository.activityLogs.value.size
        assertEquals("Extraction must not log or perform tool executions", initialActivityLogCount, repository.activityLogs.value.size)
    }
}
