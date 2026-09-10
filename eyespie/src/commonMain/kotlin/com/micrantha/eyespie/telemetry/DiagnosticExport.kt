package com.micrantha.eyespie.telemetry

import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_SHA256
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_CONTRACT_VERSION
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.persistence.EYESPIE_DATABASE_SCHEMA_VERSION
import com.micrantha.eyespie.sharing.GAME_BUNDLE_SCHEMA_VERSION
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val DIAGNOSTIC_JSON = Json {
    prettyPrint = true
}

private val SOURCE_REVISION_REGEX = Regex("[0-9a-f]{40,64}")
private val VERSION_REGEX = Regex("[0-9]+[.][0-9]+[.][0-9]+(?:-[0-9A-Za-z.-]+)?")
private val RUNTIME_VERSION_REGEX = Regex("[0-9A-Za-z][0-9A-Za-z._-]{0,63}")

enum class DiagnosticPlatform {
    ANDROID,
    IOS,
}

data class DiagnosticReleaseIdentity(
    val appVersion: String,
    val appBuild: Int,
    val sourceRevision: String? = null,
    val databaseSchemaVersion: Int = EYESPIE_DATABASE_SCHEMA_VERSION,
    val bundleSchemaVersion: Int = GAME_BUNDLE_SCHEMA_VERSION,
) {
    init {
        require(VERSION_REGEX.matches(appVersion)) { "diagnostic app version is invalid" }
        require(appBuild > 0) { "diagnostic app build must be positive" }
        require(sourceRevision == null || SOURCE_REVISION_REGEX.matches(sourceRevision)) {
            "diagnostic source revision must be a canonical git object id when present"
        }
        require(databaseSchemaVersion > 0) { "diagnostic database schema must be positive" }
        require(bundleSchemaVersion > 0) { "diagnostic bundle schema must be positive" }
    }
}

data class DiagnosticRuntimeIdentity(
    val platform: DiagnosticPlatform,
    val osVersion: String,
    val mediaPipeVersion: String,
    val embeddingContractVersion: Int = IMAGE_EMBEDDING_CONTRACT_VERSION,
    val embeddingDimensions: Int = IMAGE_EMBEDDING_DIMENSIONS,
    val imageEmbedderModelId: String = IMAGE_EMBEDDER_MODEL_ID,
    val imageEmbedderModelSha256: String = IMAGE_EMBEDDER_MODEL_SHA256,
) {
    init {
        require(osVersion.isNotBlank() && osVersion.length <= 64) {
            "diagnostic OS version is invalid"
        }
        require(RUNTIME_VERSION_REGEX.matches(mediaPipeVersion)) {
            "diagnostic MediaPipe version is invalid"
        }
        require(embeddingContractVersion > 0) { "diagnostic embedding contract must be positive" }
        require(embeddingDimensions > 0) { "diagnostic embedding dimensions must be positive" }
        require(imageEmbedderModelId.isNotBlank() && imageEmbedderModelId.length <= 160) {
            "diagnostic model id is invalid"
        }
        require(Regex("[0-9a-f]{64}").matches(imageEmbedderModelSha256)) {
            "diagnostic model SHA-256 is invalid"
        }
    }
}

data class DiagnosticIdentity(
    val release: DiagnosticReleaseIdentity,
    val runtime: DiagnosticRuntimeIdentity,
)

fun interface DiagnosticIdentityProvider {
    fun current(): DiagnosticIdentity
}

data class DiagnosticExportEnvelope(
    val schemaVersion: Int = SCHEMA_VERSION,
    val release: DiagnosticReleaseIdentity,
    val runtime: DiagnosticRuntimeIdentity,
    val evictedRecords: Long,
    val droppedRecords: Long,
    val snapshotIncomplete: Boolean,
    val records: List<DiagnosticRecord>,
) {
    init {
        require(schemaVersion == SCHEMA_VERSION) { "unsupported diagnostic export schema" }
        require(evictedRecords >= 0) { "diagnostic eviction count must be non-negative" }
        require(droppedRecords >= 0) { "diagnostic drop count must be non-negative" }
        require(records.size <= MAX_RECORDS) { "diagnostic export exceeds record bound" }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 3
        const val MAX_RECORDS: Int = BoundedDiagnosticSink.DEFAULT_CAPACITY
        const val MAX_BYTES: Int = 128 * 1024
    }
}

/**
 * Builds an explicit, bounded, user-exportable diagnostics artifact.
 *
 * This class performs no I/O and has no transport. Platform share/files UI can
 * consume [encodeJson] later without changing the telemetry contract.
 */
class DiagnosticExportService(
    private val history: DiagnosticHistory,
    private val identityProvider: DiagnosticIdentityProvider,
) {
    fun snapshot(): DiagnosticExportEnvelope {
        val identity = identityProvider.current()
        val snapshot = history.snapshot()
        return DiagnosticExportEnvelope(
            release = identity.release,
            runtime = identity.runtime,
            evictedRecords = snapshot.evictedRecords,
            droppedRecords = snapshot.droppedRecords,
            snapshotIncomplete = snapshot.incomplete,
            records = snapshot.records,
        )
    }

    fun encodeJson(): ByteArray {
        val encoded = DIAGNOSTIC_JSON
            .encodeToString(JsonElement.serializer(), snapshot().toJson())
            .plus('\n')
            .encodeToByteArray()
        require(encoded.size <= DiagnosticExportEnvelope.MAX_BYTES) {
            "diagnostic export exceeds byte bound"
        }
        return encoded
    }
}

private fun DiagnosticExportEnvelope.toJson(): JsonElement = buildJsonObject {
    put("schema_version", schemaVersion)
    put("repository", "ryjen/eyespie")
    put("release", buildJsonObject {
        put("app_version", release.appVersion)
        put("app_build", release.appBuild)
        release.sourceRevision?.let { put("source_revision", it) }
        put("database_schema_version", release.databaseSchemaVersion)
        put("bundle_schema_version", release.bundleSchemaVersion)
    })
    put("runtime", buildJsonObject {
        put("platform", runtime.platform.name.lowercase())
        put("os_version", runtime.osVersion)
        put("mediapipe_version", runtime.mediaPipeVersion)
        put("embedding_contract_version", runtime.embeddingContractVersion)
        put("embedding_dimensions", runtime.embeddingDimensions)
        put("model_id", runtime.imageEmbedderModelId)
        put("model_sha256", runtime.imageEmbedderModelSha256)
    })
    put("evicted_records", evictedRecords)
    put("dropped_records", droppedRecords)
    put("snapshot_incomplete", snapshotIncomplete)
    put("records", buildJsonArray {
        records.forEach { record ->
            add(buildJsonObject {
                put("schema_version", record.schemaVersion)
                put("operation", record.operation.name.lowercase())
                put("result", record.result.name.lowercase())
                record.code?.let { put("code", it.name.lowercase()) }
                put("duration_ms", record.durationMillis)
                put("correlation", buildJsonObject {
                    put("trace_id", record.correlation.traceId)
                    put("span_id", record.correlation.spanId)
                    record.correlation.parentSpanId?.let { put("parent_span_id", it) }
                })
            })
        }
    })
}
