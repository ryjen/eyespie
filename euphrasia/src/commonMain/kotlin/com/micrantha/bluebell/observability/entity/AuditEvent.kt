package com.micrantha.bluebell.observability.entity

import com.micrantha.eyespie.core.data.account.model.CurrentSession
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
sealed class AuditEvent(
    override val properties: Map<String, Any> = emptyMap(),
    override val timestamp: Instant = Clock.System.now(),
    override val userId: String? = CurrentSession.userId,
    override val sessionId: String? = CurrentSession.sessionId,
    override val schema: SchemaVersion = SchemaVersion("eyespie.audit", 1)
) : TelemetryEvent {
    data class UserAction(override val eventId: String) : AuditEvent()
    data class DataAccess(override val eventId: String) : AuditEvent()
    data class StateChange(override val eventId: String) : AuditEvent()
}
