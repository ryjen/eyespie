package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Health status
data class HealthStatus @OptIn(ExperimentalTime::class) constructor(
    val isHealthy: Boolean,
    val status: Status,
    val lastSuccessfulSend: Instant?,
    val lastError: String?,
    val consecutiveFailures: Int,
    val details: Map<String, Any> = emptyMap()
) {
    enum class Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }
}
