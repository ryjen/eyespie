package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant


data class DestinationStatus @OptIn(ExperimentalTime::class) constructor(
    val isEnabled: Boolean,
    val isHealthy: Boolean,
    val lastSync: Instant?,
    val pendingEvents: Int
)
