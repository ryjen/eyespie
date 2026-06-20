package com.micrantha.bluebell.observability.entity

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
sealed class AuditEvent(
    override val eventId: String = Uuid.random().toString(),
    override val properties: Map<String, Any> = emptyMap(),
    override val timestamp: Instant = Clock.System.now(),
    override val userId: String? = null,
    override val sessionId: String? = null,
    override val schema: SchemaVersion = SchemaVersion("eyespie.audit", 1)
) : TelemetryEvent {
    data class UserAction(override val eventId: String = Uuid.random().toString()) : AuditEvent(eventId = eventId)
    data class DataAccess(override val eventId: String = Uuid.random().toString()) : AuditEvent(eventId = eventId)
    data class StateChange(override val eventId: String = Uuid.random().toString()) : AuditEvent(eventId = eventId)
}
