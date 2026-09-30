package com.example.jarvis.memory

import android.util.Log
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.storage.db.MemoryEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.ln

/**
 * Advanced Long-Term Memory & Local RAG (Retrieval-Augmented Generation) Engine.
 *
 * Capabilities:
 *  1. Zero-dependency Hybrid Lexical + BM25 relevance scoring.
 *  2. "Remember this" and "Forget this" command parsing & execution.
 *  3. Dynamic Top-K memory injection directly into AgentBrain's planning loop.
 */
class LongTermRAGEngine(private val repository: JarvisRepository) {

    companion object {
        private const val TAG = "LongTermRAGEngine"
        private val STOP_WORDS = setOf(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "did", "do", "does", "doing", "don", "down", "during", "each", "few", "for",
            "from", "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself",
            "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
            "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once",
            "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own", "s", "same", "she",
            "should", "so", "some", "such", "t", "than", "that", "the", "their", "theirs", "them",
            "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
            "while", "who", "whom", "why", "will", "with", "you", "your", "yours", "yourself",
            "yaad", "rakhna", "bolo", "batao", "kya", "hai", "mera", "meri", "mere", "remember"
        )
    }

    sealed class MemoryCommandResult {
        data class Remembered(val fact: String, val category: String) : MemoryCommandResult()
        data class Forgotten(val target: String, val deletedCount: Int) : MemoryCommandResult()
        data object NotACommand : MemoryCommandResult()
    }

    /**
     * Inspects input for explicit memory persistence directives ("Remember that...", "Forget my...").
     */
    suspend fun processExplicitMemoryCommands(input: String): MemoryCommandResult {
        val lower = input.lowercase().trim()

        // 1. "Remember that / Remember this"
        val rememberPatterns = listOf(
            Regex("""(?i)^(remember that|remember this|remember|note that|keep in mind)\s*[:,-]?\s*(.+)"""),
            Regex("""(?i)^yaad rakhna ki?\s*[:,-]?\s*(.+)"""),
            Regex("""(?i)^mone rekho je?\s*[:,-]?\s*(.+)""")
        )

        for (pattern in rememberPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val fact = input.substring(match.groups.last()?.range?.first ?: 0).trim()
                if (fact.isNotBlank()) {
                    val category = classifyCategory(fact)
                    repository.addMemory(
                        title = extractTitle(fact),
                        content = fact,
                        category = category
                    )
                    return MemoryCommandResult.Remembered(fact, category)
                }
            }
        }

        // 2. "Forget that / Forget my"
        val forgetPatterns = listOf(
            Regex("""(?i)^(forget that|forget my|delete memory of|delete note on|remove memory of)\s*[:,-]?\s*(.+)"""),
            Regex("""(?i)^bhool jao ki?\s*[:,-]?\s*(.+)"""),
            Regex("""(?i)^bhule jao je?\s*[:,-]?\s*(.+)""")
        )

        for (pattern in forgetPatterns) {
            val match = pattern.find(lower)
            if (match != null) {
                val target = input.substring(match.groups.last()?.range?.first ?: 0).trim()
                if (target.isNotBlank()) {
                    val count = deleteMatchingMemories(target)
                    return MemoryCommandResult.Forgotten(target, count)
                }
            }
        }

        return MemoryCommandResult.NotACommand
    }

    /**
     * Local BM25-based Retrieval Augmented Generation (RAG).
     * Retrieves the top-K most relevant long-term memories for a given query.
     */
    suspend fun retrieveRelevantMemories(query: String, topK: Int = 3): String {
        val allMemories = repository.memories.firstOrNull() ?: emptyList()
        if (allMemories.isEmpty()) return ""

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return ""

        // Calculate BM25 scores
        val scoredMemories = allMemories.map { memory ->
            val docTokens = tokenize("${memory.title} ${memory.content} ${memory.category}")
            val score = computeBm25Score(queryTokens, docTokens, allMemories.size)
            Pair(memory, score)
        }
        .filter { it.second > 0.05f }
        .sortedByDescending { it.second }
        .take(topK)

        if (scoredMemories.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("### RECALLED LONG-TERM MEMORY (LOCAL RAG):")
        scoredMemories.forEachIndexed { idx, pair ->
            val mem = pair.first
            sb.appendLine("${idx + 1}. [${mem.category.uppercase()}] ${mem.title}: ${mem.content}")
        }
        return sb.toString().trimEnd()
    }

    private fun deleteMatchingMemories(target: String): Int {
        var count = 0
        val targetTokens = tokenize(target)
        val memories = repository.memories.value

        for (mem in memories) {
            val memTokens = tokenize("${mem.title} ${mem.content}")
            val matches = targetTokens.count { it in memTokens }
            if (matches >= maxOf(1, targetTokens.size / 2)) {
                repository.deleteMemory(mem.id)
                count++
            }
        }
        return count
    }

    private fun computeBm25Score(queryTokens: List<String>, docTokens: List<String>, totalDocs: Int): Float {
        val k1 = 1.2f
        val b = 0.75f
        val avgDocLen = 20.0f
        val docLen = docTokens.size.toFloat()

        var totalScore = 0.0f
        for (qToken in queryTokens) {
            val freq = docTokens.count { it == qToken }
            if (freq > 0) {
                // Approximate IDF
                val idf = ln((totalDocs + 1.0) / 1.5).toFloat()
                val tf = (freq * (k1 + 1.0f)) / (freq + k1 * (1.0f - b + b * (docLen / avgDocLen)))
                totalScore += idf * tf
            }
        }
        return totalScore
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("""[^a-zA-Z0-9\u0980-\u09FF\u0900-\u097F]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() && it !in STOP_WORDS }
    }

    private fun classifyCategory(fact: String): String {
        val lower = fact.lowercase()
        return when {
            lower.contains("like") || lower.contains("prefer") || lower.contains("favorite") || lower.contains("pasand") -> "Preference"
            lower.contains("birthday") || lower.contains("anniversary") || lower.contains("meeting") || lower.contains("event") -> "Event"
            lower.contains("password") || lower.contains("pin") || lower.contains("code") || lower.contains("key") -> "Security"
            lower.contains("work") || lower.contains("office") || lower.contains("company") || lower.contains("boss") -> "Work"
            else -> "Fact"
        }
    }

    private fun extractTitle(fact: String): String {
        val words = fact.split(" ")
        return if (words.size <= 5) fact else words.take(5).joinToString(" ") + "..."
    }
}
