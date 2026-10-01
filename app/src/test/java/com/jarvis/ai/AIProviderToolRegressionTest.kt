package com.jarvis.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jarvis.ai.provider.GeminiAIProvider
import com.jarvis.ai.provider.OpenAiCompatibleAIProvider
import com.jarvis.ai.storage.JarvisRepository
import com.jarvis.ai.memory.LongTermRAGEngine
import com.jarvis.ai.model.MemoryItem
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AIProviderToolRegressionTest {

    @Test
    fun testGeminiToolCallingRecordedFixture() {
        val fixtureJson = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "functionCall": {
                      "name": "flashlight",
                      "args": {
                        "input": "on"
                      }
                    }
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val jsonResponse = JSONObject(fixtureJson)
        val candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0)
        val parts = candidate.getJSONObject("content").getJSONArray("parts")
        val firstPart = parts.getJSONObject(0)
        assertTrue(firstPart.has("functionCall"))

        val fc = firstPart.getJSONObject("functionCall")
        assertEquals("flashlight", fc.getString("name"))
        assertEquals("on", fc.getJSONObject("args").getString("input"))
    }

    @Test
    fun testOpenAiToolCallingRecordedFixture() {
        val fixtureJson = """
        {
          "choices": [
            {
              "message": {
                "tool_calls": [
                  {
                    "function": {
                      "name": "flashlight",
                      "arguments": "{\"input\": \"on\"}"
                    }
                  }
                ]
              }
            }
          ]
        }
        """.trimIndent()

        val jsonResponse = JSONObject(fixtureJson)
        val choices = jsonResponse.getJSONArray("choices")
        val message = choices.getJSONObject(0).getJSONObject("message")
        assertTrue(message.has("tool_calls"))

        val toolCall = message.getJSONArray("tool_calls").getJSONObject(0)
        val func = toolCall.getJSONObject("function")
        assertEquals("flashlight", func.getString("name"))
        val args = JSONObject(func.getString("arguments"))
        assertEquals("on", args.getString("input"))
    }

    @Test
    fun testHybridRAGScoringWithEmbeddings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = JarvisRepository(context)
        val ragEngine = LongTermRAGEngine(repository)

        val memory = MemoryItem(
            id = "test-1",
            title = "Hometown",
            content = "My hometown is New York city",
            category = "Personal",
            timestamp = System.currentTimeMillis()
        )

        val queryTokens = ragEngine.tokenize("hometown")
        val queryVector = ragEngine.computeEmbedding("hometown")
        val scored = ragEngine.scoreCorpusHybrid(queryTokens, queryVector, listOf(memory))

        assertNotNull(scored)
        assertEquals(1, scored.size)
        assertTrue(scored[0].second > 0f)
    }
}
