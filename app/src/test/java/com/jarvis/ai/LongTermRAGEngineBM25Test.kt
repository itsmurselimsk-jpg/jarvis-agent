package com.jarvis.ai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jarvis.ai.memory.LongTermRAGEngine
import com.jarvis.ai.model.MemoryItem
import com.jarvis.ai.storage.JarvisRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LongTermRAGEngineBM25Test {

    @Test
    fun testBm25ScoringWithCorpusIdf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = JarvisRepository(context)
        val engine = LongTermRAGEngine(repository)

        val corpus = listOf(
            MemoryItem(id = "1", title = "Kotlin Coroutines", content = "Kotlin coroutines provide asynchronous programming for Android apps.", category = "Work"),
            MemoryItem(id = "2", title = "Favorite Food", content = "Sir prefers biryani and masala chai for evening snack.", category = "Preference"),
            MemoryItem(id = "3", title = "Project Architecture", content = "JARVIS uses MVVM architecture with Jetpack Compose UI.", category = "Work")
        )

        val queryTokens = engine.tokenize("biryani food")
        val scored = engine.scoreCorpusBM25(queryTokens, corpus)

        assertTrue(scored.isNotEmpty())
        val topMatch = scored.maxByOrNull { it.second }?.first
        assertEquals("Favorite Food", topMatch?.title)
    }
}
