package com.example.jarvis.safety

import com.example.jarvis.model.RiskLevel
import com.example.jarvis.model.SafetyRequest

data class RiskAssessment(
    val level: RiskLevel,
    val isPermitted: Boolean,
    val reason: String,
    val actionSummary: String
)

object RiskEngine {

    private val RESTRICTED_PATTERNS = listOf(
        Regex("""(?i)\b(sh|bash|exec|system|eval|chmod|chown|su|sudo|rm\s+-rf|dd\s+if=|setenforce|pm\s+uninstall|root)\b"""),
        Regex("""(?i)\b(shell|shellexec|terminal|cmd|powershell)\b"""),
        Regex("""(?i)\b(bypass|disable)\s+(lock\s*screen|pin|pattern|password|biometrics|knox|frp)\b"""),
        Regex("""(?i)\b(extract|steal|dump|sniff)\s+(keystore|tokens|credentials|passwords|auth|banking|cookies)\b"""),
        Regex("""(?i)\b(bypass|solve)\s+(captcha|recaptcha|bot\s*detection)\b"""),
        Regex("""(?i)\b(hidden|stealth|covert|secret)\s+(recording|camera|surveillance|keylogger|spy)\b""")
    )

    private val CONFIRMATION_PATTERNS = listOf(
        Regex("""(?i)\b(call|dial|phone|sms|send\s+text|message|email)\b"""),
        Regex("""(?i)\b(delete|remove|erase|wipe|clear|purge)\s+(all|memory|memories|tasks|contacts|data|files)\b"""),
        Regex("""(?i)\b(open\s+url|launch\s+browser|browse\s+to|open\s+link)\b"""),
        Regex("""(?i)\b(modify|change|toggle)\s+(system\s+settings|bluetooth|wifi|security|lock)\b"""),
        Regex("""(?i)\b(overwrite|replace|force\s+write)\s+(?:existing\s+)?(?:file|report|document)\b""")
    )

    fun assessAction(actionName: String, actionPayload: String, baseRisk: RiskLevel = RiskLevel.SAFE): RiskAssessment {
        val combined = "$actionName $actionPayload".trim()

        // 1. Strict RESTRICTED check
        for (pattern in RESTRICTED_PATTERNS) {
            if (pattern.containsMatchIn(combined)) {
                return RiskAssessment(
                    level = RiskLevel.RESTRICTED,
                    isPermitted = false,
                    reason = "Operation violates Android security baseline (security/lockscreen bypass, credential harvesting, or arbitrary shell execution strictly prohibited).",
                    actionSummary = "Restricted Command: $actionName"
                )
            }
        }

        // 2. CONFIRMATION check
        if (baseRisk == RiskLevel.CONFIRMATION) {
            return RiskAssessment(
                level = RiskLevel.CONFIRMATION,
                isPermitted = true,
                reason = "Action triggers external intent, communications, or persistent state alteration.",
                actionSummary = "$actionName ($actionPayload)"
            )
        }

        for (pattern in CONFIRMATION_PATTERNS) {
            if (pattern.containsMatchIn(combined)) {
                return RiskAssessment(
                    level = RiskLevel.CONFIRMATION,
                    isPermitted = true,
                    reason = "Action involves external communication, external browser intent, or data removal.",
                    actionSummary = "$actionName ($actionPayload)"
                )
            }
        }

        // 3. SAFE check
        return RiskAssessment(
            level = RiskLevel.SAFE,
            isPermitted = true,
            reason = "Read-only or local harmless operation.",
            actionSummary = "$actionName ($actionPayload)"
        )
    }

    fun buildSafetyRequest(
        toolName: String,
        actionPayload: String,
        reason: String,
        riskLevel: RiskLevel,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ): SafetyRequest {
        return SafetyRequest(
            toolName = toolName,
            actionDescription = actionPayload,
            reason = reason,
            riskLevel = riskLevel,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}
