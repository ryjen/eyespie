package com.micrantha.bluebell.observability

import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.repository.OkioJsonLinesDiskCache
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class OkioJsonLinesDiskCacheTest {

    @Test
    fun writesAndReadsEvents() = runTest {
        val fs = FileSystem.SYSTEM
        val path = "./build/test-observability-events-${Random.nextInt()}.jsonl".toPath()

        try {
            val cache = OkioJsonLinesDiskCache(fs, path)

            cache.store(AnalyticsEvent.FeatureUsage(properties = mapOf("action" to "open")))
                .getOrThrow()
            cache.store(AnalyticsEvent.FeatureUsage(properties = mapOf("action" to "close")))
                .getOrThrow()

            val read = cache.retrieve(10)
            assertEquals(2, read.size)

            val filtered = cache.retrieveByFilter(
                EventFilter(properties = mapOf("action" to "open")),
                10,
            )
            assertEquals(1, filtered.size)
        } finally {
            if (fs.exists(path)) fs.delete(path)
        }
    }
}
