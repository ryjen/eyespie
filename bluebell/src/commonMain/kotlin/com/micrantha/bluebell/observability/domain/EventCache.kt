package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface EventCache {
    suspend fun store(event: TelemetryEvent): Result<Unit>
    suspend fun storeBatch(events: List<TelemetryEvent>): Result<Unit>
    suspend fun retrieve(limit: Int = 100): List<TelemetryEvent>
    suspend fun retrieveByFilter(filter: EventFilter, limit: Int = 100): List<TelemetryEvent>
    suspend fun delete(eventIds: List<String>): Result<Unit>

    @OptIn(ExperimentalTime::class)
    suspend fun deleteOlderThan(timestamp: Instant): Result<Int>
    suspend fun count(): Int
    suspend fun flush(): Result<Unit>
    suspend fun isEmpty(): Boolean
}
