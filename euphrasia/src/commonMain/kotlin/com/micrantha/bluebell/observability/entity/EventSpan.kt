package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Event grouping
data class EventSpan @OptIn(ExperimentalTime::class) constructor(
    val spanId: String,
    val name: String,
    val startTime: Instant,
    val endTime: Instant?,
    val attributes: Map<String, Any>,
    val events: List<TelemetryEvent>
)
