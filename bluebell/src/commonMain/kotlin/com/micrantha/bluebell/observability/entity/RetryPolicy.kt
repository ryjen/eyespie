package com.micrantha.bluebell.observability.entity

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Retry policy
data class RetryPolicy(
    val maxAttempts: Int,
    val initialDelay: Duration,
    val maxDelay: Duration,
    val backoffMultiplier: Double,
    val retryableErrors: Set<String> = emptySet()
) {
    companion object {
        fun default() = RetryPolicy(
            maxAttempts = 3,
            initialDelay = 1.seconds,
            maxDelay = 60.seconds,
            backoffMultiplier = 2.0
        )

        fun none() = RetryPolicy(
            maxAttempts = 0,
            initialDelay = Duration.ZERO,
            maxDelay = Duration.ZERO,
            backoffMultiplier = 1.0
        )

        fun aggressive() = RetryPolicy(
            maxAttempts = 5,
            initialDelay = 500.milliseconds,
            maxDelay = 30.seconds,
            backoffMultiplier = 1.5
        )
    }

    fun getDelay(attempt: Int): Duration {
        val delay = initialDelay.inWholeMilliseconds *
                backoffMultiplier.pow(attempt.toDouble())
        return minOf(delay.toLong(), maxDelay.inWholeMilliseconds).milliseconds
    }
}
