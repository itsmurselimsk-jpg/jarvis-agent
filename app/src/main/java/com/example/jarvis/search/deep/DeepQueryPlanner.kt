package com.example.jarvis.search.deep

import com.example.jarvis.provider.AIProvider
import com.example.jarvis.vision.SensitiveDataFilter
import java.util.Locale

object DeepQueryPlanner {

    private val PREFIX_CLEANERS = listOf(
        "do deep research on",
        "do deep research about",
        "deep research on",
        "deep research about",
        "deep research",
        "research this thoroughly and explain",
        "research this thoroughly",
        "research thoroughly",
        "investigate this topic",
        "investigate and analyze",
        "investigate topic",
        "investigate",
        "give me a detailed research report on",
        "give me a detailed research report about",
        "give me a detailed research report",
        "detailed research report on",
        "compare multiple sources on",
        "compare multiple sources for",
        "compare multiple sources",
        "find evidence for this claim:",
        "find evidence for this claim",
        "find evidence for",
        "research and verify this claim",
        "research and verify this",
        "research and verify",
        "search the web for",
        "search the web about",
        "search the web",
        "search for",
        "research about",
        "research on",
        "research"
    )

    /**
     * Plans a structured deep research strategy with bounded sub-questions.
     */
    suspend fun planResearch(
        rawQuestion: String,
        depth: ResearchDepth = ResearchDepth.STANDARD,
        budget: ResearchBudget = ResearchBudget(),
        aiProvider: AIProvider? = null
    ): ResearchPlan {
        // 1. Sanitize sensitive credentials first
        val sanitized = SensitiveDataFilter.redactSensitiveData(rawQuestion)

        // 2. Normalize question string
        var normalized = sanitized.trim()
        for (prefix in PREFIX_CLEANERS) {
            if (normalized.startsWith(prefix, ignoreCase = true)) {
                normalized = normalized.substring(prefix.length).trim()
                break
            }
        }
        normalized = normalized.trimStart(':', '-', ' ', '?', '!', '"', '\'')
            .trimEnd(':', '-', ' ', '?', '!', '"', '\'')
            .trim()

        if (normalized.isBlank()) {
            normalized = sanitized.ifBlank { "General Inquiry" }
        }

        // 3. Determine sub-question count based on depth & budget
        val targetSubQuestionCount = when (depth) {
            ResearchDepth.QUICK -> minOf(2, budget.maxSubQuestions)
            ResearchDepth.STANDARD -> minOf(3, budget.maxSubQuestions)
            ResearchDepth.DEEP -> minOf(4, budget.maxSubQuestions)
        }

        // 4. Try AI-assisted sub-question formulation if available
        var subQuestions: List<ResearchSubQuestion>? = null
        if (aiProvider != null) {
            try {
                subQuestions = planWithAI(normalized, depth, targetSubQuestionCount, aiProvider)
            } catch (_: Exception) {
                // Fall back cleanly to deterministic planner
            }
        }

        if (subQuestions.isNullOrEmpty()) {
            subQuestions = planDeterministically(normalized, targetSubQuestionCount, budget)
        }

        // Enforce budget ceiling
        val boundedSubQuestions = subQuestions.take(budget.maxSubQuestions)
        val allQueries = boundedSubQuestions.flatMap { it.searchQueries }.distinct()

        val requiredEvidenceList = boundedSubQuestions.map { "Verified evidence for: ${it.purpose}" }

        return ResearchPlan(
            originalQuestion = rawQuestion,
            normalizedQuestion = normalized,
            subQuestions = boundedSubQuestions,
            searchQueries = allQueries,
            requiredEvidence = requiredEvidenceList,
            researchDepth = depth,
            status = ResearchPlanStatus.PLANNED
        )
    }

