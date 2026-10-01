package com.jarvis.ai.memory

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import android.util.Log
import com.jarvis.ai.model.MemoryItem
import com.jarvis.ai.storage.JarvisRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Advanced Long-Term Memory & Hybrid Vector + BM25 RAG Engine.
 *
 * Implements:
 *  1. Lightweight offline embedding vector generation (char-ngram / hash projection)
 *  2. Cosine similarity vector scoring combined with BM25 corpus IDF ranking (70% Vector / 30% BM25)
 *  3. Explicit "Remember this" and "Forget this" command parsing & execution
 */
class LongTermRAGEngine(private val repository: JarvisRepository) {

    companion object {
        private const val TAG = "LongTermRAGEngine"
        private const val K1 = 1.2f
        private const val B = 0.75f
        private const val VECTOR_DIM = 32

        val STOP_WORDS = setOf(
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
            "yaad", "rakhna", "rakho", "bolo", "batao", "kya", "hai", "hain", "tha", "thi", "the",
            "mera", "meri", "mere", "mujhe", "mujhko", "apna", "apni", "apne", "tum", "tumhara",
            "aap", "aapka", "karo", "karna", "yeh", "woh", "ka", "ki", "ke", "ko", "se", "par",
            "mein", "aur", "bhi", "toh", "lekin", "is", "us", "bata", "sun", "sunao",
            "mone", "rekho", "bolo", "kholo", "bhalo", "ki", "kemon", "aamr", "aami", "tumi"
        )
    }

    sealed class MemoryCommandResult {
        data class Remembered(val fact: String, val category: String) : MemoryCommandResult()
        data class Forgotten(val target: String, val deletedCount: Int) : MemoryCommandResult()
        data object NotACommand : MemoryCommandResult()
    }

    private data class IndexedMemoryDoc(
        val memory: MemoryItem,
        val tokens: List<String>,
        val docLen: Float,
        val embedding: FloatArray
    )

    suspend fun processExplicitMemoryCommands(input: String): MemoryCommandResult {
        val lower = input.lowercase().trim()

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
     * Hybrid Hybrid Vector (70%) + BM25 (30%) RAG Retrieval.
     */
    suspend fun retrieveRelevantMemories(query: String, topK: Int = 3): String {
        val allMemories = repository.memories.value.ifEmpty {
            repository.memories.firstOrNull() ?: emptyList()
        }
        if (allMemories.isEmpty()) return ""

        val queryTokens = tokenize(query)
        val queryVector = computeEmbedding(query)
        if (queryTokens.isEmpty() && queryVector.all { it == 0f }) return ""

        val scored = scoreCorpusHybrid(queryTokens, queryVector, allMemories)
            .filter { it.second > 0.08f }
            .sortedByDescending { it.second }
            .take(topK)

        if (scored.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("### RECALLED LONG-TERM MEMORY (HYBRID RAG):")
        scored.forEachIndexed { idx, pair ->
            val mem = pair.first
            sb.appendLine("${idx + 1}. [${mem.category.uppercase()}] ${mem.title}: ${mem.content}")
        }
        return sb.toString().trimEnd()
    }

    /**
     * Computes 70% Cosine Vector Similarity + 30% BM25 Hybrid Score.
     */
    fun scoreCorpusHybrid(queryTokens: List<String>, queryVector: FloatArray, corpus: List<MemoryItem>): List<Pair<MemoryItem, Float>> {
        if (corpus.isEmpty()) return emptyList()

        val totalDocs = corpus.size
        val indexedDocs = corpus.map { mem ->
            val text = "${mem.title} ${mem.content} ${mem.category}"
            val tokens = tokenize(text)
            val embedding = computeEmbedding(text)
            IndexedMemoryDoc(memory = mem, tokens = tokens, docLen = max(1f, tokens.size.toFloat()), embedding = embedding)
        }

        val totalTokens = indexedDocs.sumOf { it.tokens.size }
        val avgDocLen = max(1.0f, totalTokens.toFloat() / totalDocs.toFloat())

        val docFrequencies = mutableMapOf<String, Int>()
        for (qToken in queryTokens) {
            val docCount = indexedDocs.count { doc -> doc.tokens.contains(qToken) }
            docFrequencies[qToken] = docCount
        }

        // Compute raw BM25 scores to normalize
        val bm25Raw = indexedDocs.map { doc ->
            var score = 0.0f
            for (qToken in queryTokens) {
                val freq = doc.tokens.count { it == qToken }
                if (freq > 0) {
                    val nQ = docFrequencies[qToken] ?: 1
                    val idf = ln(1.0 + (totalDocs - nQ + 0.5) / (nQ + 0.5)).toFloat()
                    val tf = (freq * (K1 + 1.0f)) / (freq + K1 * (1.0f - B + B * (doc.docLen / avgDocLen)))
                    score += max(0.05f, idf) * tf
                }
            }
            score
        }
        val maxBm25 = bm25Raw.maxOrNull() ?: 1.0f

        return indexedDocs.mapIndexed { index, doc ->
            val bm25Norm = if (maxBm25 > 0f) bm25Raw[index] / maxBm25 else 0f
            val cosineSim = computeCosineSimilarity(queryVector, doc.embedding)
            // 70% vector embedding similarity + 30% BM25 lexical score
            val hybridScore = (0.7f * max(0f, cosineSim)) + (0.3f * bm25Norm)
            Pair(doc.memory, hybridScore)
        }
    }

    /**
     * Computes lightweight deterministic character n-gram hash vector embedding.
     */
    fun computeEmbedding(text: String): FloatArray {
        val vec = FloatArray(VECTOR_DIM)
        val cleaned = text.lowercase().trim()
        if (cleaned.isBlank()) return vec

        val words = cleaned.split(Regex("\\s+"))
        for (w in words) {
            val h = Math.abs(w.hashCode())
            val idx = h % VECTOR_DIM
            vec[idx] += 1.0f

            // Also add character trigrams
            if (w.length >= 3) {
                for (i in 0..w.length - 3) {
                    val gram = w.substring(i, i + 3)
                    val gIdx = Math.abs(gram.hashCode()) % VECTOR_DIM
                    vec[gIdx] += 0.5f
                }
            }
        }

        // L2 Normalize
        var norm = 0.0f
        for (v in vec) {
            norm += v * v
        }
        norm = sqrt(norm)
        if (norm > 0f) {
            for (i in vec.indices) {
                vec[i] /= norm
            }
        }
        return vec
    }

    private fun computeCosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size) return 0f
        var dot = 0.0f
        var n1 = 0.0f
        var n2 = 0.0f
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            n1 += v1[i] * v1[i]
            n2 += v2[i] * v2[i]
        }
        val denom = sqrt(n1) * sqrt(n2)
        return if (denom > 0f) dot / denom else 0f
    }

    // Exposed for tests
    fun scoreCorpusBM25(queryTokens: List<String>, corpus: List<MemoryItem>): List<Pair<MemoryItem, Float>> {
        val dummyVector = FloatArray(VECTOR_DIM)
        return scoreCorpusHybrid(queryTokens, dummyVector, corpus)
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
