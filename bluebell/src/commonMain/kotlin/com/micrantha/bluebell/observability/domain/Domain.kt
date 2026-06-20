package com.micrantha.bluebell.observability.domain

import com.micrantha.bluebell.observability.entity.BatchResult
import com.micrantha.bluebell.observability.entity.BatchSendResult
import com.micrantha.bluebell.observability.entity.Campaign
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationConfig
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationMetrics
import com.micrantha.bluebell.observability.entity.DestinationStatus
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.EventSchema
import com.micrantha.bluebell.observability.entity.EventSpan
import com.micrantha.bluebell.observability.entity.FlushResult
import com.micrantha.bluebell.observability.entity.HealthStatus
import com.micrantha.bluebell.observability.entity.RetryableEvent
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.SendResult
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.entity.TimeRange
import com.micrantha.bluebell.observability.entity.ValidationError
import com.micrantha.bluebell.observability.entity.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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

    // ===== Query & Retrieval =====
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

interface EventCache {
    suspend fun store(event: TelemetryEvent): Result<Unit>
    suspend fun storeBatch(events: List<TelemetryEvent>): Result<Unit>
    suspend fun retrieve(limit: Int = 100): List<TelemetryEvent>
    suspend fun retrieveByFilter(filter: EventFilter, limit: Int = 100): List<TelemetryEvent>
    suspend fun delete(eventIds: List<String>): Result<Unit>
    @OptIn(ExperimentalTime::class)
    suspend fun deleteOlderThan(timestamp: Instant): Result<Int>
    suspend fun count(): Int
    suspend fun flush(): Result<Unit>
    suspend fun isEmpty(): Boolean
}

interface EventDestination {
    val destination: Destination
    val isEnabled: Boolean
    suspend fun send(event: TelemetryEvent, context: DestinationContext): Result<SendResult>
    suspend fun sendBatch(events: List<TelemetryEvent>, context: DestinationContext): Result<BatchSendResult>
    suspend fun flush(): Result<FlushResult>
    suspend fun healthCheck(): HealthStatus
    fun getMetrics(): DestinationMetrics
    suspend fun configure(config: DestinationConfig): Result<Unit>
    suspend fun enable()
    suspend fun disable()
}

interface SchemaRegistry {
    suspend fun register(schema: EventSchema): Result<Unit>
    suspend fun getSchema(version: SchemaVersion): EventSchema?
    suspend fun getLatestSchema(name: String): EventSchema?
    suspend fun getAllVersions(name: String): List<EventSchema>
    suspend fun deprecateSchema(version: SchemaVersion): Result<Unit>
    suspend fun validate(event: TelemetryEvent): ValidationResult
    suspend fun migrate(event: TelemetryEvent, version: SchemaVersion): TelemetryEvent
}

interface SchemaMigration {
    val sourceVersion: SchemaVersion
    val targetVersion: SchemaVersion
    fun migrate(event: TelemetryEvent): TelemetryEvent
}

interface RetryQueue {
    suspend fun add(event: RetryableEvent): Result<Unit>
    suspend fun getReady(): List<RetryableEvent>
    suspend fun remove(eventIds: List<String>): Result<Unit>
    suspend fun markFailed(eventId: String, error: Throwable): Result<Unit>
    suspend fun clear(): Result<Unit>
    suspend fun size(): Int
    suspend fun isEmpty(): Boolean
}

interface PersistentRetryQueue : RetryQueue {
    suspend fun addBatch(events: List<RetryableEvent>): Result<Unit>
    suspend fun update(event: RetryableEvent): Result<Unit>
}

fun interface SupabaseInsertClient {
    suspend fun insert(table: String, rows: List<Map<String, Any?>>)
}

fun interface SessionInfoProvider {
    fun get(): SessionInfo
}

data class SessionInfo(
    val userId: String? = null,
    val sessionId: String? = null
)

// Exceptions
open class ObservabilityException(message: String, cause: Throwable? = null) : Exception(message, cause)
class SchemaValidationException(val errors: List<ValidationError>) : ObservabilityException("Schema validation failed: $errors")
class EventTooLargeException(val eventId: String, val size: Long, val maxSize: Long) : ObservabilityException("Event $eventId too large: $size > $maxSize")
class SpanNotFoundException(val spanId: String) : ObservabilityException("Span $spanId not found")
class SpanAlreadyEndedException(val spanId: String) : ObservabilityException("Span $spanId already ended")
class CampaignAlreadyExistsException(val campaignId: String) : ObservabilityException("Campaign $campaignId already exists")
class CacheFullException : ObservabilityException("Cache is full")
class CacheWriteException(cause: Throwable) : ObservabilityException("Failed to write to cache", cause)
class SchemaNotFoundException(val version: SchemaVersion) : ObservabilityException("Schema $version not found")
class MigrationNotFoundException(val source: SchemaVersion, val target: SchemaVersion) : ObservabilityException("Migration from $source to $target not found")
class IncompatibleSchemaException(override val message: String) : ObservabilityException(message)
class DestinationUnavailableException(val destination: Destination) : ObservabilityException("Destination $destination unavailable")

open class DestinationException(val destination: Destination, message: String, val temporary: Boolean = true) : ObservabilityException("[$destination] $message")
class NetworkException(destination: Destination, message: String) : DestinationException(destination, message, true)
class RateLimitException(destination: Destination) : DestinationException(destination, "Rate limited", true)
class DestinationRejectionException(destination: Destination, val eventId: String, reason: String) : DestinationException(destination, "Event $eventId rejected: $reason", false)

fun Throwable.isRetryable(): Boolean = when (this) {
    is NetworkException -> true
    is RateLimitException -> true
    is DestinationException -> this.temporary
    is CacheFullException -> true
    is CacheWriteException -> true
    else -> false
}

@OptIn(ExperimentalTime::class)
fun RetryableEvent.isReady(): Boolean = Clock.System.now() >= retryAfter

fun RetryableEvent.canRetry(): Boolean = attempts < maxAttempts

@OptIn(ExperimentalTime::class)
fun RetryableEvent.withRetry(error: Throwable, delay: Duration): RetryableEvent = copy(
    attempts = attempts + 1,
    retryAfter = Clock.System.now() + delay,
    lastError = error.message
)

@OptIn(ExperimentalTime::class)
fun Throwable.getRetryDelay(): Duration = 30.seconds
fun Throwable.toFailureReason(): String = this::class.simpleName ?: "UnknownError"
