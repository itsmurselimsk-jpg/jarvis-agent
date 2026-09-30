package com.example.jarvis.recovery

/**
 * Structured category classifications for all runtime and operational errors.
 */
enum class ErrorCategory {
    NETWORK,
    TIMEOUT,
    AI_PROVIDER,
    RATE_LIMIT,
    AUTH,
    TOOL,
    PERMISSION,
    VALIDATION,
    PARSING,
    INTERNAL,
    UNKNOWN
}

/**
 * Lifecycle states of error recovery for UI and diagnostics observation.
 */
enum class RecoveryStatus {
    IDLE,
    RETRYING,
    TEMPORARILY_UNAVAILABLE,
    FAILED,
    RECOVERED
}

/**
 * Production-grade structured error model encapsulating user-safe and technical error details.
 */
data class JarvisError(
    val category: ErrorCategory,
    val userSafeMessage: String,
    val technicalMessage: String,
    val isRecoverable: Boolean,
    val isRetryable: Boolean,
    val retryCount: Int = 0,
    val source: String = "JARVIS",
    val timestamp: Long = System.currentTimeMillis(),
    val cause: Throwable? = null
)

/**
 * Observable recovery state exposed to ViewModel, UI, and diagnostics.
 */
data class RecoveryState(
    val status: RecoveryStatus = RecoveryStatus.IDLE,
    val lastError: JarvisError? = null,
    val attempt: Int = 0,
    val maxAttempts: Int = 0,
    val message: String? = null
)
