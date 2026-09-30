package com.example.jarvis.brain

import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.search.deep.DeepResearchEngine
import com.example.jarvis.search.deep.DeepResearchResult
import com.example.jarvis.search.deep.ResearchBudget
import com.example.jarvis.search.deep.ResearchDepth
import com.example.jarvis.search.research.WebResearchRequest
import com.example.jarvis.search.research.WebSource

class DeepResearchTool(
    private val deepResearchEngine: DeepResearchEngine? = null
) : Tool {
    override val name = "DeepResearch"
    override val description = "Conducts structured multi-round deep research with sub-question planning, evidence extraction, contradiction detection, and citation provenance"
    override val riskLevel = RiskLevel.SAFE
    override val permissions = listOf("INTERNET")

    var customSourcesProvider: (suspend (WebResearchRequest) -> List<WebSource>)? = null

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val engine = deepResearchEngine ?: DeepResearchEngine(bridge = context.bridge)

        // Parse depth if specified
        val depth = when {
            input.contains("quick", ignoreCase = true) -> ResearchDepth.QUICK
            input.contains("thorough", ignoreCase = true) || input.contains("exhaustive", ignoreCase = true) || input.contains("deep", ignoreCase = true) -> ResearchDepth.DEEP
            else -> ResearchDepth.STANDARD
        }

        val result: DeepResearchResult = engine.executeDeepResearch(
            rawQuestion = input,
            depth = depth,
            budget = ResearchBudget(),
            aiProvider = context.aiProvider,
            customSourcesProvider = customSourcesProvider
        )

        context.repository.logActivity(
            "Deep Research Executed",
            "Researched: ${result.plan.normalizedQuestion} (Rounds: ${result.roundsCompleted}, Sources: ${result.sources.size}, Status: ${result.plan.status})",
            ActivityType.TOOL_EXECUTION
        )

        val output = if (result.formattedReport.isNotBlank()) {
            result.formattedReport
        } else {
            result.directAnswer
        }

        return ToolResult(
            success = result.success,
            output = output,
            visualDetail = result.sources.firstOrNull()?.url,
            verified = result.success,
            metadata = mapOf(
                "query" to result.plan.normalizedQuestion,
                "subQuestionsCount" to result.plan.subQuestions.size.toString(),
                "sourcesCount" to result.sources.size.toString(),
                "roundsCompleted" to result.roundsCompleted.toString(),
                "isPartial" to result.isPartial.toString(),
                "isOffline" to (!result.success && result.errorMessage?.contains("offline", ignoreCase = true) == true).toString()
            )
        )
    }
}
