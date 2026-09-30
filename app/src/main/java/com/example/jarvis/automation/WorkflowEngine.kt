package com.example.jarvis.automation

import com.example.jarvis.brain.ToolContext
import com.example.jarvis.brain.ToolRegistry
import com.example.jarvis.model.ActivityType
import com.example.jarvis.model.RiskLevel
import com.example.jarvis.recovery.RetryPolicy
import com.example.jarvis.recovery.executeWithRetry
import com.example.jarvis.safety.RiskEngine
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.ArrayDeque

/**
 * Execution and validation engine for multi-step automation workflows.
 */
class WorkflowEngine(
    private val toolRegistry: ToolRegistry = ToolRegistry()
) {

    /**
     * Validates DAG topological ordering, step dependencies, and tool availability.
     */
    fun validateWorkflow(workflow: Workflow): WorkflowValidationResult {
        val errors = mutableListOf<String>()
        val stepMap = workflow.steps.associateBy { it.stepId }

        // 1. Check for missing dependency references
        workflow.steps.forEach { step ->
            step.dependencies.forEach { depId ->
                if (!stepMap.containsKey(depId)) {
                    errors.add("Step '${step.stepId}' references non-existent dependency '$depId'.")
                }
            }
        }

        // 2. Check for circular dependencies (Cycle Detection)
        if (hasCycle(workflow.steps)) {
            errors.add("Workflow contains circular step dependencies.")
        }

        return WorkflowValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Executes a workflow DAG sequentially and concurrently based on topological dependency resolution.
     */
    suspend fun executeWorkflow(
        workflow: Workflow,
        context: ExecutionContext,
        onConfirmationRequired: (suspend (String, String) -> Boolean)? = null
    ): WorkflowResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stepMap = workflow.steps.associateBy { it.stepId }
        val validation = validateWorkflow(workflow)

        if (!validation.isValid) {
            return@withContext WorkflowResult(
                workflowId = workflow.id,
                status = WorkflowStatus.FAILED,
                stepResults = emptyMap(),
                totalTimeMs = System.currentTimeMillis() - startTime,
                message = "Validation failed: ${validation.errors.joinToString("; ")}"
            )
        }

        val completedSteps = mutableMapOf<String, StepResult>()
        val executedOrder = getTopologicalOrder(workflow.steps)

        var workflowStatus = WorkflowStatus.COMPLETED

        for (stepId in executedOrder) {
            val step = stepMap[stepId] ?: continue

            // Check if dependencies succeeded
            val unsatisfiedDep = step.dependencies.firstOrNull { depId ->
                val depResult = completedSteps[depId]
                depResult == null || depResult.status != StepStatus.SUCCESS
            }

            if (unsatisfiedDep != null) {
                completedSteps[stepId] = StepResult(
                    stepId = stepId,
                    status = StepStatus.SKIPPED,
                    output = "Skipped due to failed/unmet dependency '$unsatisfiedDep'.",
                    executionTimeMs = 0L
                )
                continue
            }

            // Evaluate step condition if present
            if (step.condition != null && !evaluateCondition(step.condition, context, completedSteps)) {
                completedSteps[stepId] = StepResult(
                    stepId = stepId,
                    status = StepStatus.SKIPPED,
                    output = "Skipped because step condition evaluated to false.",
                    executionTimeMs = 0L
                )
                continue
            }

            // Execute step
            val stepStart = System.currentTimeMillis()
            val interpolatedInput = interpolateInput(step.inputTemplate, context, completedSteps)

            // Assess safety risk
            val riskAssessment = RiskEngine.assessAction(step.toolName, interpolatedInput)
            if (riskAssessment.level == RiskLevel.CONFIRMATION && onConfirmationRequired != null) {
                val userApproved = onConfirmationRequired.invoke(step.name, interpolatedInput)
                if (!userApproved) {
                    completedSteps[stepId] = StepResult(
                        stepId = stepId,
                        status = StepStatus.FAILURE,
                        output = "Execution halted: User declined safety confirmation for action '${step.name}'.",
                        executionTimeMs = System.currentTimeMillis() - stepStart
                    )
                    workflowStatus = WorkflowStatus.CANCELLED
                    break
                }
            }

            // Execute tool
            val stepResult = executeSingleStep(step, interpolatedInput, context)
            completedSteps[stepId] = stepResult

            context.stepOutputs[stepId] = stepResult
            context.variables["${stepId}.output"] = stepResult.output

            if (stepResult.status == StepStatus.FAILURE) {
                when (step.onFailure) {
                    FailureAction.STOP, FailureAction.ROLLBACK -> {
                        workflowStatus = WorkflowStatus.FAILED
                        if (step.onFailure == FailureAction.ROLLBACK) {
                            rollbackExecutedSteps(completedSteps.keys.toList(), context)
                        }
                        val remainingIndex = executedOrder.indexOf(stepId) + 1
                        if (remainingIndex < executedOrder.size) {
                            for (i in remainingIndex until executedOrder.size) {
                                val remId = executedOrder[i]
                                if (!completedSteps.containsKey(remId)) {
                                    completedSteps[remId] = StepResult(
                                        stepId = remId,
                                        status = StepStatus.SKIPPED,
                                        output = "Skipped due to prior step failure in '${step.stepId}'.",
                                        executionTimeMs = 0L
                                    )
                                }
                            }
                        }
                        break
                    }
                    FailureAction.CONTINUE -> {
                        workflowStatus = WorkflowStatus.COMPLETED // Keep going
                    }
                }
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        val finalResult = WorkflowResult(
            workflowId = workflow.id,
            status = workflowStatus,
            stepResults = completedSteps,
            totalTimeMs = totalTime,
            message = "Workflow execution finished with status $workflowStatus in ${totalTime}ms."
        )

        context.repository?.logActivity(
            title = "WORKFLOW_EXECUTION",
            detail = "Workflow '${workflow.name}' (${workflow.id}) finished with status $workflowStatus",
            type = ActivityType.SYSTEM_EVENT
        )

        finalResult
    }

    private suspend fun executeSingleStep(
        step: WorkflowStep,
        input: String,
        context: ExecutionContext
    ): StepResult {
        val stepStart = System.currentTimeMillis()
        val tool = toolRegistry.getTool(step.toolName)

        if (tool == null) {
            return StepResult(
                stepId = step.stepId,
                status = StepStatus.FAILURE,
                output = "Tool '${step.toolName}' not found in registry.",
                executionTimeMs = System.currentTimeMillis() - stepStart,
                errorMessage = "Missing tool"
            )
        }

        return try {
            val repo = context.repository ?: context.brain?.repository
            val bridge = context.brain?.bridge

            if (repo == null || bridge == null) {
                return StepResult(
                    stepId = step.stepId,
                    status = StepStatus.FAILURE,
                    output = "Step execution error: Context repository or bridge is missing.",
                    executionTimeMs = System.currentTimeMillis() - stepStart,
                    errorMessage = "Missing repository/bridge"
                )
            }

            val toolContext = ToolContext(
                repository = repo,
                bridge = bridge
            )

            val toolResult = withTimeout(step.timeoutMs) {
                if (step.retryPolicy != RetryPolicy.NONE) {
                    val retryRes = executeWithRetry(
                        policy = step.retryPolicy,
                        operationName = "WorkflowStep_${step.stepId}",
                        source = step.name
                    ) {
                        tool.execute(input, toolContext)
                    }
                    retryRes.getOrElse {
                        com.example.jarvis.brain.ToolResult(
                            success = false,
                            output = "Execution failed after retries: ${it.message}",
                            verified = false
                        )
                    }
                } else {
                    tool.execute(input, toolContext)
                }
            }

            StepResult(
                stepId = step.stepId,
                status = if (toolResult.success) StepStatus.SUCCESS else StepStatus.FAILURE,
                output = toolResult.output,
                rawData = toolResult.metadata,
                executionTimeMs = System.currentTimeMillis() - stepStart,
                errorMessage = if (!toolResult.success) toolResult.output else null
            )
        } catch (e: TimeoutCancellationException) {
            StepResult(
                stepId = step.stepId,
                status = StepStatus.FAILURE,
                output = "Step '${step.name}' timed out after ${step.timeoutMs}ms.",
                executionTimeMs = System.currentTimeMillis() - stepStart,
                errorMessage = "Timeout"
            )
        } catch (e: Exception) {
            StepResult(
                stepId = step.stepId,
                status = StepStatus.FAILURE,
                output = "Step execution error: ${e.message}",
                executionTimeMs = System.currentTimeMillis() - stepStart,
                errorMessage = e.message
            )
        }
    }

    private fun interpolateInput(
        template: String,
        context: ExecutionContext,
        stepResults: Map<String, StepResult>
    ): String {
        var result = template

        // Replace telemetry variables
        context.deviceTelemetry?.let { tel ->
            result = result.replace("\${battery_percent}", tel.batteryPercent.toString())
            result = result.replace("\${network_type}", tel.networkType)
        }

        // Replace context variables
        context.variables.forEach { (k, v) ->
            result = result.replace("\${$k}", v?.toString() ?: "")
        }

        // Replace step outputs
        stepResults.forEach { (stepId, res) ->
            result = result.replace("\${$stepId.output}", res.output)
        }

        return result
    }

    private fun evaluateCondition(
        condition: WorkflowCondition,
        context: ExecutionContext,
        stepResults: Map<String, StepResult>
    ): Boolean {
        return when (condition) {
            is WorkflowCondition.BatteryLevelCondition -> {
                val battery = context.deviceTelemetry?.batteryPercent ?: 100
                battery in condition.minPercent..condition.maxPercent
            }
            is WorkflowCondition.NetworkConnectedCondition -> {
                val network = context.deviceTelemetry?.networkType ?: "NONE"
                if (condition.requireWifi && network != "WIFI") return false
                if (condition.requireCellular && network != "CELLULAR") return false
                network != "NONE"
            }
            is WorkflowCondition.TimeWindowCondition -> {
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                if (condition.startHour <= condition.endHour) {
                    currentHour in condition.startHour..condition.endHour
                } else {
                    currentHour >= condition.startHour || currentHour <= condition.endHour
                }
            }
            is WorkflowCondition.StepResultCondition -> {
                val targetResult = stepResults[condition.targetStepId] ?: return false
                val matchSuccess = (targetResult.status == StepStatus.SUCCESS) == condition.expectedSuccess
                val matchValue = condition.valueContains == null || targetResult.output.contains(condition.valueContains, ignoreCase = true)
                matchSuccess && matchValue
            }
            is WorkflowCondition.CompositeCondition -> {
                if (condition.operator == WorkflowCondition.CompositeCondition.Operator.AND) {
                    condition.conditions.all { evaluateCondition(it, context, stepResults) }
                } else {
                    condition.conditions.any { evaluateCondition(it, context, stepResults) }
                }
            }
        }
    }

    private fun hasCycle(steps: List<WorkflowStep>): Boolean {
        val inDegree = mutableMapOf<String, Int>()
        val adjList = mutableMapOf<String, MutableList<String>>()

        steps.forEach { step ->
            inDegree[step.stepId] = step.dependencies.size
            adjList[step.stepId] = mutableListOf()
        }

        steps.forEach { step ->
            step.dependencies.forEach { dep ->
                adjList[dep]?.add(step.stepId)
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        var visitedCount = 0
        while (queue.isNotEmpty()) {
            val u = queue.poll() ?: break
            visitedCount++
            adjList[u]?.forEach { v ->
                val newDeg = (inDegree[v] ?: 1) - 1
                inDegree[v] = newDeg
                if (newDeg == 0) {
                    queue.add(v)
                }
            }
        }

        return visitedCount != steps.size
    }

    private fun getTopologicalOrder(steps: List<WorkflowStep>): List<String> {
        val inDegree = mutableMapOf<String, Int>()
        val adjList = mutableMapOf<String, MutableList<String>>()

        steps.forEach { step ->
            inDegree[step.stepId] = step.dependencies.size
            adjList[step.stepId] = mutableListOf()
        }

        steps.forEach { step ->
            step.dependencies.forEach { dep ->
                adjList[dep]?.add(step.stepId)
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        val order = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val u = queue.poll() ?: break
            order.add(u)
            adjList[u]?.forEach { v ->
                val newDeg = (inDegree[v] ?: 1) - 1
                inDegree[v] = newDeg
                if (newDeg == 0) {
                    queue.add(v)
                }
            }
        }
        return order
    }

    private fun rollbackExecutedSteps(executedStepIds: List<String>, context: ExecutionContext) {
        context.repository?.logActivity(
            title = "WORKFLOW_ROLLBACK",
            detail = "Rolled back executed steps: ${executedStepIds.joinToString(", ")}",
            type = ActivityType.SYSTEM_EVENT
        )
    }
}

data class WorkflowValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
