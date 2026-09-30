package com.example.jarvis.brain

import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.storage.JarvisRepository

data class ToolContext(
    val repository: JarvisRepository,
    val bridge: AndroidBridge,
    val activeVisionResult: com.example.jarvis.vision.VisionResult? = null,
    val activeDocument: com.example.jarvis.document.DocumentModel? = null,
    val activeDocumentSummary: com.example.jarvis.document.DocumentSummary? = null,
    val activeFileAnalysis: com.example.jarvis.document.FileAnalysisResult? = null,
    val previousDocument: com.example.jarvis.document.DocumentModel? = null,
    val previousFileAnalysis: com.example.jarvis.document.FileAnalysisResult? = null,
    val fileGenerationPipeline: com.example.jarvis.generation.FileGenerationPipeline? = null,
    val aiProvider: com.example.jarvis.provider.AIProvider? = null
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
