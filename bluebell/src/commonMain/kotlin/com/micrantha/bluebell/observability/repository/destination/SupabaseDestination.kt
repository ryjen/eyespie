package com.micrantha.bluebell.observability.repository.destination

import com.micrantha.bluebell.observability.domain.DestinationRejectionException
import com.micrantha.bluebell.observability.domain.DestinationUnavailableException
import com.micrantha.bluebell.observability.domain.EventDestination
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.BatchSendResult
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.DestinationConfig
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.entity.DestinationMetrics
import com.micrantha.bluebell.observability.entity.FlushResult
import com.micrantha.bluebell.observability.entity.HealthStatus
import com.micrantha.bluebell.observability.entity.RejectedEvent
import com.micrantha.bluebell.observability.entity.RejectionReason
import com.micrantha.bluebell.observability.entity.SendResult
import com.micrantha.bluebell.observability.entity.SupabaseConfig
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Sends analytics/usage events to Supabase via a thin insert function.
 *
 * The app layer passes an implementation of [SupabaseInsertClient] so this stays KMP-friendly
 * and avoids depending on Supabase-KT inside :bluebell.
 */
class SupabaseDestination(
    private val client: SupabaseInsertClient,
    private var config: SupabaseConfig = SupabaseConfig(),
    private val json: Json = Json { encodeDefaults = true },
) : EventDestination {

    override val destination: Destination = Destination.CLOUD_STORAGE
    override var isEnabled: Boolean = config.enabled

    private val metrics = AtomicDestinationMetrics()

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
            if (event !is AnalyticsEvent) {
                throw DestinationRejectionException(
                    destination = destination,
                    eventId = event.eventId,
                    reason = "Supabase destination only accepts AnalyticsEvent"
                )
            }

            client.insert(
                table = config.table,
                rows = listOf(event.toRow(context, json))
            )

            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            metrics.recordSuccess(latency)

            SendResult(
                eventId = event.eventId,
                accepted = true,
                destination = destination,
                latencyMs = latency
            )
        }.onFailure { err ->
            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            metrics.recordFailure(latency)
            // Preserve the exception for upstream handling.
            throw err
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

        val (analytics, rejected) = events.partition { it is AnalyticsEvent }
        val rejectedEvents = rejected.map {
            RejectedEvent(
                eventId = it.eventId,
                reason = RejectionReason.Other("unsupported_type"),
                message = "Supabase destination only accepts AnalyticsEvent"
            )
        }

        runCatching {
            if (analytics.isNotEmpty()) {
                val rows = analytics.map { (it as AnalyticsEvent).toRow(context, json) }
                client.insert(table = config.table, rows = rows)
            }

            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            metrics.recordBatchSuccess(analytics.size, latency)

            BatchSendResult(
                totalEvents = events.size,
                acceptedEvents = analytics.size,
                rejectedEvents = rejectedEvents,
                latencyMs = latency
            )
        }.onFailure { err ->
            val latency = Clock.System.now().toEpochMilliseconds() - startTime
            metrics.recordFailure(latency)
            throw err
        }
    }

    override suspend fun flush(): Result<FlushResult> {
        // Supabase PostgREST doesn't require a flush.
        return Result.success(FlushResult(flushedCount = 0, failedCount = 0, durationMs = 0))
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun healthCheck(): HealthStatus {
        // Keep it cheap: assume healthy if enabled.
        return HealthStatus(
            isHealthy = isEnabled,
            status = if (isEnabled) HealthStatus.Status.HEALTHY else HealthStatus.Status.DEGRADED,
            lastSuccessfulSend = Clock.System.now(),
            lastError = null,
            consecutiveFailures = 0,
            details = mapOf("table" to config.table)
        )
    }

    override fun getMetrics(): DestinationMetrics = metrics.toMetrics()

    override suspend fun configure(config: DestinationConfig): Result<Unit> = runCatching {
        require(config is SupabaseConfig) { "Invalid config type" }
        this.config = config
        this.isEnabled = config.enabled
    }

    override suspend fun enable() {
        isEnabled = true
    }

    override suspend fun disable() {
        isEnabled = false
    }
}

/**
 * Minimal interface so :bluebell doesn’t need to depend on Supabase-KT.
 *
 * App layer can implement this using Supabase-KT PostgREST insert into the configured table.
 */
fun interface SupabaseInsertClient {
    suspend fun insert(table: String, rows: List<Map<String, Any?>>)
}

@Serializable
private data class UsageEventRow(
    val event_id: String,
    val schema_name: String,
    val schema_version: Int,
    val event_type: String,
    val user_id: String? = null,
    val session_id: String? = null,
    val timestamp_ms: Long,
    val properties_json: String,
)

@OptIn(ExperimentalTime::class)
private fun AnalyticsEvent.toRow(
    context: DestinationContext,
    json: Json,
): Map<String, Any?> {
    val propsJson = json.encodeToString(properties.mapValues { it.value.toString() })

    val row = UsageEventRow(
        event_id = eventId,
        schema_name = schema.name,
        schema_version = schema.version,
        event_type = this::class.simpleName ?: "AnalyticsEvent",
        user_id = context.userId ?: userId,
        session_id = context.sessionId ?: sessionId,
        timestamp_ms = timestamp.toEpochMilliseconds(),
        properties_json = propsJson,
    )

    // Map to keys that PostgREST likes.
    return mapOf(
        "event_id" to row.event_id,
        "schema_name" to row.schema_name,
        "schema_version" to row.schema_version,
        "event_type" to row.event_type,
        "user_id" to row.user_id,
        "session_id" to row.session_id,
        "timestamp_ms" to row.timestamp_ms,
        "properties_json" to row.properties_json,
    )
}
