package com.example.jarvis.recovery

import com.example.jarvis.security.SensitiveDataFilter
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Intelligent error classifier and sanitizer.
 * Converts raw runtime exceptions into safe, structured JarvisError instances
 * and ensures sensitive credentials, API keys, or tokens are never leaked.
 */
object ErrorClassifier {

    fun classify(
        throwable: Throwable,
        source: String = "AI_PROVIDER",
        currentRetryCount: Int = 0
    ): JarvisError {
        val rawMessage = throwable.message ?: throwable.javaClass.simpleName
        val sanitizedTechMessage = SensitiveDataFilter.sanitizeForDisplay(rawMessage)
        val lower = rawMessage.lowercase()

        val (category, isRetryable, isRecoverable, userSafeMsg) = when {
            // Cancellation / Interruption
            throwable is kotlinx.coroutines.CancellationException -> {
                Quad(
                    ErrorCategory.INTERNAL,
                    false,
                    false,
                    "Operation was cancelled."
                )
            }

            // Timeouts
            throwable is SocketTimeoutException || throwable is TimeoutException ||
            lower.contains("timeout") || lower.contains("timed out") -> {
                Quad(
                    ErrorCategory.TIMEOUT,
                    true,
                    true,
                    "The operation timed out. Verifying connectivity and retrying..."
                )
            }

            // Rate Limits / Quotas (e.g. HTTP 429, RESOURCE_EXHAUSTED)
            lower.contains("429") || lower.contains("quota") ||
            lower.contains("resource_exhausted") || lower.contains("rate limit") -> {
                // Rate limits should NOT hammer the provider with aggressive retries
                Quad(
                    ErrorCategory.RATE_LIMIT,
                    false,
                    true,
                    "The cloud AI service is experiencing high traffic. Operating using onboard intelligence."
                )
            }

            // Authentication & API Key issues
            lower.contains("401") || lower.contains("403") || lower.contains("api_key") ||
            lower.contains("unauthorized") || lower.contains("forbidden") || lower.contains("invalid key") -> {
                Quad(
                    ErrorCategory.AUTH,
                    false,
                    false,
                    "Authentication with the AI provider failed. Please verify API credentials in Settings."
                )
            }

            // Permissions
            throwable is SecurityException || lower.contains("permission denied") || lower.contains("missing permission") -> {
                Quad(
                    ErrorCategory.PERMISSION,
                    false,
                    false,
                    "Required system permissions are not granted for this action."
                )
            }

            // Network / Connection drops
            throwable is UnknownHostException || throwable is ConnectException || throwable is IOException ||
            lower.contains("unable to resolve host") || lower.contains("network unreachable") || lower.contains("connection refused") -> {
                Quad(
                    ErrorCategory.NETWORK,
                    true,
                    true,
                    "Network connection is temporarily unavailable. Retrying..."
                )
            }

            // Parsing & Malformed JSON
            throwable is org.json.JSONException || lower.contains("json") || lower.contains("parse") || lower.contains("malformed") -> {
                Quad(
                    ErrorCategory.PARSING,
                    true,
                    true,
                    "Received an unreadable response format from the AI provider."
                )
            }

            // Validation
            throwable is IllegalArgumentException || throwable is IllegalStateException -> {
                Quad(
                    ErrorCategory.VALIDATION,
                    false,
                    false,
                    "Invalid parameters provided for this operation."
                )
            }

            // Server-side provider errors (HTTP 500, 502, 503, 504)
            lower.contains("500") || lower.contains("502") || lower.contains("503") || lower.contains("504") ||
            lower.contains("server error") || lower.contains("overloaded") -> {
                Quad(
                    ErrorCategory.AI_PROVIDER,
                    true,
                    true,
                    "The cloud AI service is temporarily overloaded. Please try again in a moment."
                )
            }

            else -> {
                Quad(
                    ErrorCategory.UNKNOWN,
                    false,
                    false,
                    "An unexpected error occurred. Local recovery protocols engaged."
                )
            }
        }

        return JarvisError(
            category = category,
            userSafeMessage = userSafeMsg,
            technicalMessage = sanitizedTechMessage,
            isRecoverable = isRecoverable,
            isRetryable = isRetryable,
            retryCount = currentRetryCount,
            source = source,
            cause = throwable
        )
    }

    private data class Quad(
        val category: ErrorCategory,
        val isRetryable: Boolean,
        val isRecoverable: Boolean,
        val userSafeMessage: String
    )
}
