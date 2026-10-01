package com.jarvis.ai.brain

import com.jarvis.ai.ui.*
import com.jarvis.ai.ui.components.*

import com.jarvis.ai.bridge.AndroidBridge
import com.jarvis.ai.model.RiskLevel
import com.jarvis.ai.storage.JarvisRepository

data class ToolContext(
    val repository: JarvisRepository,
    val bridge: AndroidBridge,
    val activeVisionResult: com.jarvis.ai.vision.VisionResult? = null,
    val activeDocument: com.jarvis.ai.document.DocumentModel? = null,
    val activeDocumentSummary: com.jarvis.ai.document.DocumentSummary? = null,
    val activeFileAnalysis: com.jarvis.ai.document.FileAnalysisResult? = null,
    val previousDocument: com.jarvis.ai.document.DocumentModel? = null,
    val previousFileAnalysis: com.jarvis.ai.document.FileAnalysisResult? = null,
    val fileGenerationPipeline: com.jarvis.ai.generation.FileGenerationPipeline? = null,
    val aiProvider: com.jarvis.ai.provider.AIProvider? = null
)

data class ToolResult(
    val success: Boolean,
    val output: String,
    val visualDetail: String? = null,
    val requiresUserAction: Boolean = false,
    val verified: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
)

interface Tool {
    val name: String
    val description: String
    val riskLevel: RiskLevel
    val permissions: List<String>

    suspend fun execute(input: String, context: ToolContext): ToolResult

    suspend fun verify(result: ToolResult, context: ToolContext): Boolean {
        return result.success && result.verified
    }
}
