package com.example.jarvis.recovery

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Production-grade configurable retry policy with exponential backoff and randomized jitter.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialBackoffMs: Long = 250L,
    val maxBackoffMs: Long = 2000L,
    val backoffMultiplier: Double = 2.0,
    val jitterFactor: Double = 0.2
) {
    companion object {
        val DEFAULT = RetryPolicy()
        val NONE = RetryPolicy(maxAttempts = 1)
        val AGGRESSIVE_FALLBACK = RetryPolicy(maxAttempts = 2, initialBackoffMs = 150L, maxBackoffMs = 1000L)
        val READ_ONLY_TOOL = RetryPolicy(maxAttempts = 2, initialBackoffMs = 200L, maxBackoffMs = 1000L)
    }

    /**
     * Calculates exponential backoff with jitter for a specific attempt count.
     */
    fun calculateDelay(attempt: Int): Long {
        if (attempt <= 1) return initialBackoffMs
        val rawBackoff = (initialBackoffMs * backoffMultiplier.pow((attempt - 1).toDouble())).toLong()
        val bounded = min(rawBackoff, maxBackoffMs)
        val jitter = (bounded * jitterFactor * Random.nextDouble(-1.0, 1.0)).toLong()
        return (bounded + jitter).coerceAtLeast(50L)
    }
}

/**
 * Executes a suspending block with bounded retry semantics.
 * - Respects coroutine cancellation immediately.
 * - Automatically classifies errors.
 * - Stops immediately for permanent non-retryable errors or rate limits.
 */
suspend fun <T> executeWithRetry(
    policy: RetryPolicy = RetryPolicy.DEFAULT,
    operationName: String = "Operation",
    source: String = "JARVIS",
    onRetry: suspend (attempt: Int, error: JarvisError, delayMs: Long) -> Unit = { _, _, _ -> },
    block: suspend (attempt: Int) -> T
): Result<T> {
    var lastError: JarvisError? = null

    for (attempt in 1..policy.maxAttempts) {
        currentCoroutineContext().ensureActive()
        try {
            val result = block(attempt)
            return Result.success(result)
        } catch (t: Throwable) {
            currentCoroutineContext().ensureActive()

            val classified = ErrorClassifier.classify(
                throwable = t,
                source = source,
                currentRetryCount = attempt
            )
            lastError = classified

            // Non-retryable error, cancellation, or rate-limit -> abort immediately
            if (!classified.isRetryable || attempt >= policy.maxAttempts) {
                return Result.failure(classified.cause ?: t)
            }

            val delayMs = policy.calculateDelay(attempt)
            onRetry(attempt, classified, delayMs)
            delay(delayMs)
        }
    }

    val fallbackThrowable = lastError?.cause ?: RuntimeException("$operationName failed after ${policy.maxAttempts} attempts")
    return Result.failure(fallbackThrowable)
}
