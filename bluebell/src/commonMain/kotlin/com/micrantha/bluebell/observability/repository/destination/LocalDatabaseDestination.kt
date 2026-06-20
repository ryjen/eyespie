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
import kotlin.time.Clock
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
                accepted = false,
                successfulIds = emptyList(),
                failedEvents = emptyList(),
                latencyMs = 0
            )
        )
    }

    override suspend fun flush(): Result<FlushResult> {
        return Result.success(FlushResult(eventsProcessed = 0, success = true))
    }

    override suspend fun healthCheck(): HealthStatus {
        return HealthStatus(
            isHealthy = false,
            lastCheck = Clock.System.now(),
            details = mapOf("reason" to "not_implemented")
        )
    }

    override fun getMetrics(): DestinationMetrics {
        return DestinationMetrics(
            totalSent = 0,
            totalFailed = 0,
            queueSize = 0,
            lastSendTime = null,
            avgLatencyMs = 0.0
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

