package com.jarvis.ai.memory

import java.util.Locale

/**
 * Memory Recall Gate (inspired by isair/jarvis memory/recall_gate.py)
 * 
 * Determines whether long-term memory retrieval and Knowledge Graph enrichment
 * are relevant for the current user query without bloating prompt context.
 */
object RecallGatingEngine {

    private val STOPWORDS = setOf(
        "a", "an", "the", "and", "or", "but", "if", "then", "is", "are", "was",
        "were", "be", "been", "do", "does", "did", "have", "has", "had", "of",
        "in", "on", "at", "to", "for", "with", "by", "from", "it", "this", "that",
        "kya", "hai", "bhai", "bol", "kar", "mein", "ko", "se", "aur", "ki", "ka"
    )

    private val EXPLICIT_MEMORY_KEYWORDS = listOf(
        "remember", "memory", "yaad", "pichli", "pehle", "past", "history",
        "what do you know", "mere baare", "amar somporke", "who am i", "my name",
        "my project", "mera project", "favorite", "routine", "notes", "diary"
    )

    /**
     * Returns true if memory retrieval should be activated for this query.
     */
    fun shouldRecall(query: String, recentDialogue: List<Pair<String, String>>): Boolean {
        val lower = query.lowercase(Locale.ROOT).trim()

        // 1. Explicit memory queries must always recall
        if (EXPLICIT_MEMORY_KEYWORDS.any { lower.contains(it) }) {
            return true
        }

        // 2. Pure small greetings ("hi", "ok", "cool", "bye") don't need heavy memory search
        if (lower in listOf("hi", "hello", "hey", "ok", "theek hai", "bye", "good night", "shob thik")) {
            return false
        }

        // 3. Simple immediate device toggles don't need memory lookup ("turn on torch", "flashlight off")
        if (lower.startsWith("torch") || lower.startsWith("flashlight") || lower.startsWith("volume")) {
            return false
        }

        // 4. Default: Fail-open to ensure contextual awareness
        return true
    }
}
