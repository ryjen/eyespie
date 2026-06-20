package com.micrantha.bluebell.observability.entity

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
sealed class SystemEvent(
    override val eventId: String = Uuid.random().toString(),
    override val properties: Map<String, Any> = emptyMap(),
    override val timestamp: Instant = Clock.System.now(),
    override val userId: String? = null,
    override val sessionId: String? = null,
    override val schema: SchemaVersion = SchemaVersion("eyespie.system", 1)
) : TelemetryEvent {

    data class Crash(override val eventId: String = Uuid.random().toString()) : SystemEvent(eventId = eventId)
    data class Error(override val eventId: String = Uuid.random().toString()) : SystemEvent(eventId = eventId)
    data class Performance(
        override val eventId: String = Uuid.random().toString(),
        override val timestamp: Instant = Clock.System.now(),
        override val properties: Map<String, Any> = emptyMap(),
        override val schema: SchemaVersion = SchemaVersion("system.performance", 1)
    ) : SystemEvent(eventId = eventId, timestamp = timestamp, properties = properties, schema = schema)
}
