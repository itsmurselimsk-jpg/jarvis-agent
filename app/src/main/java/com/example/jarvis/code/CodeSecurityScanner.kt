package com.example.jarvis.code

import java.util.regex.Pattern

/**
 * High-precision secret detection and sanitization.
 * Enforces mandatory privacy rules:
 * - Detected secrets are sanitized before any external transmission.
 * - Raw secrets are NEVER sent to AIProvider.
 * - Raw secrets are NEVER printed in plain text in logs.
 */
object CodeSecurityScanner {

    private val GOOGLE_API_KEY_REGEX = Regex("""AIza[A-Za-z0-9_-]{35}""")
    private val OPENAI_API_KEY_REGEX = Regex("""sk-[A-Za-z0-9_-]{20,}""")
    private val AWS_ACCESS_KEY_REGEX = Regex("""AKIA[0-9A-Z]{16}""")
    private val GITHUB_TOKEN_REGEX = Regex("""(?:ghp_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{22,})""")
    private val PRIVATE_KEY_REGEX = Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----""")
    private val JWT_BEARER_REGEX = Regex("""Bearer\s+[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""", RegexOption.IGNORE_CASE)
    private val GENERIC_SECRET_REGEX = Regex("""(?i)(?:api_?key|secret|password|access_?token|client_?secret|auth_?token)\s*[:=]\s*["']([^"'\s]{8,})["']""")

    /**
     * Scans raw source code for hardcoded secrets, returning findings.
     */
    fun scanForSecrets(content: String, fileName: String? = null): List<CodeFinding> {
        val findings = mutableListOf<CodeFinding>()
        val lines = content.lines()

        lines.forEachIndexed { index, line ->
            val lineNumber = index + 1

            // 1. Google / Firebase API Keys
            GOOGLE_API_KEY_REGEX.findAll(line).forEach {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.CRITICAL,
                        category = FindingCategory.SECURITY,
                        file = fileName,
                        line = lineNumber,
                        description = "Hardcoded Google/Firebase API Key detected",
                        explanation = "Embedding plain text API keys in source code risks credential theft and unauthorized quota consumption.",
                        suggestedFix = "Store the key in AI Studio Secrets panel and access via BuildConfig or BuildConfig.API_KEY."
                    )
                )
            }

            // 2. OpenAI API Keys
            OPENAI_API_KEY_REGEX.findAll(line).forEach {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.CRITICAL,
                        category = FindingCategory.SECURITY,
                        file = fileName,
                        line = lineNumber,
                        description = "Hardcoded OpenAI API Key detected",
                        explanation = "Exposing OpenAI secret tokens enables unauthorized model invocations and billing charges.",
                        suggestedFix = "Inject the key securely through environment configuration or AI Studio Secrets."
                    )
                )
            }

            // 3. AWS Credentials
            AWS_ACCESS_KEY_REGEX.findAll(line).forEach {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.CRITICAL,
                        category = FindingCategory.SECURITY,
                        file = fileName,
                        line = lineNumber,
                        description = "Hardcoded AWS Access Key ID detected",
                        explanation = "AWS credentials in source code can lead to compromised cloud infrastructure.",
                        suggestedFix = "Use IAM roles or inject via secure environment variables."
                    )
                )
            }

            // 4. GitHub Tokens
            GITHUB_TOKEN_REGEX.findAll(line).forEach {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.CRITICAL,
                        category = FindingCategory.SECURITY,
                        file = fileName,
                        line = lineNumber,
                        description = "Hardcoded GitHub Personal Access Token detected",
                        explanation = "GitHub tokens permit unauthorized repository access and modifications.",
                        suggestedFix = "Revoke this token immediately and use secret managers."
                    )
                )
            }

            // 5. JWT Bearer Tokens
            JWT_BEARER_REGEX.findAll(line).forEach {
                findings.add(
                    CodeFinding(
                        severity = FindingSeverity.HIGH,
                        category = FindingCategory.SECURITY,
                        file = fileName,
                        line = lineNumber,
                        description = "Hardcoded JWT / Bearer authentication token detected",
                        explanation = "Hardcoded session tokens permit session hijacking and unauthorized API access.",
                        suggestedFix = "Acquire tokens dynamically via an authentication handshake."
                    )
                )
            }

            // 6. Generic hardcoded secret / password assignments
            GENERIC_SECRET_REGEX.findAll(line).forEach { match ->
                val secretVal = match.groupValues[1]
                // Filter out non-secret placeholder tokens
                if (!isPlaceholder(secretVal)) {
                    findings.add(
                        CodeFinding(
                            severity = FindingSeverity.HIGH,
                            category = FindingCategory.SECURITY,
                            file = fileName,
                            line = lineNumber,
                            description = "Hardcoded credential/password assignment detected",
                            explanation = "Hardcoded credentials in source control present severe security risks.",
                            suggestedFix = "Move sensitive credentials out of source code into secure storage or build configs."
                        )
                    )
                }
            }
        }

        // 7. Multi-line Private Keys
        PRIVATE_KEY_REGEX.findAll(content).forEach {
            findings.add(
                CodeFinding(
                    severity = FindingSeverity.CRITICAL,
                    category = FindingCategory.SECURITY,
                    file = fileName,
                    line = null,
                    description = "Embedded Private Key (RSA / EC / PKCS) detected",
                    explanation = "Private encryption keys must never reside in client-side source code.",
                    suggestedFix = "Use Android Keystore or server-side key management."
                )
            )
        }

        return findings
    }

    /**
     * Sanitizes source code by masking all detected secrets with redaction placeholders
     * so that the raw secret text is NEVER transmitted to external AI providers.
     */
    fun sanitizeForAiTransmission(rawCode: String): Pair<String, List<CodeFinding>> {
        val findings = scanForSecrets(rawCode)
        var sanitized = rawCode

        sanitized = GOOGLE_API_KEY_REGEX.replace(sanitized, "[REDACTED_GOOGLE_API_KEY]")
        sanitized = OPENAI_API_KEY_REGEX.replace(sanitized, "[REDACTED_OPENAI_KEY]")
        sanitized = AWS_ACCESS_KEY_REGEX.replace(sanitized, "[REDACTED_AWS_KEY]")
        sanitized = GITHUB_TOKEN_REGEX.replace(sanitized, "[REDACTED_GITHUB_TOKEN]")
        sanitized = PRIVATE_KEY_REGEX.replace(sanitized, "-----BEGIN PRIVATE KEY-----\n[REDACTED_PRIVATE_KEY]\n-----END PRIVATE KEY-----")
        sanitized = JWT_BEARER_REGEX.replace(sanitized, "Bearer [REDACTED_JWT_TOKEN]")
        sanitized = GENERIC_SECRET_REGEX.replace(sanitized) { match ->
            if (match.value.contains("[REDACTED_")) {
                match.value
            } else {
                val isColon = match.value.contains(":")
                val sep = if (isColon) ":" else "="
                val prefix = match.value.substringBefore(sep)
                "$prefix$sep \"[REDACTED_CREDENTIAL]\""
            }
        }

        return Pair(sanitized, findings)
    }

    private fun isPlaceholder(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("your_") || lower.contains("placeholder") || lower.contains("my_gemini_api_key") ||
                lower.contains("xxx") || lower.contains("test_") || lower.contains("sample") || lower.contains("dummy")
    }
}
