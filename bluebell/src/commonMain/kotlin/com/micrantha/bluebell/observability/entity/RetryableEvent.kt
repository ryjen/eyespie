package com.micrantha.bluebell.observability.entity

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class RetryableEvent @OptIn(ExperimentalTime::class) constructor(
    val event: TelemetryEvent,
    val destination: Destination,
    val retryAfter: Instant,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val firstAttempt: Instant = Clock.System.now(),
    val lastError: String? = null
) {
    fun canRetry(): Boolean = attempts < maxAttempts

    @OptIn(ExperimentalTime::class)
    fun isReady(): Boolean = Clock.System.now() >= retryAfter

    @OptIn(ExperimentalTime::class)
    fun withRetry(error: Throwable, nextRetryDelay: Duration): RetryableEvent {
        return copy(
            attempts = attempts + 1,
            retryAfter = Clock.System.now().plus(nextRetryDelay),
            lastError = error.message
        )
    }
}
