package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.domain.EventCache
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SQLiteEventCache(
    private val database: Any
) : EventCache {

    override suspend fun store(event: TelemetryEvent): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun storeBatch(events: List<TelemetryEvent>): Result<Unit> =
        withContext(Dispatchers.IO) {
            Result.success(Unit)
        }

    override suspend fun retrieve(limit: Int): List<TelemetryEvent> =
        withContext(Dispatchers.IO) {
            emptyList()
        }

    override suspend fun retrieveByFilter(filter: EventFilter, limit: Int): List<TelemetryEvent> =
        withContext(Dispatchers.IO) {
            emptyList()
        }

    override suspend fun delete(eventIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            Result.success(Unit)
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun deleteOlderThan(timestamp: Instant): Result<Int> =
        withContext(Dispatchers.IO) {
            Result.success(0)
        }

    override suspend fun count(): Int = withContext(Dispatchers.IO) {
        0
    }

    override suspend fun flush(): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun isEmpty(): Boolean = count() == 0
}