    /**
     * Deterministic, rule-based sub-question generator covering technical, comparative, and factual inquiries.
     */
    fun planDeterministically(
        normalized: String,
        targetCount: Int,
        budget: ResearchBudget
    ): List<ResearchSubQuestion> {
        val lower = normalized.lowercase(Locale.ROOT)
        val subQuestions = mutableListOf<ResearchSubQuestion>()

        if (lower.contains("vs") || lower.contains("versus") || lower.contains("compare") || lower.contains("difference")) {
            // Comparative inquiry
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-1",
                    question = "What are the core specifications and current architectures being compared in: $normalized?",
                    purpose = "Establish baseline architecture and specifications",
                    searchQueries = listOf("$normalized specifications architecture", "$normalized official documentation").take(budget.maxQueriesPerSubQuestion)
                )
            )
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-2",
                    question = "What are the key functional differentiators, performance metrics, and feature contrasts?",
                    purpose = "Identify key functional and technical differences",
                    searchQueries = listOf("$normalized major differences comparison", "$normalized performance benchmarks").take(budget.maxQueriesPerSubQuestion)
                )
            )
            if (targetCount >= 3) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-3",
                        question = "What are the supported platforms, ecosystem compatibility, and security implications?",
                        purpose = "Analyze ecosystem support and security characteristics",
                        searchQueries = listOf("$normalized platform support security", "$normalized compatibility constraints").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
            if (targetCount >= 4) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-4",
                        question = "What are the documented tradeoffs, known limitations, and official roadmaps?",
                        purpose = "Document known limitations and expert consensus",
                        searchQueries = listOf("$normalized limitations drawbacks", "$normalized developer consensus").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
        } else if (lower.contains("security") || lower.contains("vulnerability") || lower.contains("cve") || lower.contains("hack") || lower.contains("breach")) {
            // Security / vulnerability inquiry
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-1",
                    question = "What is the official disclosure, CVE description, or advisory for: $normalized?",
                    purpose = "Retrieve official advisory and severity classification",
                    searchQueries = listOf("$normalized security advisory official", "$normalized CVE details").take(budget.maxQueriesPerSubQuestion)
                )
            )
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-2",
                    question = "What systems, components, or versions are affected, and what is the technical mechanism?",
                    purpose = "Determine affected versions and impact scope",
                    searchQueries = listOf("$normalized affected versions impact", "$normalized technical analysis").take(budget.maxQueriesPerSubQuestion)
                )
            )
            if (targetCount >= 3) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-3",
                        question = "What official mitigations, security patches, or defensive recommendations have been published?",
                        purpose = "Identify published patches and remediation steps",
                        searchQueries = listOf("$normalized patch mitigation release", "$normalized recommended actions").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
        } else if (lower.contains("android") || lower.contains("kotlin") || lower.contains("compose") || lower.contains("flutter") || lower.contains("ios")) {
            // Mobile / Platform technical inquiry
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-1",
                    question = "What is the official release timeline, API level, and core architecture for: $normalized?",
                    purpose = "Identify release metadata and architectural foundations",
                    searchQueries = listOf("$normalized release date API features", "$normalized developer documentation").take(budget.maxQueriesPerSubQuestion)
                )
            )
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-2",
                    question = "What major capabilities, UI/UX enhancements, and runtime behavior changes are introduced?",
                    purpose = "Catalogue major feature additions and framework changes",
                    searchQueries = listOf("$normalized new features overview", "$normalized behavior changes").take(budget.maxQueriesPerSubQuestion)
                )
            )
            if (targetCount >= 3) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-3",
                        question = "What privacy, security, and hardware requirements are modified?",
                        purpose = "Examine security policies and hardware constraints",
                        searchQueries = listOf("$normalized privacy security changes", "$normalized device requirements").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
            if (targetCount >= 4) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-4",
                        question = "What are the migration guidelines, deprecations, and developer adoption hurdles?",
                        purpose = "Highlight deprecations and migration requirements",
                        searchQueries = listOf("$normalized migration guide deprecations", "$normalized developer preview").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
        } else {
            // General / Science / Factual inquiry
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-1",
                    question = "What is the primary definition, history, and official baseline for: $normalized?",
                    purpose = "Establish verified definitions and historical context",
                    searchQueries = listOf("$normalized overview definition", "$normalized official documentation").take(budget.maxQueriesPerSubQuestion)
                )
            )
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-2",
                    question = "What are the key verified developments, empirical findings, and data points?",
                    purpose = "Collect empirical findings and verified facts",
                    searchQueries = listOf("$normalized key findings data", "$normalized recent research").take(budget.maxQueriesPerSubQuestion)
                )
            )
            if (targetCount >= 3) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-3",
                        question = "What are the primary perspectives, independent consensus, and known limitations?",
                        purpose = "Evaluate consensus, criticisms, and limitations",
                        searchQueries = listOf("$normalized analysis consensus", "$normalized limitations challenges").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
            if (targetCount >= 4) {
                subQuestions.add(
                    ResearchSubQuestion(
                        id = "sub-4",
                        question = "What are the future projections, ongoing initiatives, or unresolved questions?",
                        purpose = "Synthesize forward outlook and open questions",
                        searchQueries = listOf("$normalized future outlook roadmap", "$normalized open challenges").take(budget.maxQueriesPerSubQuestion)
                    )
                )
            }
        }

        return subQuestions.take(targetCount)
    }

    private suspend fun planWithAI(
        normalized: String,
        depth: ResearchDepth,
        targetCount: Int,
        aiProvider: AIProvider
    ): List<ResearchSubQuestion>? {
        val systemPrompt = """
            You are JARVIS Deep Research Planner.
            Decompose the following inquiry into exactly $targetCount distinct, focused research sub-questions.
            For each sub-question, output:
            Q: [Focused Sub-Question]
            P: [Specific Purpose of this sub-question]
            S: [Search Query 1] | [Search Query 2]
            Do not include conversational banter.
        """.trimIndent()

        val prompt = "Inquiry: $normalized\nDepth: ${depth.name}\nSub-questions needed: $targetCount"
        val response = aiProvider.generateResponse(prompt, systemPrompt) { _: String -> }
        if (response.isBlank()) return null

        val subQuestions = mutableListOf<ResearchSubQuestion>()
        var currentQ = ""
        var currentP = ""
        var currentS = mutableListOf<String>()
        var index = 1

        response.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Q:", ignoreCase = true) -> {
                    if (currentQ.isNotBlank()) {
                        subQuestions.add(
                            ResearchSubQuestion(
                                id = "sub-$index",
                                question = currentQ,
                                purpose = currentP.ifBlank { "Investigate $currentQ" },
                                searchQueries = if (currentS.isNotEmpty()) currentS else listOf(currentQ)
                            )
                        )
                        index++
                        currentP = ""
                        currentS = mutableListOf()
                    }
                    currentQ = trimmed.substring(2).trim()
                }
                trimmed.startsWith("P:", ignoreCase = true) -> {
                    currentP = trimmed.substring(2).trim()
                }
                trimmed.startsWith("S:", ignoreCase = true) -> {
                    val queries = trimmed.substring(2).split("|").map { it.trim() }.filter { it.isNotBlank() }
                    currentS.addAll(queries)
                }
            }
        }

        if (currentQ.isNotBlank()) {
            subQuestions.add(
                ResearchSubQuestion(
                    id = "sub-$index",
                    question = currentQ,
                    purpose = currentP.ifBlank { "Investigate $currentQ" },
                    searchQueries = if (currentS.isNotEmpty()) currentS else listOf(currentQ)
                )
            )
        }

        return if (subQuestions.isNotEmpty()) subQuestions else null
    }
}
