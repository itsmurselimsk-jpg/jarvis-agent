package com.example.jarvis.brain

import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.protocol.StarkProtocolEngine

/**
 * Stark Protocol Tool for J.A.R.V.I.S.
 * Executes executive operational protocols: Morning Briefing, Night Standby, Secure Perimeter, and Power Surge.
 */
class StarkProtocolTool : Tool {
    override val name = "StarkProtocol"
    override val description = "Executes executive J.A.R.V.I.S. operational protocols including Morning Briefing (weather, telemetry, agenda), Night Standby (volume damping, power preservation), Secure Perimeter (privacy audit, sensor shielding), and Arc Reactor Power Surge."
    override val riskLevel = RiskLevel.SAFE
    override val permissions = emptyList<String>()

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val protocolType = StarkProtocolEngine.StarkProtocolType.fromInput(input)
        val result = StarkProtocolEngine.executeProtocol(
            protocolType = protocolType,
            bridge = context.bridge,
            repository = context.repository,
            context = context.bridge.getApplicationContext()
        )

        val outputFormatted = StringBuilder()
        outputFormatted.appendLine("═══ ${result.summaryBadge} ═══")
        outputFormatted.appendLine(result.speechText)
        outputFormatted.appendLine()
        outputFormatted.appendLine("Telemetry Matrix:")
        result.telemetryPoints.forEach { point ->
            outputFormatted.appendLine("• $point")
        }

        return ToolResult(
            success = true,
            output = outputFormatted.toString().trimEnd(),
            verified = true,
            metadata = mapOf(
                "protocol" to result.protocol.code,
                "badge" to result.summaryBadge,
                "speechText" to result.speechText
            )
        )
    }
}
