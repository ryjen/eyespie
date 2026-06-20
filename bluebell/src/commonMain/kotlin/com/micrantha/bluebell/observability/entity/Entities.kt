package com.micrantha.bluebell.observability.entity

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class ObservabilityConfig(
    val maxEventSize: Long = 100_000L,
    val maxRetryAttempts: Int = 3,
    val retryProcessInterval: Duration = 30.seconds,
    val enablePersistentQueue: Boolean = true,
    val maxQueueSize: Int = 10000
) {
    companion object {
        fun default() = ObservabilityConfig()
    }
}

data class SchemaVersion(val name: String, val version: Int)

data class EventSchema(
    val name: String,
    val version: Int,
    val description: String? = null,
    val properties: Map<String, PropertyType> = emptyMap(),
    val required: Set<String> = emptySet(),
    val deprecated: Boolean = false,
    val deprecatedFields: Set<String> = emptySet(),
    val fieldReplacements: Map<String, String> = emptyMap()
)

enum class PropertyType { STRING, NUMBER, BOOLEAN, OBJECT, ARRAY }

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val warnings: List<ValidationWarning> = emptyList()
)

data class ValidationError(val message: String, val reason: ValidationErrorReason, val field: String? = null)
enum class ValidationErrorReason { MISSING_PROPERTY, INVALID_TYPE, VALUE_OUT_OF_RANGE, SCHEMA_NOT_FOUND, MISSING_REQUIRED_FIELD }
data class ValidationWarning(val message: String, val field: String? = null, val suggestion: String? = null)


@OptIn(ExperimentalTime::class)
data class EventSpan(
    val spanId: String,
    val name: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val attributes: Map<String, Any> = emptyMap(),
    val events: List<TelemetryEvent> = emptyList()
)

@OptIn(ExperimentalTime::class)
data class Campaign(
    val campaignId: String,
    val name: String,
    val startDate: Instant,
    val endDate: Instant,
    val targetEvents: Set<String> = emptySet()
)

@OptIn(ExperimentalTime::class)
data class TimeRange(val start: Instant, val end: Instant)

data class EventFilter(
    val userId: String? = null,
    val eventType: String? = null,
    val campaignId: String? = null,
    val properties: Map<String, Any> = emptyMap(),
    val eventTypes: List<String>? = null
)

@OptIn(ExperimentalTime::class)
data class DestinationStatus(
    val isEnabled: Boolean,
    val isHealthy: Boolean,
    val lastSync: Instant? = null,
    val pendingEvents: Int = 0
)

@OptIn(ExperimentalTime::class)
data class DestinationContext(
    val campaigns: List<Campaign> = emptyList(),
    val spanId: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val globalProperties: Map<String, Any> = emptyMap(),
    val deviceInfo: DeviceInfo? = null,
    val networkInfo: NetworkInfo? = null,
    val timestamp: Instant,
    val retryAttempt: Int = 0,
    val traceContext: TraceContext? = null
)

data class DeviceInfo(
    val deviceId: String,
    val platform: String,
    val osVersion: String,
    val appVersion: String
)

data class NetworkInfo(
    val type: String,
    val isConnected: Boolean,
    val signalStrength: Int? = null
)

data class TraceContext(
    val traceId: String,
    val spanId: String,
    val parentSpanId: String? = null,
    val traceFlags: Int = 0
)

enum class Destination {
    FIREBASE, MIXPANEL, NEW_RELIC, SUPABASE, LOCAL_DB, CLOUD_STORAGE
}

data class BatchResult(
    val totalEvents: Int,
    val successfulEvents: Int,
    val failedEvents: Int,
    val failures: List<EventFailure> = emptyList(),
    val processingTimeMs: Long
)

data class EventFailure(
    val event: TelemetryEvent,
    val reason: String,
    val message: String,
    val retryable: Boolean
)

data class SendResult(
    val accepted: Boolean,
    val eventId: String,
    val latencyMs: Long,
    val metadata: Map<String, Any> = emptyMap()
)

data class BatchSendResult(
    val accepted: Boolean,
    val successfulIds: List<String>,
    val failedEvents: List<RejectedEvent>,
    val latencyMs: Long
)

data class RejectedEvent(
    val eventId: String,
    val reason: String,
    val message: String? = null
)

data class FlushResult(
    val eventsProcessed: Int,
    val success: Boolean,
    val error: String? = null
)

@OptIn(ExperimentalTime::class)
data class HealthStatus(
    val isHealthy: Boolean,
    val lastCheck: Instant,
    val details: Map<String, String> = emptyMap()
)

@OptIn(ExperimentalTime::class)
data class DestinationMetrics(
    val totalSent: Long,
    val totalFailed: Long,
    val queueSize: Int,
    val lastSendTime: Instant? = null,
    val avgLatencyMs: Double = 0.0
)

@OptIn(ExperimentalTime::class)
data class RetryableEvent(
    val event: TelemetryEvent,
    val destination: Destination,
    val retryAfter: Instant,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val firstAttempt: Instant = retryAfter,
    val lastError: String? = null
)

@OptIn(ExperimentalTime::class)
data class CachedEvent(
    val event: TelemetryEvent,
    val cachedAt: Instant
)

data class CacheConfig(
    val maxMemoryEvents: Int = 1000,
    val evictionBatchSize: Int = 100
)

// Destination Configs
sealed interface DestinationConfig {
    val enabled: Boolean
}

data class FirebaseConfig(
    override val enabled: Boolean = true,
    val appId: String = "",
    val projectId: String,
    val apiKey: String,
    val analyticsCollectionEnabled: Boolean = true
) : DestinationConfig

data class NewRelicConfig(
    override val enabled: Boolean = true,
    val accountId: String,
    val apiKey: String,
    val region: String = "US",
    val timeout: Duration = 10.seconds,
    val customAttributes: Map<String, String> = emptyMap()
) : DestinationConfig

data class SupabaseConfig(
    override val enabled: Boolean = true,
    val url: String,
    val key: String,
    val table: String = "events"
) : DestinationConfig

data class LocalDatabaseConfig(
    override val enabled: Boolean = true,
    val databaseName: String = "observability.db"
) : DestinationConfig

data class CloudStorageConfig(
    override val enabled: Boolean = true,
    val bucketName: String
) : DestinationConfig
