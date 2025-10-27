package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Base event model
sealed interface TelemetryEvent {
    val eventId: String

    @OptIn(ExperimentalTime::class)
    val timestamp: Instant
    val userId: String?
    val sessionId: String?
    val properties: Map<String, Any>
    val schema: SchemaVersion
}
