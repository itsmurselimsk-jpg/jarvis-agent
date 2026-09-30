package com.example.jarvis.automation

import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.storage.JarvisRepository
import com.example.jarvis.tasks.JarvisAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Top-level manager connecting Workflows, Triggers, AlarmScheduler, and AgentBrain.
 */
class AutomationOrchestrator(
    private val repository: JarvisRepository,
    private val bridge: AndroidBridge,
    private val brain: AgentBrain,
    private val workflowEngine: WorkflowEngine = WorkflowEngine(brain.registry)
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeWorkflows = ConcurrentHashMap<String, Workflow>()
    private val _workflowsState = MutableStateFlow<List<Workflow>>(emptyList())
    val workflowsState: StateFlow<List<Workflow>> = _workflowsState.asStateFlow()

    init {
        registerDefaultWorkflows()
    }

    private fun registerDefaultWorkflows() {
        val systemHealthCheckWorkflow = Workflow(
            id = "wf_system_health",
            name = "System Health Diagnostics",
            description = "Checks battery, network, and active tasks, then generates a summary report.",
            trigger = Trigger.ManualTrigger(),
            steps = listOf(
                WorkflowStep(
                    stepId = "s1_telemetry",
                    name = "Read Device Telemetry",
                    toolName = "DeviceInfo",
                    inputTemplate = "extract status"
                ),
                WorkflowStep(
                    stepId = "s2_report",
                    name = "Generate Summary Report",
                    toolName = "FileGeneration",
                    inputTemplate = "save as markdown called system_health_report.md with content \${s1_telemetry.output}",
                    dependencies = listOf("s1_telemetry")
                )
            )
        )

        registerWorkflow(systemHealthCheckWorkflow)
    }

    fun registerWorkflow(workflow: Workflow) {
        activeWorkflows[workflow.id] = workflow
        syncWorkflows()

        // Handle scheduling if workflow has a ScheduledTrigger
        if (workflow.trigger is Trigger.ScheduledTrigger && workflow.status == WorkflowStatus.ACTIVE) {
            scheduleWorkflowTrigger(workflow, workflow.trigger)
        }
    }

    fun getWorkflow(id: String): Workflow? = activeWorkflows[id]

    fun getAllWorkflows(): List<Workflow> = activeWorkflows.values.toList()

    fun updateWorkflowStatus(id: String, status: WorkflowStatus) {
        val existing = activeWorkflows[id] ?: return
        val updated = existing.copy(status = status, updatedAt = System.currentTimeMillis())
        activeWorkflows[id] = updated
        syncWorkflows()
    }

    suspend fun runWorkflow(
        workflowId: String,
        onConfirmationRequired: (suspend (String, String) -> Boolean)? = null
    ): WorkflowResult {
        val workflow = activeWorkflows[workflowId]
            ?: return WorkflowResult(
                workflowId = workflowId,
                status = WorkflowStatus.FAILED,
                stepResults = emptyMap(),
                totalTimeMs = 0L,
                message = "Workflow '$workflowId' not found."
            )

        val updatedRunning = workflow.copy(status = WorkflowStatus.RUNNING)
        activeWorkflows[workflowId] = updatedRunning
        syncWorkflows()

        val telemetry = bridge.telemetry.value
        val context = ExecutionContext(
            workflowId = workflowId,
            deviceTelemetry = telemetry,
            repository = repository,
            brain = brain
        )

        val result = workflowEngine.executeWorkflow(updatedRunning, context, onConfirmationRequired)

        val finalStatus = if (result.status == WorkflowStatus.COMPLETED) WorkflowStatus.ACTIVE else result.status
        val finishedWorkflow = updatedRunning.copy(
            status = finalStatus,
            lastExecutedAt = System.currentTimeMillis(),
            executionHistoryCount = updatedRunning.executionHistoryCount + 1
        )
        activeWorkflows[workflowId] = finishedWorkflow
        syncWorkflows()

        return result
    }

    private fun scheduleWorkflowTrigger(workflow: Workflow, trigger: Trigger.ScheduledTrigger) {
        val triggerTimeMs = trigger.runAtTimeMillis
            ?: (System.currentTimeMillis() + (trigger.intervalMs ?: 3600000L))

        com.example.jarvis.tasks.JarvisAlarmScheduler.scheduleTaskReminder(
            context = bridge.getApplicationContext(),
            taskId = "wf_${workflow.id}",
            title = "Workflow: ${workflow.name}",
            triggerAtMillis = triggerTimeMs
        )
    }

    private fun syncWorkflows() {
        _workflowsState.value = activeWorkflows.values.toList()
    }
}
