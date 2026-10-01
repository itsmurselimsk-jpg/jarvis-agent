package com.jarvis.ai.brain

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import com.jarvis.ai.model.ActivityType
import com.jarvis.ai.model.RiskLevel
import com.jarvis.ai.search.research.ResearchResponseFormatter
import com.jarvis.ai.search.research.WebResearchResult
import com.jarvis.ai.search.research.WebResearchService
import com.jarvis.ai.search.research.WebSource
import com.jarvis.ai.search.research.WebResearchRequest

class WebResearchTool(
    private val webResearchService: WebResearchService? = null
) : Tool {
    override val name = "WebResearch"
    override val description = "Conducts safe, verified web research with multi-source corroboration, conflict detection, and citations"
    override val riskLevel = RiskLevel.SAFE
    override val permissions = listOf("INTERNET")

    // Optional custom source provider hook for unit testing or custom search engines
    var customSourcesProvider: (suspend (WebResearchRequest) -> List<WebSource>)? = null

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val service = webResearchService ?: WebResearchService(bridge = context.bridge)
        val result: WebResearchResult = service.research(
            rawQuery = input,
            aiProvider = context.aiProvider,
            customSourcesProvider = customSourcesProvider
        )

        context.repository.logActivity(
            "Web Research Formulated",
            "Researched: ${result.normalizedQuery} (Status: ${result.verificationStatus.displayName}, Sources: ${result.sources.size})",
            ActivityType.TOOL_EXECUTION
        )

        val output = if (result.formattedOutput.isNotBlank()) {
            result.formattedOutput
        } else {
            ResearchResponseFormatter.format(result)
        }

        return ToolResult(
            success = result.success,
            output = output,
            visualDetail = result.sources.firstOrNull()?.url,
            verified = result.success,
            metadata = mapOf(
                "query" to result.normalizedQuery,
                "sourcesCount" to result.sources.size.toString(),
                "verificationStatus" to result.verificationStatus.name,
                "isOffline" to (!result.success && result.errorMessage?.contains("offline", ignoreCase = true) == true).toString()
            )
        )
    }
}
