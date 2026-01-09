package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration

// Configuration
sealed interface DestinationConfig {
    val enabled: Boolean
    val batchSize: Int
    val flushInterval: Duration
    val retryPolicy: RetryPolicy
    val timeout: Duration
}
