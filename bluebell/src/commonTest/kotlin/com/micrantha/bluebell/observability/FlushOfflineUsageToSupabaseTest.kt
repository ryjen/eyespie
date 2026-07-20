package com.micrantha.bluebell.observability

import com.micrantha.bluebell.observability.domain.SupabaseInsertClient
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.DestinationContext
import com.micrantha.bluebell.observability.repository.OkioJsonLinesDiskCache
import com.micrantha.bluebell.observability.usecase.FlushOfflineUsageToSupabase
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class FlushOfflineUsageToSupabaseTest {

    @Test
    fun flushesAndDeletesOnlyUploadedAnalytics() = runTest {
        val fs = FileSystem.SYSTEM
        val path = "./build/test-offline-usage.jsonl".toPath()
        if (fs.exists(path)) fs.delete(path)

        val cache = OkioJsonLinesDiskCache(fs, path)

        cache.store(AnalyticsEvent.FeatureUsage(properties = mapOf("action" to "open")))
            .getOrThrow()
        cache.store(AnalyticsEvent.FeatureUsage(properties = mapOf("action" to "close")))
            .getOrThrow()

        var inserted = 0
        val fakeSupabase = SupabaseInsertClient { _, rows ->
            inserted += rows.size
        }

        val usecase = FlushOfflineUsageToSupabase(cache, fakeSupabase)
        val result = usecase.flushOnce(
            batchSize = 100,
            context = DestinationContext(timestamp = Clock.System.now()),
        ).getOrThrow()

        assertEquals(2, result)
        assertEquals(2, inserted)
        assertEquals(0, cache.count())

        if (fs.exists(path)) fs.delete(path)
    }
}
