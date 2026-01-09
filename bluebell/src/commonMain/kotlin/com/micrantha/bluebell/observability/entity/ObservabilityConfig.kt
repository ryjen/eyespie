package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ObservabilityConfig(
    val maxEventSize: Long = 100_000L,
    val maxRetryAttempts: Int = 3,
    val retryProcessInterval: Duration = 30.seconds,
    val enablePersistentQueue: Boolean = true,
    val maxQueueSize: Int = 10000
) {
    companion object {
        fun default() = ObservabilityConfig()
    }
}
