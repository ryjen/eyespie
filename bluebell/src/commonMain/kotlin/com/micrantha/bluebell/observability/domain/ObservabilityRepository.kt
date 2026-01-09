package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.BatchResult
import com.micrantha.bluebell.observability.entity.Campaign
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationStatus
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.EventSchema
import com.micrantha.bluebell.observability.entity.EventSpan
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.entity.TimeRange
import com.micrantha.bluebell.observability.entity.ValidationResult
import kotlinx.coroutines.flow.Flow

interface ObservabilityRepository {
    // ===== Event Recording =====
    suspend fun record(event: TelemetryEvent): Result<Unit>
    suspend fun recordBatch(events: List<TelemetryEvent>): Result<BatchResult>

    // ===== Span Management =====
    suspend fun startSpan(name: String, attributes: Map<String, Any> = emptyMap()): EventSpan
    suspend fun endSpan(spanId: String): Result<Unit>
    suspend fun recordInSpan(spanId: String, event: TelemetryEvent): Result<Unit>

    // ===== Campaign Tracking =====
    suspend fun registerCampaign(campaign: Campaign): Result<Unit>
    suspend fun getActiveCampaigns(): List<Campaign>
    suspend fun getCampaignEvents(campaignId: String): Flow<TelemetryEvent>

    // ===== Property Management =====
    suspend fun setGlobalProperties(properties: Map<String, Any>)
    suspend fun setUserProperties(properties: Map<String, Any>)
    suspend fun clearProperties(keys: Set<String>)

    // ===== Schema Management =====
    suspend fun registerSchema(schema: EventSchema): Result<Unit>
    suspend fun validateEvent(event: TelemetryEvent): ValidationResult
    suspend fun migrateEvent(event: TelemetryEvent, targetVersion: SchemaVersion): TelemetryEvent

    // ===== Query & Retrieval (for non-repudiation) =====
    suspend fun queryEvents(
        filter: EventFilter,
        timeRange: TimeRange,
        limit: Int = 100
    ): Flow<TelemetryEvent>

    suspend fun getEventsByUser(userId: String, timeRange: TimeRange): Flow<TelemetryEvent>
    suspend fun getEventsBySession(sessionId: String): Flow<TelemetryEvent>

    // ===== Destination Management =====
    suspend fun flush(): Result<Unit>
    suspend fun setDestinationEnabled(destination: Destination, enabled: Boolean)
    suspend fun getDestinationStatus(): Map<Destination, DestinationStatus>
}
