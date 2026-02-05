package com.micrantha.bluebell.observability.repository

import com.micrantha.bluebell.observability.domain.DiskCache
import com.micrantha.bluebell.observability.entity.AnalyticsEvent
import com.micrantha.bluebell.observability.entity.EventFilter
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A simple disk cache that persists events as JSON Lines (one event per line).
 *
 * Notes:
 * - This is optimized for reliability + simplicity, not for high throughput.
 * - It supports filtering by a subset of fields/properties.
 * - It’s safe to use from multiple coroutines via a mutex.
 */
class OkioJsonLinesDiskCache(
    private val fileSystem: FileSystem,
    private val filePath: Path,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) : DiskCache {

    private val lock = Mutex()

    /** Convenience wrapper around [delete] to match common naming. */
    suspend fun deleteByIds(eventIds: List<String>): Result<Unit> = delete(eventIds)

    override suspend fun write(event: TelemetryEvent): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            lock.withLock {
                ensureParentDirExists()
                val sink: BufferedSink = fileSystem.appendingSink(filePath, mustExist = false).buffer()
                sink.use {
                    it.writeUtf8(json.encodeToString(event.toRecord()))
                    it.writeUtf8("\n")
                }
            }
        }
    }

    override suspend fun writeBatch(events: List<TelemetryEvent>): Result<Unit> = runCatching {
        if (events.isEmpty()) return@runCatching Unit
        withContext(Dispatchers.IO) {
            lock.withLock {
                ensureParentDirExists()
                val sink: BufferedSink = fileSystem.appendingSink(filePath, mustExist = false).buffer()
                sink.use {
                    events.forEach { event ->
                        it.writeUtf8(json.encodeToString(event.toRecord()))
                        it.writeUtf8("\n")
                    }
                }
            }
        }
    }

    override suspend fun readOldest(limit: Int): List<TelemetryEvent> = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext emptyList()
        lock.withLock {
            readAllLocked().take(limit)
        }
    }

    override suspend fun query(filter: EventFilter, limit: Int): List<TelemetryEvent> = withContext(Dispatchers.IO) {
        if (limit <= 0) return@withContext emptyList()
        lock.withLock {
            readAllLocked().asSequence()
                .filter { it.matchesFilter(filter) }
                .take(limit)
                .toList()
        }
    }

    override suspend fun delete(eventIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (eventIds.isEmpty()) return@runCatching
            lock.withLock {
                val remaining = readAllLocked().filterNot { it.eventId in eventIds }
                rewriteLocked(remaining)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun deleteOlderThan(timestamp: Instant): Int = withContext(Dispatchers.IO) {
        lock.withLock {
            val events = readAllLocked()
            val (kept, removed) = events.partition { it.timestamp >= timestamp }
            if (removed.isNotEmpty()) {
                rewriteLocked(kept)
            }
            removed.size
        }
    }

    override suspend fun count(): Int = withContext(Dispatchers.IO) {
        lock.withLock { readAllLocked().size }
    }

    override suspend fun isEmpty(): Boolean = count() == 0

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    private fun ensureParentDirExists() {
        val parent = filePath.parent ?: return
        fileSystem.createDirectories(parent, mustCreate = false)
    }

    private fun readAllLocked(): List<TelemetryEvent> {
        if (!fileSystem.exists(filePath)) return emptyList()
        val source: BufferedSource = fileSystem.source(filePath).buffer()
        return source.use {
            val out = mutableListOf<TelemetryEvent>()
            while (true) {
                val line = it.readUtf8Line() ?: break
                if (line.isBlank()) continue
                val record = json.decodeFromString<TelemetryRecord>(line)
                out.add(record.toEvent())
            }
            out
        }
    }

    private fun rewriteLocked(events: List<TelemetryEvent>) {
        ensureParentDirExists()
        fileSystem.write(filePath) {
            events.forEach { event ->
                writeUtf8(json.encodeToString(event.toRecord()))
                writeUtf8("\n")
            }
        }
    }
}

private fun TelemetryEvent.matchesFilter(filter: EventFilter): Boolean {
    if (filter.eventTypes != null && this::class.simpleName !in filter.eventTypes) {
        return false
    }
    if (filter.userId != null && this.userId != filter.userId) {
        return false
    }
    filter.properties?.forEach { (key, value) ->
        if (this.properties[key] != value) {
            return false
        }
    }
    return true
}

@Serializable
private data class TelemetryRecord(
    val eventId: String,
    val timestampEpochMs: Long,
    val userId: String? = null,
    val sessionId: String? = null,
    val schemaName: String,
    val schemaVersion: Int,
    val kind: String,
    val properties: Map<String, String> = emptyMap(),
) {
    @OptIn(ExperimentalTime::class)
    fun toEvent(): TelemetryEvent {
        return when (kind) {
            AnalyticsEvent.FeatureUsage::class.simpleName -> AnalyticsEvent.FeatureUsage(
                eventId = eventId,
                properties = properties,
                schema = SchemaVersion(schemaName, schemaVersion),
            )

            AnalyticsEvent.UserFlow::class.simpleName -> AnalyticsEvent.UserFlow(
                eventId = eventId,
                schema = SchemaVersion(schemaName, schemaVersion),
            )

            AnalyticsEvent.BusinessMetric::class.simpleName -> AnalyticsEvent.BusinessMetric(
                eventId = eventId,
                schema = SchemaVersion(schemaName, schemaVersion),
            )

            else -> AnalyticsEvent.FeatureUsage(
                eventId = eventId,
                properties = properties,
                schema = SchemaVersion(schemaName, schemaVersion),
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun TelemetryEvent.toRecord(): TelemetryRecord {
    val stringProps = properties.mapValues { (_, v) -> v.toString() }
    return TelemetryRecord(
        eventId = eventId,
        timestampEpochMs = timestamp.toEpochMilliseconds(),
        userId = userId,
        sessionId = sessionId,
        schemaName = schema.name,
        schemaVersion = schema.version,
        kind = this::class.simpleName ?: "unknown",
        properties = stringProps,
    )
}
