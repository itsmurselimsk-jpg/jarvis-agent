package com.example.jarvis.automation

import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.DeviceTelemetry
import com.example.jarvis.recovery.RetryPolicy
import com.example.jarvis.storage.JarvisRepository

/**
 * Status of an Automation Workflow.
 */
enum class WorkflowStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    WAITING_CONFIRMATION
}

/**
 * Action to take when a step in a workflow fails.
 */
enum class FailureAction {
    STOP,
    CONTINUE,
    ROLLBACK
}

/**
 * Trigger definition for initiating a workflow.
 */
sealed class Trigger {
    data class ManualTrigger(
        val triggeredBy: String = "User"
    ) : Trigger()

    data class ScheduledTrigger(
        val cronExpression: String? = null,
        val intervalMs: Long? = null,
        val runAtTimeMillis: Long? = null,
        val repeat: Boolean = false
    ) : Trigger()

    data class EventTrigger(
        val eventType: String, // e.g., "BATTERY_LOW", "WIFI_CONNECTED", "TASK_DUE", "FILE_CREATED"
        val parameters: Map<String, String> = emptyMap()
    ) : Trigger()
}

/**
 * Condition evaluation rules for steps or triggers.
 */
sealed class WorkflowCondition {
    data class BatteryLevelCondition(
        val minPercent: Int = 0,
        val maxPercent: Int = 100
    ) : WorkflowCondition()

    data class NetworkConnectedCondition(
        val requireWifi: Boolean = false,
        val requireCellular: Boolean = false
    ) : WorkflowCondition()

    data class TimeWindowCondition(
        val startHour: Int, // 0-23
        val endHour: Int   // 0-23
    ) : WorkflowCondition()

    data class StepResultCondition(
        val targetStepId: String,
        val expectedSuccess: Boolean = true,
        val valueContains: String? = null
    ) : WorkflowCondition()

    data class CompositeCondition(
        val operator: Operator,
        val conditions: List<WorkflowCondition>
    ) : WorkflowCondition() {
        enum class Operator { AND, OR }
    }
}

/**
 * Individual step within a workflow DAG.
 */
data class WorkflowStep(
    val stepId: String,
    val name: String,
    val toolName: String,
    val inputTemplate: String,
    val dependencies: List<String> = emptyList(), // stepIds that must complete before this step
    val condition: WorkflowCondition? = null,
    val retryPolicy: RetryPolicy = RetryPolicy.NONE,
    val timeoutMs: Long = 20000L,
    val onFailure: FailureAction = FailureAction.STOP
)

/**
 * Full workflow definition.
 */
data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val trigger: Trigger = Trigger.ManualTrigger(),
    val steps: List<WorkflowStep>,
    val status: WorkflowStatus = WorkflowStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastExecutedAt: Long? = null,
    val executionHistoryCount: Int = 0
)

/**
 * State and variable context carried through a workflow execution.
 */
data class ExecutionContext(
    val workflowId: String,
    val variables: MutableMap<String, Any?> = mutableMapOf(),
    val stepOutputs: MutableMap<String, StepResult> = mutableMapOf(),
    val deviceTelemetry: DeviceTelemetry? = null,
    val repository: JarvisRepository? = null,
    val brain: AgentBrain? = null
)

/**
 * Execution status of an individual step.
 */
enum class StepStatus {
    SUCCESS,
    FAILURE,
    SKIPPED,
    WAITING_CONFIRMATION
}

/**
 * Result of executing an individual step.
 */
data class StepResult(
    val stepId: String,
    val status: StepStatus,
    val output: String,
    val rawData: Map<String, Any?> = emptyMap(),
    val executionTimeMs: Long = 0L,
    val errorMessage: String? = null
)

/**
 * Result of executing an entire workflow.
 */
data class WorkflowResult(
    val workflowId: String,
    val status: WorkflowStatus,
    val stepResults: Map<String, StepResult>,
    val totalTimeMs: Long,
    val message: String
)
