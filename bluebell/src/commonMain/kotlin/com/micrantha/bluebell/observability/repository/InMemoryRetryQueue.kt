package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.domain.MutableThreadSafeMap
import com.micrantha.bluebell.observability.domain.PersistentRetryQueue
import com.micrantha.bluebell.observability.domain.RetryQueue
import com.micrantha.bluebell.observability.entity.AuditEvent
import com.micrantha.bluebell.observability.entity.RetryableEvent
import com.micrantha.bluebell.observability.entity.SystemEvent
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

// ============================================================================
// SUPPORTING IMPLEMENTATIONS
// ============================================================================

// Retry Queue Implementation
class InMemoryRetryQueue(
    private val maxQueueSize: Int = 10000,
    private val persistQueue: PersistentRetryQueue? = null
) : RetryQueue {

    private val queue = MutableThreadSafeMap<String, RetryableEvent>()
    private val lock = Mutex()
    private val logger by logger()

    override suspend fun add(event: RetryableEvent) = lock.withLock {
        if (queue.size() >= maxQueueSize) {
            evictOldest()
        }

        queue[event.event.eventId] = event

        if (event.event.isPersistable()) {
            persistQueue?.add(event)
        }
    }

    override suspend fun addBatch(events: List<RetryableEvent>) = lock.withLock {
        events.forEach { event ->
            if (queue.size() < maxQueueSize) {
                queue[event.event.eventId] = event

                if (event.event.isPersistable()) {
                    persistQueue?.add(event)
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun getReady(): List<RetryableEvent> = lock.withLock {
        queue.values()
            .filter { it.isReady() && it.canRetry() }
            .sortedBy { it.retryAfter }
    }

    override suspend fun remove(eventIds: List<String>) = lock.withLock {
        eventIds.forEach { eventId ->
            queue.remove(eventId)
            persistQueue?.remove(listOf(eventId))
        }
    }

    override suspend fun markFailed(eventId: String, error: Throwable) = lock.withLock {
        val event = queue[eventId] ?: return@withLock

        if (!event.canRetry()) {
            queue.remove(eventId)
            persistQueue?.remove(listOf(eventId))
            logPermanentFailure(event, error)
        } else {
            val delay = calculateBackoff(event.attempts)
            val updated = event.withRetry(error, delay)
            queue[eventId] = updated

            if (event.event.isPersistable()) {
                persistQueue?.update(updated)
            }
        }
    }

    override suspend fun clear(): Unit = lock.withLock {
        queue.clear()
        persistQueue?.clear()
    }

    override suspend fun size(): Int = queue.size()

    override suspend fun isEmpty(): Boolean = queue.isEmpty()

    @OptIn(ExperimentalTime::class)
    private suspend fun evictOldest() {
        val toEvict = queue.values()
            .filter { !it.event.isPersistable() }
            .minByOrNull { it.firstAttempt }

        toEvict?.let { queue.remove(it.event.eventId) }
    }

    private fun calculateBackoff(attempts: Int): Duration {
        val baseDelay = 5.seconds
        val multiplier = 2.0.pow(attempts.toDouble())
        val jitter = Random.nextDouble(0.8, 1.2)
        val delayMs = (baseDelay.inWholeMilliseconds * multiplier * jitter).toLong()
        return minOf(delayMs, 5.minutes.inWholeMilliseconds).milliseconds
    }

    private fun logPermanentFailure(event: RetryableEvent, error: Throwable) {
        logger.error(error) {
            "Event ${event.event.eventId} permanently failed after ${event.attempts} attempts " +
                    "to ${event.destination}"
        }
    }

    private fun TelemetryEvent.isPersistable(): Boolean = when (this) {
        is AuditEvent -> true
        is SystemEvent.Crash -> true
        else -> false
    }
}
