package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Disk cache implementation (using SQLite or similar)
interface DiskCache {
    suspend fun write(event: TelemetryEvent): Result<Unit>
    suspend fun writeBatch(events: List<TelemetryEvent>): Result<Unit>
    suspend fun readOldest(limit: Int): List<TelemetryEvent>
    suspend fun query(filter: EventFilter, limit: Int): List<TelemetryEvent>
    suspend fun delete(eventIds: List<String>): Result<Unit>

    @OptIn(ExperimentalTime::class)
    suspend fun deleteOlderThan(timestamp: Instant): Int
    suspend fun count(): Int
    suspend fun isEmpty(): Boolean
}
