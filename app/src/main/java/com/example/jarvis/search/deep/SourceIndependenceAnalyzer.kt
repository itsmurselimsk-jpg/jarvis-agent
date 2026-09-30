package com.example.jarvis.search.deep

import com.example.jarvis.search.research.SourceQualitySignal
import com.example.jarvis.search.research.WebSource
import java.util.Locale

object SourceIndependenceAnalyzer {

    data class IndependenceReport(
        val independentSources: List<WebSource>,
        val duplicateClusters: Map<String, List<WebSource>>,
        val syndicatedCount: Int,
        val independentCount: Int
    )

    /**
     * Evaluates source independence and flags duplicate, copied, or syndicated sources.
     */
    fun analyzeIndependence(sources: List<WebSource>): IndependenceReport {
        if (sources.isEmpty()) {
            return IndependenceReport(emptyList(), emptyMap(), 0, 0)
        }

        val duplicateClusters = mutableMapOf<String, MutableList<WebSource>>()
        val processedIds = mutableSetOf<String>()
        val classifiedSources = mutableListOf<WebSource>()

        for (i in sources.indices) {
            val sourceA = sources[i]
            if (processedIds.contains(sourceA.id)) continue

            val cluster = mutableListOf(sourceA)
            processedIds.add(sourceA.id)

            for (j in (i + 1) until sources.size) {
                val sourceB = sources[j]
                if (processedIds.contains(sourceB.id)) continue

                if (isDuplicateOrSyndicated(sourceA, sourceB)) {
                    cluster.add(sourceB)
                    processedIds.add(sourceB.id)
                }
            }

            if (cluster.size > 1) {
                duplicateClusters[sourceA.id] = cluster
                // Mark primary in cluster as representative, others as secondary syndication
                val representative = cluster.first().let { src ->
                    val signals = src.credibilitySignals.toMutableList()
                    if (!signals.contains(SourceQualitySignal.INDEPENDENT_CORROBORATION)) {
                        signals.add(SourceQualitySignal.INDEPENDENT_CORROBORATION)
                    }
                    src.copy(credibilitySignals = signals)
                }
                classifiedSources.add(representative)

                for (secondary in cluster.drop(1)) {
                    val signals = secondary.credibilitySignals.toMutableList()
                    signals.remove(SourceQualitySignal.INDEPENDENT_CORROBORATION)
                    if (!signals.contains(SourceQualitySignal.SECONDARY_REPORTING)) {
                        signals.add(SourceQualitySignal.SECONDARY_REPORTING)
                    }
                    classifiedSources.add(secondary.copy(credibilitySignals = signals))
                }
            } else {
                val signals = sourceA.credibilitySignals.toMutableList()
                if (!signals.contains(SourceQualitySignal.INDEPENDENT_CORROBORATION)) {
                    signals.add(SourceQualitySignal.INDEPENDENT_CORROBORATION)
                }
                classifiedSources.add(sourceA.copy(credibilitySignals = signals))
            }
        }

        val syndicatedCount = classifiedSources.count {
            it.credibilitySignals.contains(SourceQualitySignal.SECONDARY_REPORTING)
        }
        val independentCount = classifiedSources.size - syndicatedCount

        return IndependenceReport(
            independentSources = classifiedSources,
            duplicateClusters = duplicateClusters,
            syndicatedCount = syndicatedCount,
            independentCount = independentCount
        )
    }

    /**
     * Checks if two sources share the same domain, identical text snippets, or explicit syndication attribution.
     */
    fun isDuplicateOrSyndicated(a: WebSource, b: WebSource): Boolean {
        // 1. Same domain
        if (a.domain.equals(b.domain, ignoreCase = true) && a.domain.isNotBlank()) {
            return true
        }

        // 2. High textual snippet similarity (Jaccard token similarity)
        val sim = calculateSnippetSimilarity(a.snippet, b.snippet)
        if (sim >= 0.60f) {
            return true
        }

        // 3. Explicit wire / syndication markers
        val textA = a.snippet.lowercase(Locale.ROOT)
        val textB = b.snippet.lowercase(Locale.ROOT)

        val wireKeywords = listOf(
            "reuters reported", "associated press reported", "according to reuters",
            "according to ap", "originally published on", "reprinted with permission",
            "syndicated from", "via pr newswire", "business wire reports"
        )

        for (wire in wireKeywords) {
            if (textA.contains(wire) && textB.contains(wire)) {
                return true
            }
        }

        return false
    }

    /**
     * Computes word-level Jaccard index between two text snippets.
     */
    fun calculateSnippetSimilarity(textA: String, textB: String): Float {
        val tokensA = textA.lowercase(Locale.ROOT)
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .toSet()
        val tokensB = textB.lowercase(Locale.ROOT)
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .toSet()

        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0f

        val intersection = tokensA.intersect(tokensB).size
        val union = tokensA.union(tokensB).size
        if (union == 0) return 0f

        return intersection.toFloat() / union.toFloat()
    }
}
