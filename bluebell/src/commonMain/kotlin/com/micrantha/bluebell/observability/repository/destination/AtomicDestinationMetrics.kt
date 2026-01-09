package com.micrantha.bluebell.observability.repository.destination

import com.micrantha.bluebell.observability.entity.DestinationMetrics
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Atomic metrics tracking
@OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)
class AtomicDestinationMetrics {
    private val totalSent = AtomicLong(0)
    private val totalAccepted = AtomicLong(0)
    private val totalRejected = AtomicLong(0)
    private val totalBatches = AtomicLong(0)
    private val totalLatency = AtomicLong(0)
    private val totalErrors = AtomicLong(0)
    private val bytesTransferred = AtomicLong(0)
    private var lastSendTime: Instant? = null

    fun recordSuccess(latencyMs: Long) {
        totalSent.incrementAndFetch()
        totalAccepted.incrementAndFetch()
        totalLatency.addAndFetch(latencyMs)
        lastSendTime = Clock.System.now()
    }

    fun recordBatchSuccess(count: Int, latencyMs: Long) {
        totalSent.addAndFetch(count.toLong())
        totalAccepted.addAndFetch(count.toLong())
        totalBatches.incrementAndFetch()
        totalLatency.addAndFetch(latencyMs)
        lastSendTime = Clock.System.now()
    }

    fun recordFailure(latencyMs: Long) {
        totalSent.incrementAndFetch()
        totalRejected.incrementAndFetch()
        totalErrors.incrementAndFetch()
        totalLatency.addAndFetch(latencyMs)
    }

    fun toMetrics(): DestinationMetrics {
        val sent = totalSent.load()
        val avgLatency = if (sent > 0) totalLatency.load().toDouble() / sent else 0.0
        val errorRate = if (sent > 0) totalErrors.load().toDouble() / sent else 0.0

        return DestinationMetrics(
            totalEventsSent = sent,
            totalEventsAccepted = totalAccepted.load(),
            totalEventsRejected = totalRejected.load(),
            totalBatchesSent = totalBatches.load(),
            averageLatencyMs = avgLatency,
            errorRate = errorRate,
            lastSendTime = lastSendTime,
            queueSize = 0,
            bytesTransferred = bytesTransferred.load()
        )
    }
}
