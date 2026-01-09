package com.micrantha.bluebell.observability.repository.destination

import com.micrantha.bluebell.observability.domain.DestinationRejectionException
import com.micrantha.bluebell.observability.domain.DestinationUnavailableException
import com.micrantha.bluebell.observability.domain.EventDestination
import com.micrantha.bluebell.observability.entity.BatchSendResult
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationConfig
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationMetrics
import com.micrantha.bluebell.observability.entity.FlushResult
import com.micrantha.bluebell.observability.entity.HealthStatus
import com.micrantha.bluebell.observability.entity.NewRelicConfig
import com.micrantha.bluebell.observability.entity.RejectedEvent
import com.micrantha.bluebell.observability.entity.RejectionReason
import com.micrantha.bluebell.observability.entity.SendResult
import com.micrantha.bluebell.observability.entity.SystemEvent
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class NewRelicDestination(
    private val client: NewRelicClient,
    private var config: NewRelicConfig
) : EventDestination {

    override val destination = Destination.NEW_RELIC
    override var isEnabled = config.enabled

    private val metrics = AtomicDestinationMetrics()

    @OptIn(ExperimentalTime::class)
    private var lastSuccessfulSend: Instant? = null
    private var lastError: String? = null
    private var consecutiveFailures = 0

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
            // Convert to NewRelic format
            val nrEvent = when (event) {
                is SystemEvent.Crash -> convertToCrash(event, context)
                is SystemEvent.Error -> convertToError(event, context)
                is SystemEvent.Performance -> convertToMetric(event, context)
                else -> throw DestinationRejectionException(
                    destination = destination,
                    eventId = event.eventId,
                    reason = "Event type not supported by NewRelic"
                )
            }

            // Send with timeout
            withTimeout(config.timeout) {
                client.sendEvent(nrEvent)
            }

            val latency = Clock.System.now().toEpochMilliseconds() - startTime

            // Update metrics
            metrics.recordSuccess(latency)
            lastSuccessfulSend = Clock.System.now()
            consecutiveFailures = 0

            SendResult(
                eventId = event.eventId,
                accepted = true,
                destination = destination,
                latencyMs = latency,
                metadata = mapOf("nr_event_type" to nrEvent.eventType)
            )
        }.onFailure { error ->
            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            metrics.recordFailure(latency)
            lastError = error.message
            consecutiveFailures++
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun sendBatch(
        events: List<TelemetryEvent>,
        context: DestinationContext
    ): Result<BatchSendResult> = withContext(Dispatchers.IO) {
        if (!isEnabled) {
            return@withContext Result.failure(
                DestinationUnavailableException(destination, "Destination is disabled")
            )
        }

        val startTime = Clock.System.now().toEpochMilliseconds()
        val rejected = mutableListOf<RejectedEvent>()

        runCatching {
            // Convert all events
            val nrEvents = events.mapNotNull { event ->
                try {
                    when (event) {
                        is SystemEvent.Crash -> convertToCrash(event, context)
                        is SystemEvent.Error -> convertToError(event, context)
                        is SystemEvent.Performance -> convertToMetric(event, context)
                        else -> {
                            rejected.add(
                                RejectedEvent(
                                    eventId = event.eventId,
                                    reason = RejectionReason.Other("unsupported_type"),
                                    message = "Event type not supported"
                                )
                            )
                            null
                        }
                    }
                } catch (e: Exception) {
                    rejected.add(
                        RejectedEvent(
                            eventId = event.eventId,
                            reason = RejectionReason.ValidationFailed(
                                listOf(
                                    e.message ?: "Unknown"
                                )
                            ),
                            message = "Conversion failed"
                        )
                    )
                    null
                }
            }

            // Send batch
            withTimeout(config.timeout) {
                client.sendBatch(nrEvents)
            }

            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            val accepted = nrEvents.size

            metrics.recordBatchSuccess(accepted, latency)
            lastSuccessfulSend = Clock.System.now()
            consecutiveFailures = 0

            BatchSendResult(
                totalEvents = events.size,
                acceptedEvents = accepted,
                rejectedEvents = rejected,
                latencyMs = latency
            )
        }.onFailure { error ->
            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            metrics.recordFailure(latency)
            lastError = error.message
            consecutiveFailures++
        }
    }

    override suspend fun flush(): Result<FlushResult> {
        // NewRelic typically doesn't require explicit flushing
        return Result.success(FlushResult(0, 0, 0))
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun healthCheck(): HealthStatus {
        return try {
            val pingResult = client.ping()
            HealthStatus(
                isHealthy = pingResult,
                status = when {
                    !pingResult -> HealthStatus.Status.UNHEALTHY
                    consecutiveFailures > 5 -> HealthStatus.Status.DEGRADED
                    else -> HealthStatus.Status.HEALTHY
                },
                lastSuccessfulSend = lastSuccessfulSend,
                lastError = lastError,
                consecutiveFailures = consecutiveFailures,
                details = mapOf(
                    "account_id" to config.accountId,
                    "region" to config.region
                )
            )
        } catch (e: Exception) {
            HealthStatus(
                isHealthy = false,
                status = HealthStatus.Status.UNHEALTHY,
                lastSuccessfulSend = lastSuccessfulSend,
                lastError = e.message,
                consecutiveFailures = consecutiveFailures
            )
        }
    }

    override fun getMetrics(): DestinationMetrics = metrics.toMetrics()

    override suspend fun configure(config: DestinationConfig): Result<Unit> = runCatching {
        require(config is NewRelicConfig) { "Invalid config type" }
        this.config = config
        this.isEnabled = config.enabled
    }

    override suspend fun enable() {
        isEnabled = true
    }

    override suspend fun disable() {
        isEnabled = false
    }

    @OptIn(ExperimentalTime::class)
    private fun convertToCrash(
        event: SystemEvent.Crash,
        context: DestinationContext
    ): NewRelicEvent {
        return NewRelicEvent(
            eventType = "MobileError",
            timestamp = event.timestamp.toEpochMilliseconds(),
            attributes = buildMap {
                put("error.class", event.properties["error_class"] ?: "Unknown")
                put("error.message", event.properties["error_message"] ?: "")
                put("error.stacktrace", event.properties["stacktrace"] ?: "")
                put("appVersion", context.deviceInfo?.appVersion)
                put("osVersion", context.deviceInfo?.osVersion)
                put("deviceId", context.deviceInfo?.deviceId)
                context.userId?.let { put("userId", it) }
                context.sessionId?.let { put("sessionId", it) }
                putAll(context.globalProperties)
                putAll(config.customAttributes)
            }
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun convertToError(
        event: SystemEvent.Error,
        context: DestinationContext
    ): NewRelicEvent {
        return NewRelicEvent(
            eventType = "MobileHandledException",
            timestamp = event.timestamp.toEpochMilliseconds(),
            attributes = buildMap {
                put("error.message", event.properties["message"] ?: "")
                put("error.severity", event.properties["severity"] ?: "error")
                context.userId?.let { put("userId", it) }
                context.sessionId?.let { put("sessionId", it) }
                putAll(context.globalProperties)
            }
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun convertToMetric(
        event: SystemEvent.Performance,
        context: DestinationContext
    ): NewRelicEvent {
        return NewRelicEvent(
            eventType = "Mobile",
            timestamp = event.timestamp.toEpochMilliseconds(),
            attributes = buildMap {
                putAll(event.properties)
                context.userId?.let { put("userId", it) }
                context.sessionId?.let { put("sessionId", it) }
                putAll(context.globalProperties)
            }
        )
    }
}

// NewRelic client models
data class NewRelicEvent(
    val eventType: String,
    val timestamp: Long,
    val attributes: Map<String, Any?>
)

interface NewRelicClient {
    suspend fun sendEvent(event: NewRelicEvent)
    suspend fun sendBatch(events: List<NewRelicEvent>)
    suspend fun ping(): Boolean
}
