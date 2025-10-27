package com.micrantha.bluebell.observability.entity

import com.micrantha.eyespie.core.data.account.model.CurrentSession
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Event categories
@OptIn(ExperimentalTime::class)
sealed class SystemEvent(
    override val properties: Map<String, Any> = emptyMap(),
    override val timestamp: Instant = Clock.System.now(),
    override val userId: String? = CurrentSession.userId,
    override val sessionId: String? = CurrentSession.sessionId,
    override val schema: SchemaVersion = SchemaVersion("eyespie.system", 1)
) : TelemetryEvent {

    data class Crash(override val eventId: String) : SystemEvent()
    data class Error(override val eventId: String) : SystemEvent()
    data class Performance(
        override val eventId: String,
        override val timestamp: Instant = Clock.System.now(),
        override val properties: Map<String, Any> = emptyMap(),
        override val schema: SchemaVersion = SchemaVersion("system.performance", 1)
    ) : SystemEvent()
}
