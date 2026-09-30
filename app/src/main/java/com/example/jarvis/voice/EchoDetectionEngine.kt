package com.example.jarvis.voice

import android.util.Log
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

/**
 * Acoustic Echo Detection & Self-Listening Shield (inspired by isair/jarvis echo_detection.py)
 * 
 * Prevents JARVIS from listening to its own TTS voice feedback, preventing infinite
 * self-response loops while still allowing the user to barge in or issue follow-ups.
 */
class EchoDetectionEngine {

    companion object {
        private const val TAG = "EchoDetectionEngine"
        private const val COOLDOWN_WINDOW_MS = 1800L

        private fun safeLog(msg: String) {
            try {
                Log.d(TAG, msg)
            } catch (_: Throwable) {
                // Ignore in unmocked JVM unit tests
            }
        }
    }

    private var lastTtsText: String = ""
    private var lastTtsFinishTime: Long = 0L
    private var isTtsPlaying: Boolean = false

    fun notifyTtsStarted(spokenText: String) {
        lastTtsText = spokenText.trim()
        isTtsPlaying = true
    }

    fun notifyTtsFinished() {
        lastTtsFinishTime = System.currentTimeMillis()
        isTtsPlaying = false
    }

    /**
     * Determines whether the transcribed text from microphone should be rejected as an echo.
     */
    fun shouldRejectAsEcho(heardText: String): Boolean {
        if (heardText.isBlank() || lastTtsText.isBlank()) return false
        val now = System.currentTimeMillis()

        // 1. If TTS is actively playing and heard text matches TTS
        if (isTtsPlaying) {
            val similarity = computeSimilarity(heardText, lastTtsText)
            if (similarity >= 0.70) {
                safeLog("Rejected as active TTS echo (sim: $similarity): \"$heardText\"")
                return true
            }
        }

        // 2. In cooldown window right after TTS finished
        if (lastTtsFinishTime > 0 && (now - lastTtsFinishTime) < COOLDOWN_WINDOW_MS) {
            val similarity = computeSimilarity(heardText, lastTtsText)
            if (similarity >= 0.78) {
                safeLog("Rejected as cooldown echo (sim: $similarity): \"$heardText\"")
                return true
            }
        }

        return false
    }

    /**
     * If the microphone captured the end of a TTS prompt followed by user speech,
     * strips the leading echo and preserves the user's intended query.
     */
    fun cleanupLeadingEcho(heardText: String): String {
        if (heardText.isBlank() || lastTtsText.isBlank()) return heardText

        val heardWords = normalizeWords(heardText)
        val ttsWords = normalizeWords(lastTtsText)

        if (heardWords.size < 3 || ttsWords.isEmpty()) return heardText

        var maxOverlap = 0
        val maxCheck = min(ttsWords.size, heardWords.size - 1)

        for (i in maxCheck downTo 2) {
            val ttsSlice = ttsWords.takeLast(i)
            val heardSlice = heardWords.take(i)
            if (ttsSlice == heardSlice) {
                maxOverlap = i
                break
            }
        }

        return if (maxOverlap >= 2 && maxOverlap < heardWords.size) {
            val rawSplit = heardText.trim().split(Regex("\\s+"))
            if (rawSplit.size > maxOverlap) {
                val cleaned = rawSplit.drop(maxOverlap).joinToString(" ")
                Log.d(TAG, "Cleaned leading echo from user input. Remaining: \"$cleaned\"")
                cleaned
            } else {
                heardText
            }
        } else {
            heardText
        }
    }

    private fun normalizeWords(text: String): List<String> {
        val clean = text.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9\\s]"), "")
        return clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    private fun computeSimilarity(a: String, b: String): Double {
        val wordsA = normalizeWords(a).toSet()
        val wordsB = normalizeWords(b).toSet()

        if (wordsA.isEmpty() || wordsB.isEmpty()) return 0.0

        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size

        // Jaccard token similarity + substring boost
        val jaccard = intersection.toDouble() / union.toDouble()
        val normA = wordsA.joinToString(" ")
        val normB = wordsB.joinToString(" ")

        return if (normB.contains(normA) && normA.length > 5) {
            max(jaccard, 0.85)
        } else {
            jaccard
        }
    }
}
