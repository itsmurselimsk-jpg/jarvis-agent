package com.example.jarvis.plugin

import com.example.jarvis.security.SensitiveDataFilter

/**
 * Result of manifest or tool validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Security guardian for the Plugin and Connected Services subsystem.
 * Defends against prompt injection, privilege escalation, credential leakage,
 * recursive loops, oversized payloads, and malformed schemas.
 */
object PluginSafetyEngine {

    private const val MAX_RESPONSE_PAYLOAD_BYTES = 32 * 1024 // 32 KB limit per plugin output
    private const val MAX_CALL_DEPTH = 2

    // Patterns indicating prompt injection attacks embedded inside untrusted plugin data
    private val PROMPT_INJECTION_PATTERNS = listOf(
        Regex("""(?i)\b(ignore\s+(?:all\s+)?previous\s+instructions)\b"""),
        Regex("""(?i)\b(disregard\s+(?:all\s+)?prior\s+guidelines)\b"""),
        Regex("""(?i)\b(you\s+are\s+now\s+in\s+developer\s+mode)\b"""),
        Regex("""(?i)\b(system\s*prompt\s*override)\b"""),
        Regex("""(?i)\b(export|dump|leak|send)\s+(?:all\s+)?(?:credentials|tokens|keys|passwords|database)\b"""),
        Regex("""(?i)\b(elevate\s+privileges|bypass\s+safety|disable\s+guardrails)\b"""),
        Regex("""(?i)<\s*(?:script|iframe|object|embed)[^>]*>"""),
        Regex("""(?i)\bjavascript\s*:""")
    )

    private val SUSPICIOUS_ID_REGEX = Regex("""^[a-zA-Z0-9_\-\.]{3,64}$""")
    private val VERSION_REGEX = Regex("""^\d+(\.\d+){1,3}(-[a-zA-Z0-9\.\-_]+)?$""")

    /**
     * Validates a plugin manifest for structural integrity and safety standards.
     */
    fun validateManifest(manifest: PluginManifest): ValidationResult {
        val errors = mutableListOf<String>()

        if (manifest.id.isBlank() || !SUSPICIOUS_ID_REGEX.matches(manifest.id)) {
            errors.add("Invalid plugin ID: must be 3-64 alphanumeric characters, dashes, dots, or underscores")
        }

        if (manifest.displayName.isBlank() || manifest.displayName.length > 80) {
            errors.add("Display name must be non-blank and at most 80 characters")
        }

        if (!VERSION_REGEX.matches(manifest.version)) {
            errors.add("Invalid semantic version format: '${manifest.version}'")
        }

        if (manifest.description.isBlank() || manifest.description.length > 500) {
            errors.add("Description must be non-blank and at most 500 characters")
        }

        if (manifest.capabilities.isEmpty()) {
            errors.add("Plugin must declare at least one capability")
        }

        // Validate permissions match declared capabilities
        for (perm in manifest.permissions) {
            val valid = when (perm) {
                PluginPermission.READ_RECORDS -> manifest.capabilities.contains(PluginCapability.READ)
                PluginPermission.WRITE_RECORDS -> manifest.capabilities.contains(PluginCapability.WRITE)
                PluginPermission.SEARCH_RECORDS -> manifest.capabilities.contains(PluginCapability.SEARCH)
                PluginPermission.CREATE_RECORDS -> manifest.capabilities.contains(PluginCapability.CREATE)
                PluginPermission.UPDATE_RECORDS -> manifest.capabilities.contains(PluginCapability.UPDATE)
                PluginPermission.DELETE_RECORDS -> manifest.capabilities.contains(PluginCapability.DELETE)
                PluginPermission.ACCESS_NETWORK -> manifest.capabilities.contains(PluginCapability.NETWORK)
                PluginPermission.ACCESS_ACCOUNT -> manifest.capabilities.contains(PluginCapability.ACCOUNT_DATA)
                PluginPermission.ACCESS_SENSITIVE -> manifest.capabilities.contains(PluginCapability.SENSITIVE_DATA)
            }
            if (!valid) {
                errors.add("Permission '$perm' is declared without matching capability in manifest")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validates a plugin tool definition against its parent manifest and schema rules.
     */
    fun validateToolDefinition(
        tool: PluginToolDefinition,
        manifest: PluginManifest
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (tool.name.isBlank() || !SUSPICIOUS_ID_REGEX.matches(tool.name)) {
            errors.add("Tool name '${tool.name}' is invalid")
        }

        if (tool.description.isBlank() || tool.description.length > 300) {
            errors.add("Tool description must be between 1 and 300 characters")
        }

        // Tool description must not contain injection commands
        for (pattern in PROMPT_INJECTION_PATTERNS) {
            if (pattern.containsMatchIn(tool.description)) {
                errors.add("Tool description contains forbidden injection payload pattern")
                break
            }
        }

        // Tool capabilities must be a subset of manifest capabilities
        for (cap in tool.requiredCapabilities) {
            if (!manifest.capabilities.contains(cap)) {
                errors.add("Tool '${tool.name}' requires capability '$cap' which is not declared in plugin manifest '${manifest.id}'")
            }
        }

        // Input schema verification
        for ((key, typeDesc) in tool.inputSchema) {
            if (key.isBlank() || key.length > 40) {
                errors.add("Input parameter key '$key' is invalid")
            }
            if (typeDesc.length > 100) {
                errors.add("Type description for parameter '$key' is too long")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Validates payload size to protect against memory exhaustion or denial-of-service.
     */
    fun validatePayloadSize(payload: String, maxBytes: Int = MAX_RESPONSE_PAYLOAD_BYTES): Boolean {
        return payload.toByteArray(Charsets.UTF_8).size <= maxBytes
    }

    /**
     * Sanitizes plugin responses by defusing prompt injection and isolating external data.
     */
    fun sanitizePluginOutput(rawOutput: String): String {
        if (rawOutput.isBlank()) return rawOutput

        var sanitized = rawOutput

        // Truncate if exceeds max payload
        if (sanitized.length > MAX_RESPONSE_PAYLOAD_BYTES) {
            sanitized = sanitized.take(MAX_RESPONSE_PAYLOAD_BYTES) + "\n[DATA TRUNCATED: Payload exceeded safe buffer limit]"
        }

        // Defuse prompt injection attempts
        for (pattern in PROMPT_INJECTION_PATTERNS) {
            sanitized = pattern.replace(sanitized) { mr ->
                "[INERT_PLUGIN_DATA: neutralized control token '${mr.value.take(25)}...']"
            }
        }

        // General credential redaction
        sanitized = SensitiveDataFilter.redactSensitiveData(sanitized)

        return sanitized
    }

    /**
     * Redacts known active secrets and credentials before display or logging.
     */
    fun redactSecrets(text: String, activeSecrets: List<String> = emptyList()): String {
        if (text.isBlank()) return text
        var result = SensitiveDataFilter.redactSensitiveData(text)
        for (secret in activeSecrets) {
            if (secret.isNotBlank() && secret.length >= 4) {
                result = result.replace(secret, "[PLUGIN_SECRET_REDACTED]")
            }
        }
        return result
    }

    /**
     * Checks current plugin invocation depth to prevent infinite recursion.
     */
    fun checkCallDepth(currentDepth: Int): Boolean {
        return currentDepth <= MAX_CALL_DEPTH
    }
}
