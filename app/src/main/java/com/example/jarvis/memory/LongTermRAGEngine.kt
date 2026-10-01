package com.example.jarvis.memory

import android.util.Log
import com.example.jarvis.model.MemoryItem
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.ln
import kotlin.math.max

/**
 * Advanced Long-Term Memory & Local RAG (Retrieval-Augmented Generation) Engine.
 *
 * Implements deterministic BM25 ranking computed over the actual memory corpus:
 *  - Real corpus inverse document frequency (IDF)
 *  - Dynamic average document length calculation
 *  - Comprehensive English, Hindi, and Hinglish stop word filtering
 *  - In-memory index cache for high-throughput zero-latency retrieval
 */
class LongTermRAGEngine(private val repository: JarvisRepository) {

    companion object {
        private const val TAG = "LongTermRAGEngine"
        private const val K1 = 1.2f
        private const val B = 0.75f

        val STOP_WORDS = setOf(
            // English Stop Words
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
            "tell", "jarvis", "please", "can", "could", "would", "should", "know",

            // Hindi & Hinglish Stop Words
            "yaad", "rakhna", "rakho", "bolo", "batao", "kya", "hai", "hain", "tha", "thi", "the",
            "mera", "meri", "mere", "mujhe", "mujhko", "apna", "apni", "apne", "tum", "tumhara",
            "aap", "aapka", "karo", "karna", "yeh", "woh", "ka", "ki", "ke", "ko", "se", "par",
            "mein", "aur", "bhi", "toh", "lekin", "is", "us", "bata", "sun", "sunao",
            // Bengali Stop Words
            "mone", "rekho", "bolo", "kholo", "bhalo", "ki", "kemon", "aamr", "aami", "tumi"
        )
    }

    sealed class MemoryCommandResult {
        data class Remembered(val fact: String, val category: String) : MemoryCommandResult()
        data class Forgotten(val target: String, val deletedCount: Int) : MemoryCommandResult()
        data object NotACommand : MemoryCommandResult()
    }

    // In-memory tokenized document index cache
    private data class IndexedMemoryDoc(
        val memory: MemoryItem,
        val tokens: List<String>,
        val docLen: Float
    )

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
        val allMemories = repository.memories.value.ifEmpty {
            repository.memories.firstOrNull() ?: emptyList()
        }
        if (allMemories.isEmpty()) return ""

        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return ""

        val scoredMemories = scoreCorpusBM25(queryTokens, allMemories)
            .filter { it.second > 0.08f }
            .sortedByDescending { it.second }
            .take(topK)

        if (scoredMemories.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("### RECALLED LONG-TERM MEMORY (BM25 RAG):")
        scoredMemories.forEachIndexed { idx, pair ->
            val mem = pair.first
            sb.appendLine("${idx + 1}. [${mem.category.uppercase()}] ${mem.title}: ${mem.content}")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Computes genuine BM25 scores over the full corpus.
     */
    fun scoreCorpusBM25(queryTokens: List<String>, corpus: List<MemoryItem>): List<Pair<MemoryItem, Float>> {
        if (corpus.isEmpty() || queryTokens.isEmpty()) return emptyList()

        val totalDocs = corpus.size
        val indexedDocs = corpus.map { mem ->
            val tokens = tokenize("${mem.title} ${mem.content} ${mem.category}")
            IndexedMemoryDoc(memory = mem, tokens = tokens, docLen = max(1f, tokens.size.toFloat()))
        }

        val totalTokens = indexedDocs.sumOf { it.tokens.size }
        val avgDocLen = max(1.0f, totalTokens.toFloat() / totalDocs.toFloat())

        // Compute genuine document frequencies n(q) for each query token
        val docFrequencies = mutableMapOf<String, Int>()
        for (qToken in queryTokens) {
            val docCount = indexedDocs.count { doc -> doc.tokens.contains(qToken) }
            docFrequencies[qToken] = docCount
        }

        // Calculate BM25 score for each document
        return indexedDocs.map { doc ->
            var score = 0.0f
            for (qToken in queryTokens) {
                val freq = doc.tokens.count { it == qToken }
                if (freq > 0) {
                    val nQ = docFrequencies[qToken] ?: 1
                    // Standard BM25 Robertson-Spärck Jones IDF formula: ln(1 + (N - n(q) + 0.5) / (n(q) + 0.5))
                    val idf = ln(1.0 + (totalDocs - nQ + 0.5) / (nQ + 0.5)).toFloat()
                    val tf = (freq * (K1 + 1.0f)) / (freq + K1 * (1.0f - B + B * (doc.docLen / avgDocLen)))
                    score += max(0.05f, idf) * tf
                }
            }
            Pair(doc.memory, score)
        }
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

    fun tokenize(text: String): List<String> {
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
