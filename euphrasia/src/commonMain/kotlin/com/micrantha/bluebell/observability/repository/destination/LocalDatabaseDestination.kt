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
        TODO("Not yet implemented")
    }

    override suspend fun sendBatch(
        events: List<TelemetryEvent>,
        context: DestinationContext
    ): Result<BatchSendResult> {
        TODO("Not yet implemented")
    }

    override suspend fun flush(): Result<FlushResult> {
        TODO("Not yet implemented")
    }

    override suspend fun healthCheck(): HealthStatus {
        TODO("Not yet implemented")
    }

    override fun getMetrics(): DestinationMetrics {
        TODO("Not yet implemented")
    }

    override suspend fun configure(config: DestinationConfig): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun enable() {
        TODO("Not yet implemented")
    }

    override suspend fun disable() {
        TODO("Not yet implemented")
    }
}
