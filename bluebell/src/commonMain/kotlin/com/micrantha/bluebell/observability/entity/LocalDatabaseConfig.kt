package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class LocalDatabaseConfig(
    override val enabled: Boolean = true,
    override val batchSize: Int = 500,
    override val flushInterval: Duration = 5.seconds,
    override val retryPolicy: RetryPolicy = RetryPolicy.none(),
    override val timeout: Duration = 10.seconds,
    val maxDatabaseSize: Long = 100_000_000L, // 100MB
    val retentionDays: Int = 30,
    val vacuumInterval: Duration = 24.hours
) : DestinationConfig
