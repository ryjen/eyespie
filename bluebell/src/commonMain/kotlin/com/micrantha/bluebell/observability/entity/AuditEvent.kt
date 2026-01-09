package com.micrantha.bluebell.observability.entity

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed class AuditEvent(
    override val properties: Map<String, Any> = emptyMap(),
    override val timestamp: Instant = Clock.System.now(),
    override val userId: String? = null,
    override val sessionId: String? = null,
    override val schema: SchemaVersion = SchemaVersion("eyespie.audit", 1)
) : TelemetryEvent {
    data class UserAction(override val eventId: String) : AuditEvent()
    data class DataAccess(override val eventId: String) : AuditEvent()
    data class StateChange(override val eventId: String) : AuditEvent()
}
