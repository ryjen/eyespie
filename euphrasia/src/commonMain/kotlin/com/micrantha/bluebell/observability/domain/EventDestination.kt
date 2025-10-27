package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.BatchSendResult
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationConfig
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationMetrics
import com.micrantha.bluebell.observability.entity.FlushResult
import com.micrantha.bluebell.observability.entity.HealthStatus
import com.micrantha.bluebell.observability.entity.SendResult
import com.micrantha.bluebell.observability.entity.TelemetryEvent

interface EventDestination {
    val destination: Destination
    val isEnabled: Boolean

    // Core operations
    suspend fun send(
        event: TelemetryEvent,
        context: DestinationContext
    ): Result<SendResult>

    suspend fun sendBatch(
        events: List<TelemetryEvent>,
        context: DestinationContext
    ): Result<BatchSendResult>

    suspend fun flush(): Result<FlushResult>

    // Health and status
    suspend fun healthCheck(): HealthStatus
    fun getMetrics(): DestinationMetrics

    // Configuration
    suspend fun configure(config: DestinationConfig): Result<Unit>
    suspend fun enable()
    suspend fun disable()
}
