package com.micrantha.bluebell.observability.repository.destination

import com.micrantha.bluebell.observability.domain.DestinationUnavailableException
import com.micrantha.bluebell.observability.domain.EventDestination
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.BatchSendResult
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationConfig
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationMetrics
import com.micrantha.bluebell.observability.entity.FirebaseConfig
import com.micrantha.bluebell.observability.entity.FlushResult
import com.micrantha.bluebell.observability.entity.HealthStatus
import com.micrantha.bluebell.observability.entity.RejectedEvent
import com.micrantha.bluebell.observability.entity.RejectionReason
import com.micrantha.bluebell.observability.entity.SendResult
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class FirebaseDestination(
    private val analytics: Any,
    private var config: FirebaseConfig
) : EventDestination {

    override val destination = Destination.FIREBASE
    override var isEnabled = config.enabled

    private val metrics = AtomicDestinationMetrics()
    private val eventQueue = mutableListOf<Pair<TelemetryEvent, DestinationContext>>()
    private val queueLock = Mutex()

    @OptIn(ExperimentalTime::class)
    override suspend fun send(
        event: TelemetryEvent,
        context: DestinationContext
    ): Result<SendResult> = withContext(Dispatchers.IO) {
        if (!isEnabled) {
            return@withContext Result.failure(
                DestinationUnavailableException(destination, "Destination is disabled")
            )
        }

        val startTime = Clock.System.now().toEpochMilliseconds()

        runCatching {
            when (event) {
                is AnalyticsEvent -> {
//                    val bundle = convertToBundle(event, context)
//                    analytics.logEvent(getEventName(event), bundle)

                    val latency = Clock.System.now().toEpochMilliseconds() - startTime
                    metrics.recordSuccess(latency)

                    SendResult(
                        eventId = event.eventId,
                        accepted = true,
                        destination = destination,
                        latencyMs = latency
                    )
                }

                else -> {
                    // Queue non-analytics events
                    queueLock.withLock {
                        eventQueue.add(event to context)
                    }

                    SendResult(
                        eventId = event.eventId,
                        accepted = false,
                        destination = destination,
                        latencyMs = 0,
                        metadata = mapOf("status" to "queued")
                    )
                }
            }
        }.onFailure { error ->
            metrics.recordFailure(0)
        }
    }

    override suspend fun sendBatch(
        events: List<TelemetryEvent>,
        context: DestinationContext
    ): Result<BatchSendResult> {
        // Firebase doesn't have native batching, send individually
        val results = events.map { send(it, context) }

        val accepted = results.count { it.isSuccess }
        val rejected = results.mapIndexedNotNull { index, result ->
            result.exceptionOrNull()?.let {
                RejectedEvent(
                    eventId = events[index].eventId,
                    reason = RejectionReason.Other("send_failed"),
                    message = it.message ?: "Unknown error"
                )
            }
        }

        return Result.success(
            BatchSendResult(
                totalEvents = events.size,
                acceptedEvents = accepted,
                rejectedEvents = rejected,
                latencyMs = 0
            )
        )
    }

    override suspend fun flush(): Result<FlushResult> {
        return try {
            val flushed = queueLock.withLock {
                val count = eventQueue.size
                eventQueue.clear()
                count
            }

            Result.success(
                FlushResult(
                    flushedCount = flushed,
                    failedCount = 0,
                    durationMs = 0
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun healthCheck(): HealthStatus {
        return HealthStatus(
            isHealthy = config.analyticsCollectionEnabled,
            status = if (config.analyticsCollectionEnabled)
                HealthStatus.Status.HEALTHY
            else
                HealthStatus.Status.DEGRADED,
            lastSuccessfulSend = Clock.System.now(),
            lastError = null,
            consecutiveFailures = 0,
            details = mapOf(
                "project_id" to config.projectId,
                "queue_size" to eventQueue.size
            )
        )
    }

    @OptIn(ExperimentalTime::class)
    override fun getMetrics(): DestinationMetrics = metrics.toMetrics().copy(
        queueSize = eventQueue.size
    )

    override suspend fun configure(config: DestinationConfig): Result<Unit> = runCatching {
        require(config is FirebaseConfig) { "Invalid config type" }
        this.config = config
        this.isEnabled = config.enabled
//        analytics.setAnalyticsCollectionEnabled(config.analyticsCollectionEnabled)
    }

    override suspend fun enable() {
        isEnabled = true
//        analytics.setAnalyticsCollectionEnabled(true)
    }

    override suspend fun disable() {
        isEnabled = false
//        analytics.setAnalyticsCollectionEnabled(false)
    }
}
