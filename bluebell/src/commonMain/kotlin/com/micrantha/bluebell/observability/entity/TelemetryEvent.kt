package com.micrantha.bluebell.observability.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed interface TelemetryEvent {
    val eventId: String
    val timestamp: Instant
    val userId: String?
    val sessionId: String?
    val properties: Map<String, Any>
    val schema: SchemaVersion
}
