package com.micrantha.bluebell.observability.usecase

import com.micrantha.bluebell.observability.domain.SupabaseInsertClient
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.repository.OkioJsonLinesDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime


/**
 * Drain offline-captured [AnalyticsEvent]s from an [OkioJsonLinesDiskCache] into Supabase.
 */
class FlushOfflineUsageToSupabase(
    private val diskCache: OkioJsonLinesDiskCache,
    private val supabase: SupabaseInsertClient,
    private val table: String = "usage_events",
) {

    @OptIn(ExperimentalTime::class)
    suspend fun flushOnce(
        batchSize: Int = 100,
        context: DestinationContext,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(batchSize > 0) { "batchSize must be > 0" }

            // Read a raw batch (may contain other event types)
            val batch = diskCache.retrieve(batchSize)
            if (batch.isEmpty()) return@runCatching 0

            val analytics = batch.filterIsInstance<AnalyticsEvent>()
            if (analytics.isEmpty()) {
                // Nothing we can upload with this use case. Don’t delete anything.
                return@runCatching 0
            }

            val rows = analytics.map { event ->
                // Keep it very simple: align with the migration schema.
                mapOf(
                    "event_id" to event.eventId,
                    "schema_name" to event.schema.name,
                    "schema_version" to event.schema.version,
                    "event_type" to (event::class.simpleName ?: "AnalyticsEvent"),
                    "user_id" to (context.userId ?: event.userId),
                    "session_id" to (context.sessionId ?: event.sessionId),
                    "timestamp_ms" to event.timestamp.toEpochMilliseconds(),
                    "properties_json" to event.properties.mapValues { it.value.toString() },
                )
            }

            supabase.insert(table = table, rows = rows)

            // Only delete what we successfully inserted.
            diskCache.delete(analytics.map { it.eventId }).getOrThrow()

            analytics.size
        }
    }
}

