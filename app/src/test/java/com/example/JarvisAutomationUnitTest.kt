package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.automation.AutomationOrchestrator
import com.example.jarvis.automation.ExecutionContext
import com.example.jarvis.automation.FailureAction
import com.example.jarvis.automation.StepStatus
import com.example.jarvis.automation.Trigger
import com.example.jarvis.automation.Workflow
import com.example.jarvis.automation.WorkflowCondition
import com.example.jarvis.automation.WorkflowEngine
import com.example.jarvis.automation.WorkflowStatus
import com.example.jarvis.automation.WorkflowStep
import com.example.jarvis.brain.AgentBrain
import com.example.jarvis.bridge.AndroidBridge
import com.example.jarvis.model.DeviceTelemetry
import com.example.jarvis.provider.JarvisUnifiedAIProvider
import com.example.jarvis.storage.JarvisRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JarvisAutomationUnitTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository
    private lateinit var bridge: AndroidBridge
    private lateinit var brain: AgentBrain
    private lateinit var workflowEngine: WorkflowEngine
    private lateinit var orchestrator: AutomationOrchestrator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        bridge = AndroidBridge(context)
        brain = AgentBrain(
            repository = repository,
            bridge = bridge,
            aiProvider = JarvisUnifiedAIProvider(repository),
            onConfirmationRequired = {}
        )
        workflowEngine = WorkflowEngine(brain.registry)
        orchestrator = AutomationOrchestrator(repository, bridge, brain, workflowEngine)
    }

    // 1. DAG VALIDATION & CYCLE DETECTION
    @Test
    fun testWorkflowValidationAndCycleDetection() {
        // Valid DAG
        val validWorkflow = Workflow(
            id = "wf_valid",
            name = "Valid Sequential Workflow",
            description = "No cycles or invalid dependencies",
            steps = listOf(
                WorkflowStep("step1", "Read Memory", "Memory", "read state"),
                WorkflowStep("step2", "Format Notes", "FileGeneration", "save as txt called note.txt with content \${step1.output}", dependencies = listOf("step1"))
            )
        )
        val validRes = workflowEngine.validateWorkflow(validWorkflow)
        assertTrue(validRes.isValid)
        assertTrue(validRes.errors.isEmpty())

        // Cyclic DAG
        val cyclicWorkflow = Workflow(
            id = "wf_cyclic",
            name = "Cyclic Workflow",
            description = "Has circular dependencies",
            steps = listOf(
                WorkflowStep("step1", "Step 1", "Memory", "read", dependencies = listOf("step2")),
                WorkflowStep("step2", "Step 2", "Memory", "read", dependencies = listOf("step1"))
            )
        )
        val cyclicRes = workflowEngine.validateWorkflow(cyclicWorkflow)
        assertFalse(cyclicRes.isValid)
        assertTrue(cyclicRes.errors.any { it.contains("circular") })
    }

    // 2. SEQUENTIAL WORKFLOW EXECUTION & STEP OUTPUT INTERPOLATION
    @Test
    fun testSequentialWorkflowExecution() = runBlocking {
        val workflow = Workflow(
            id = "wf_seq",
            name = "Sequential Execution Test",
            description = "Tests interpolation of step output",
            steps = listOf(
                WorkflowStep("s1", "Check Battery", "Battery", "check battery level"),
                WorkflowStep("s2", "Log Telemetry", "FileGeneration", "save as txt called telemetry_run.txt with content \${s1.output}", dependencies = listOf("s1"))
            )
        )

        val executionContext = ExecutionContext(
            workflowId = workflow.id,
            deviceTelemetry = DeviceTelemetry(batteryPercent = 92, networkType = "WIFI"),
            repository = repository,
            brain = brain
        )

        val result = workflowEngine.executeWorkflow(workflow, executionContext)
        assertEquals(WorkflowStatus.COMPLETED, result.status)
        assertEquals(2, result.stepResults.size)
        assertEquals(StepStatus.SUCCESS, result.stepResults["s1"]?.status)
        assertEquals(StepStatus.SUCCESS, result.stepResults["s2"]?.status)
        assertTrue(result.stepResults["s2"]?.output?.contains("telemetry_run.txt") == true)
    }

    // 3. CONDITION EVALUATION
    @Test
    fun testWorkflowConditionEvaluation() = runBlocking {
        val conditionalWorkflow = Workflow(
            id = "wf_cond",
            name = "Conditional Execution Test",
            description = "Skips step if condition fails",
            steps = listOf(
                WorkflowStep(
                    stepId = "s1_battery_check",
                    name = "Battery Alert Step",
                    toolName = "Notifications",
                    inputTemplate = "send alert Low Battery",
                    condition = WorkflowCondition.BatteryLevelCondition(minPercent = 0, maxPercent = 20)
                )
            )
        )

        // Telemetry with 85% battery -> condition should evaluate to false and skip step
        val contextWith85Percent = ExecutionContext(
            workflowId = conditionalWorkflow.id,
            deviceTelemetry = DeviceTelemetry(batteryPercent = 85, networkType = "WIFI"),
            repository = repository,
            brain = brain
        )

        val result = workflowEngine.executeWorkflow(conditionalWorkflow, contextWith85Percent)
        assertEquals(WorkflowStatus.COMPLETED, result.status)
        assertEquals(StepStatus.SKIPPED, result.stepResults["s1_battery_check"]?.status)
        assertTrue(result.stepResults["s1_battery_check"]?.output?.contains("evaluated to false") == true)
    }

    // 4. AUTOMATION ORCHESTRATOR INTEGRATION
    @Test
    fun testOrchestratorExecution() = runBlocking {
        val workflows = orchestrator.getAllWorkflows()
        assertTrue(workflows.isNotEmpty())

        val healthWf = workflows.first { it.id == "wf_system_health" }
        val result = orchestrator.runWorkflow(healthWf.id)

        assertEquals(WorkflowStatus.COMPLETED, result.status)
        val updatedWf = orchestrator.getWorkflow(healthWf.id)
        assertNotNull(updatedWf)
        assertEquals(1, updatedWf!!.executionHistoryCount)
    }

    // 5. FAILURE HANDLING & ROLLBACK
    @Test
    fun testFailureHandlingStopAndRollback() = runBlocking {
        val failingWorkflow = Workflow(
            id = "wf_fail",
            name = "Failing Workflow",
            description = "Tests failure action STOP",
            steps = listOf(
                WorkflowStep("s1", "Invalid Tool Step", "NonExistentTool", "do something", onFailure = FailureAction.STOP),
                WorkflowStep("s2", "Dependent Step", "Memory", "read", dependencies = listOf("s1"))
            )
        )

        val contextEnv = ExecutionContext(
            workflowId = failingWorkflow.id,
            repository = repository,
            brain = brain
        )

        val result = workflowEngine.executeWorkflow(failingWorkflow, contextEnv)
        assertEquals(WorkflowStatus.FAILED, result.status)
        assertEquals(StepStatus.FAILURE, result.stepResults["s1"]?.status)
        assertEquals(StepStatus.SKIPPED, result.stepResults["s2"]?.status)
    }
}
