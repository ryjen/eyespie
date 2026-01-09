package com.micrantha.bluebell.observability.entity

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Context passed to destinations
data class DestinationContext @OptIn(ExperimentalTime::class) constructor(
    val campaigns: List<Campaign> = emptyList(),
    val spanId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val globalProperties: Map<String, Any> = emptyMap(),
    val deviceInfo: DeviceInfo? = null,
    val networkInfo: NetworkInfo? = null,
    val timestamp: Instant = Clock.System.now(),
    val retryAttempt: Int = 0,
    val traceContext: TraceContext? = null
)
