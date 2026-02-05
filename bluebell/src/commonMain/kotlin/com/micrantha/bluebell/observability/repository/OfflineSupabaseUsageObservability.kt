package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.domain.UsageEvent
import com.micrantha.bluebell.observability.domain.UsageObservability
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.usecase.FlushOfflineUsageToSupabase
import kotlin.time.ExperimentalTime

/**
 * Implementation of [UsageObservability] that:
 * - writes usage events to an offline disk cache
 * - flushes them to Supabase when requested
 */
class OfflineSupabaseUsageObservability(
    private val diskCache: OkioJsonLinesDiskCache,
    private val flushToSupabase: FlushOfflineUsageToSupabase,
    private val contextProvider: DestinationContextProvider,
) : UsageObservability {

    override suspend fun track(event: UsageEvent): Result<Unit> {
        val telemetry = AnalyticsEvent.FeatureUsage(
            properties = buildMap {
                put("name", event.name)
                putAll(event.properties)
            }
        )
        return diskCache.write(telemetry)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun flush(): Result<Int> {
        return flushToSupabase.flushOnce(
            batchSize = 200,
            context = contextProvider.get(),
        )
    }
}

fun interface DestinationContextProvider {
    fun get(): DestinationContext
}

object DefaultDestinationContextProvider {
    @OptIn(ExperimentalTime::class)
    fun create(): DestinationContextProvider = DestinationContextProvider { DestinationContext() }
}
