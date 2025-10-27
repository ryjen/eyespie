package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CachedEvent @OptIn(ExperimentalTime::class) constructor(
    val event: TelemetryEvent,
    val cachedAt: Instant
)
