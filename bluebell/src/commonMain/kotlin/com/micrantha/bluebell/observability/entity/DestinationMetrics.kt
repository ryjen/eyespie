package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Metrics
data class DestinationMetrics @OptIn(ExperimentalTime::class) constructor(
    val totalEventsSent: Long,
    val totalEventsAccepted: Long,
    val totalEventsRejected: Long,
    val totalBatchesSent: Long,
    val averageLatencyMs: Double,
    val errorRate: Double,
    val lastSendTime: Instant?,
    val queueSize: Int,
    val bytesTransferred: Long
)
