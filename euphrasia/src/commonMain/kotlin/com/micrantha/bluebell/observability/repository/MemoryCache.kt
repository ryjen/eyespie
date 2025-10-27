package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.entity.CachedEvent
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// In-memory cache implementation
class MemoryCache {
    private val cache = LinkedHashMap<String, CachedEvent>(16, 0.75f) // LRU
    private val lock = Mutex()

    @OptIn(ExperimentalTime::class)
    suspend fun put(event: TelemetryEvent) = lock.withLock {
        cache[event.eventId] = CachedEvent(
            event = event,
            cachedAt = Clock.System.now()
        )
    }

    suspend fun getAll(limit: Int = Int.MAX_VALUE): List<TelemetryEvent> = lock.withLock {
        cache.values.take(limit).map { it.event }
    }

    suspend fun filter(filter: EventFilter, limit: Int): List<TelemetryEvent> = lock.withLock {
        cache.values
            .map { it.event }
            .filter { it.matchesFilter(filter) }
            .take(limit)
    }

    suspend fun remove(eventIds: List<String>) = lock.withLock {
        eventIds.forEach { cache.remove(it) }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun removeOlderThan(timestamp: Instant): Int = lock.withLock {
        val toRemove = cache.values
            .filter { it.event.timestamp < timestamp }
            .map { it.event.eventId }

        toRemove.forEach { cache.remove(it) }
        toRemove.size
    }

    suspend fun getLRU(count: Int): List<TelemetryEvent> = lock.withLock {
        cache.values.take(count).map { it.event }
    }

    fun size(): Int = cache.size

    suspend fun clear() = lock.withLock {
        cache.clear()
    }

    fun isEmpty(): Boolean = cache.isEmpty()
}

private fun TelemetryEvent.matchesFilter(filter: EventFilter): Boolean {
    if (filter.eventTypes != null && this::class.simpleName !in filter.eventTypes) {
        return false
    }
    if (filter.userId != null && this.userId != filter.userId) {
        return false
    }
    filter.properties?.forEach { (key, value) ->
        if (this.properties[key] != value) {
            return false
        }
    }
    return true
}
