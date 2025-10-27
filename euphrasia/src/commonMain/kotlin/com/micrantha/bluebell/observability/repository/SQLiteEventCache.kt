package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.domain.DiskCache
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SQLiteEventCache(
    private val database: Any
) : DiskCache {

    override suspend fun write(event: TelemetryEvent): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
//            database.eventDao().insert(event.toEntity())
            TODO("Not yet implemented")
        }
    }

    override suspend fun writeBatch(events: List<TelemetryEvent>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
//                database.eventDao().insertAll(events.map { it.toEntity() })
                TODO("Not yet implemented")

            }
        }

    override suspend fun readOldest(limit: Int): List<TelemetryEvent> =
        withContext(Dispatchers.IO) {
//            database.eventDao()
//                .getOldest(limit)
//                .map { it.toEvent() }
            TODO("Not yet implemented")

        }

    override suspend fun query(filter: EventFilter, limit: Int): List<TelemetryEvent> =
        withContext(Dispatchers.IO) {
//            database.eventDao()
//                .query(filter, limit)
//                .map { it.toEvent() }
            TODO("Not yet implemented")

        }

    override suspend fun delete(eventIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
//                database.eventDao().deleteByIds(eventIds)
                TODO("Not yet implemented")

            }
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun deleteOlderThan(timestamp: Instant): Int =
        withContext(Dispatchers.IO) {
//            database.eventDao().deleteOlderThan(timestamp.toEpochMilliseconds())
            TODO("Not yet implemented")
        }

    override suspend fun count(): Int = withContext(Dispatchers.IO) {
//        database.eventDao().count()
        TODO("Not yet implemented")
    }

    override suspend fun isEmpty(): Boolean = count() == 0
}
