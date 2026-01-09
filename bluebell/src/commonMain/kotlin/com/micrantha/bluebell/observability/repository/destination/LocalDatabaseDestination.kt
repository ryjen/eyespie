package com.micrantha.bluebell.observability.repository.destination

import com.micrantha.bluebell.observability.domain.EventDestination
import com.micrantha.bluebell.observability.entity.BatchSendResult
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationConfig
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationMetrics
import com.micrantha.bluebell.observability.entity.FlushResult
import com.micrantha.bluebell.observability.entity.HealthStatus
import com.micrantha.bluebell.observability.entity.LocalDatabaseConfig
import com.micrantha.bluebell.observability.entity.SendResult
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LocalDatabaseDestination(
    private val db: Any,
    config: LocalDatabaseConfig = LocalDatabaseConfig()
) : EventDestination {

    override val destination = Destination.LOCAL_DB
    override var isEnabled = config.enabled

    override suspend fun send(
        event: TelemetryEvent,
        context: DestinationContext
    ): Result<SendResult> {
        return Result.success(
            SendResult(
                eventId = event.eventId,
                accepted = false,
                destination = destination,
                latencyMs = 0,
                metadata = mapOf("reason" to "not_implemented")
            )
        )
    }

    override suspend fun sendBatch(
        events: List<TelemetryEvent>,
        context: DestinationContext
    ): Result<BatchSendResult> {
        return Result.success(
            BatchSendResult(
                totalEvents = events.size,
                acceptedEvents = 0,
                rejectedEvents = emptyList(),
                latencyMs = 0,
                metadata = mapOf("reason" to "not_implemented")
            )
        )
    }

    override suspend fun flush(): Result<FlushResult> {
        return Result.success(FlushResult(flushedCount = 0, failedCount = 0, durationMs = 0))
    }

    override suspend fun healthCheck(): HealthStatus {
        return HealthStatus(
            isHealthy = false,
            status = HealthStatus.Status.UNKNOWN,
            lastSuccessfulSend = null,
            lastError = "not_implemented",
            consecutiveFailures = 0,
            details = mapOf("reason" to "not_implemented")
        )
    }

    override fun getMetrics(): DestinationMetrics {
        return DestinationMetrics(
            totalEventsSent = 0,
            totalEventsAccepted = 0,
            totalEventsRejected = 0,
            totalBatchesSent = 0,
            averageLatencyMs = 0.0,
            errorRate = 0.0,
            lastSendTime = null,
            queueSize = 0,
            bytesTransferred = 0
        )
    }

    override suspend fun configure(config: DestinationConfig): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun enable() {
        isEnabled = true
    }

    override suspend fun disable() {
        isEnabled = false
    }
}
