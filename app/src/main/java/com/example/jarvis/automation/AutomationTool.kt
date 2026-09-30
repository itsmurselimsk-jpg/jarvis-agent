package com.example.jarvis.automation

import com.example.jarvis.brain.Tool
import com.example.jarvis.brain.ToolContext
import com.example.jarvis.brain.ToolResult
import com.example.jarvis.model.RiskLevel
import java.util.Locale

/**
 * Agent tool exposing the Automation Orchestration Engine to AgentBrain.
 */
class AutomationTool(
    private val orchestrator: AutomationOrchestrator? = null
) : Tool {
    override val name: String = "AutomationEngine"
    override val description: String = "Orchestrates multi-step workflows, conditional task chains, and scheduled triggers."
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val permissions: List<String> = emptyList()

    override suspend fun execute(input: String, context: ToolContext): ToolResult {
        val lower = input.lowercase(Locale.ROOT).trim()

        if (orchestrator == null) {
            return ToolResult(
                success = false,
                output = "AutomationOrchestrator is not initialized in the current context.",
                verified = false
            )
        }

        return when {
            lower.contains("list") || lower.contains("show workflows") || lower.contains("all workflows") -> {
                val workflows = orchestrator.getAllWorkflows()
                val summary = if (workflows.isEmpty()) {
                    "No automation workflows currently registered."
                } else {
                    workflows.joinToString("\n\n") { wf ->
                        "• [${wf.id}] ${wf.name} (${wf.status})\n  Description: ${wf.description}\n  Steps: ${wf.steps.size} step(s)"
                    }
                }
                ToolResult(
                    success = true,
                    output = "REGISTERED AUTOMATION WORKFLOWS:\n\n$summary",
                    verified = true,
                    metadata = mapOf("workflowCount" to workflows.size.toString())
                )
            }

            lower.contains("run") || lower.contains("execute") || lower.contains("trigger") -> {
                val targetWorkflow = orchestrator.getAllWorkflows().firstOrNull { wf ->
                    lower.contains(wf.id.lowercase(Locale.ROOT)) || lower.contains(wf.name.lowercase(Locale.ROOT))
                } ?: orchestrator.getAllWorkflows().firstOrNull()

                if (targetWorkflow == null) {
                    return ToolResult(
                        success = false,
                        output = "No matching workflow found to execute.",
                        verified = false
                    )
                }

                val result = orchestrator.runWorkflow(targetWorkflow.id)
                val isSuccess = result.status == WorkflowStatus.COMPLETED

                ToolResult(
                    success = isSuccess,
                    output = "WORKFLOW EXECUTION RESULT:\n" +
                            "Workflow: ${targetWorkflow.name} (${targetWorkflow.id})\n" +
                            "Status: ${result.status}\n" +
                            "Execution Time: ${result.totalTimeMs}ms\n\n" +
                            "STEP SUMMARY:\n" +
                            result.stepResults.entries.joinToString("\n") { (stepId, stepRes) ->
                                "• $stepId: [${stepRes.status}] ${stepRes.output}"
                            },
                    verified = isSuccess,
                    metadata = mapOf(
                        "workflowId" to targetWorkflow.id,
                        "status" to result.status.name,
                        "totalTimeMs" to result.totalTimeMs.toString()
                    )
                )
            }

            else -> {
                // Default: list available workflows and commands
                val workflows = orchestrator.getAllWorkflows()
                ToolResult(
                    success = true,
                    output = "JARVIS Automation Engine Online.\n" +
                            "Registered Workflows: ${workflows.size}\n" +
                            "Commands: 'list workflows', 'run workflow <id/name>'",
                    verified = true
                )
            }
        }
    }
}
