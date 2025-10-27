package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.domain.DiskCache
import com.micrantha.bluebell.observability.domain.EventCache
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.AuditEvent
import com.micrantha.bluebell.observability.entity.CacheConfig
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.SystemEvent
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class MultiTierEventCache(
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache,
    private val config: CacheConfig
) : EventCache {

    override suspend fun store(event: TelemetryEvent): Result<Unit> = runCatching {
        // Store in memory first for fast access
        memoryCache.put(event)

        // Persist to disk if memory is full or event is important
        if (memoryCache.size() >= config.maxMemoryEvents || event.isPersistable()) {
            diskCache.write(event)
        }

        // Evict old events if necessary
        if (memoryCache.size() > config.maxMemoryEvents) {
            evictLRU()
        }
    }

    override suspend fun storeBatch(events: List<TelemetryEvent>): Result<Unit> = runCatching {
        events.forEach { event ->
            memoryCache.put(event)
        }

        // Write persistable events to disk in batch
        val persistable = events.filter { it.isPersistable() }
        if (persistable.isNotEmpty()) {
            diskCache.writeBatch(persistable)
        }
    }

    override suspend fun retrieve(limit: Int): List<TelemetryEvent> {
        val memoryEvents = memoryCache.getAll(limit)

        return if (memoryEvents.size < limit) {
            // Need more events from disk
            val remaining = limit - memoryEvents.size
            val diskEvents = diskCache.readOldest(remaining)
            memoryEvents + diskEvents
        } else {
            memoryEvents.take(limit)
        }
    }

    override suspend fun retrieveByFilter(
        filter: EventFilter,
        limit: Int
    ): List<TelemetryEvent> {
        val memoryResults = memoryCache.filter(filter, limit)

        return if (memoryResults.size < limit) {
            val remaining = limit - memoryResults.size
            val diskResults = diskCache.query(filter, remaining)
            memoryResults + diskResults
        } else {
            memoryResults
        }
    }

    override suspend fun delete(eventIds: List<String>): Result<Unit> = runCatching {
        memoryCache.remove(eventIds)
        diskCache.delete(eventIds)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun deleteOlderThan(timestamp: Instant): Result<Int> = runCatching {
        val memoryDeleted = memoryCache.removeOlderThan(timestamp)
        val diskDeleted = diskCache.deleteOlderThan(timestamp)
        memoryDeleted + diskDeleted
    }

    override suspend fun count(): Int {
        return memoryCache.size() + diskCache.count()
    }

    override suspend fun flush(): Result<Unit> = runCatching {
        val allEvents = memoryCache.getAll()
        diskCache.writeBatch(allEvents)
        memoryCache.clear()
    }

    override suspend fun isEmpty(): Boolean {
        return memoryCache.isEmpty() && diskCache.isEmpty()
    }

    private suspend fun evictLRU() {
        val toEvict = memoryCache.getLRU(config.evictionBatchSize)
        toEvict.forEach { event ->
            if (event.isPersistable()) {
                diskCache.write(event)
            }
            memoryCache.remove(listOf(event.eventId))
        }
    }

    private fun TelemetryEvent.isPersistable(): Boolean = when (this) {
        is AuditEvent -> true  // Always persist for non-repudiation
        is SystemEvent.Crash -> true  // Important for debugging
        is AnalyticsEvent -> false  // Can be lost if not synced
        else -> false
    }
}
