package com.micrantha.bluebell.observability

import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.repository.OkioJsonLinesDiskCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

class OkioJsonLinesDiskCacheTest {

    @Test
    fun writesAndReadsEvents() = runTest {
        val fs = FileSystem.SYSTEM
        val path = "./build/test-observability-events.jsonl".toPath()

        // clean
        if (fs.exists(path)) fs.delete(path)

        val cache = OkioJsonLinesDiskCache(fs, path)

        cache.write(AnalyticsEvent.FeatureUsage(properties = mapOf("action" to "open"))).getOrThrow()
        cache.write(AnalyticsEvent.FeatureUsage(properties = mapOf("action" to "close"))).getOrThrow()

        val read = cache.readOldest(10)
        assertEquals(2, read.size)

        val filtered = cache.query(EventFilter(properties = mapOf("action" to "open")), 10)
        assertEquals(1, filtered.size)

        // cleanup
        if (fs.exists(path)) fs.delete(path)
    }
}
