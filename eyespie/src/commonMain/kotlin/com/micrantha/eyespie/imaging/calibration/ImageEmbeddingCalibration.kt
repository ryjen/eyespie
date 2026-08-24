package com.micrantha.eyespie.imaging.calibration

import com.micrantha.eyespie.core.MatchEngine
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_CONTRACT_VERSION
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal const val IMAGE_EMBEDDING_CALIBRATION_REPORT_SCHEMA_VERSION = 2
internal const val IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT = 5

internal data class ImageEmbeddingCalibrationApplication(
    val version: String,
    val build: Int,
) {
    init {
        require(version.isNotBlank()) { "calibration application version must not be blank" }
        require(build > 0) { "calibration application build must be positive" }
    }
}

internal data class ImageEmbeddingCalibrationDevice(
    val manufacturer: String,
    val model: String,
    val os: String,
) {
    init {
        require(manufacturer.isNotBlank()) { "calibration device manufacturer must not be blank" }
        require(model.isNotBlank()) { "calibration device model must not be blank" }
        require(os.isNotBlank()) { "calibration device OS must not be blank" }
    }
}

internal data class ImageEmbeddingCalibrationRuntime(
    val name: String,
    val version: String,
) {
    init {
        require(name.isNotBlank()) { "calibration runtime name must not be blank" }
        require(version.isNotBlank()) { "calibration runtime version must not be blank" }
    }
}

internal data class ImageEmbeddingCalibrationModel(
    val sha256: String,
    val id: String = IMAGE_EMBEDDER_MODEL_ID,
) {
    init {
        require(id == IMAGE_EMBEDDER_MODEL_ID) { "calibration model id must use the active embedding contract" }
        require(sha256.length == 64 && sha256.all { it in '0'..'9' || it in 'a'..'f' }) {
            "calibration model SHA-256 must be 64 lowercase hex characters"
        }
    }
}

internal data class ImageEmbeddingCalibrationMatchPolicy(
    val cosineThreshold: Double,
) {
    init {
        require(cosineThreshold.isFinite() && cosineThreshold in -1.0..1.0) {
            "calibration cosine threshold must be finite and within [-1, 1]"
        }
    }
}

internal data class ImageEmbeddingCalibrationFixture(
    val id: String,
    val role: String,
    val sourceSha256: String,
    val embedding: List<Float>,
    val repeatCount: Int,
    val repeatCosineMin: Double,
    val repeatMaxAbsDelta: Double,
) {
    init {
        require(id.isNotBlank()) { "calibration fixture id must not be blank" }
        require(role.isNotBlank()) { "calibration fixture role must not be blank" }
        require(sourceSha256.length == 64 && sourceSha256.all { it in '0'..'9' || it in 'a'..'f' }) {
            "calibration fixture SHA-256 must be 64 lowercase hex characters"
        }
        canonicalImageEmbedding(embedding)
        require(repeatCount == IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) {
            "calibration fixture must contain exactly $IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT runs"
        }
        require(repeatCosineMin.isFinite() && repeatCosineMin in -1.0..1.0) {
            "calibration repeat cosine must be finite and within [-1, 1]"
        }
        require(repeatMaxAbsDelta.isFinite() && repeatMaxAbsDelta >= 0.0) {
            "calibration repeat delta must be finite and non-negative"
        }
    }
}

internal data class ImageEmbeddingCalibrationReport(
    val platform: String,
    val application: ImageEmbeddingCalibrationApplication,
    val device: ImageEmbeddingCalibrationDevice,
    val runtime: ImageEmbeddingCalibrationRuntime,
    val model: ImageEmbeddingCalibrationModel,
    val matchPolicy: ImageEmbeddingCalibrationMatchPolicy,
    val fixtures: List<ImageEmbeddingCalibrationFixture>,
) {
    init {
        require(platform == "android" || platform == "ios") {
            "calibration platform must be android or ios"
        }
        require(fixtures.isNotEmpty()) { "calibration report must contain fixtures" }
        require(fixtures.map { it.id }.distinct().size == fixtures.size) {
            "calibration report fixture ids must be unique"
        }
    }
}

internal fun summarizeImageEmbeddingCalibrationFixture(
    id: String,
    role: String,
    sourceSha256: String,
    runs: List<List<Float>>,
): ImageEmbeddingCalibrationFixture {
    require(runs.size == IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) {
        "calibration requires exactly $IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT inference runs"
    }
    val canonical = runs.map(::canonicalImageEmbedding)
    val first = canonical.first()
    val matchEngine = MatchEngine()

    var minimumCosine = 1.0
    var maximumAbsoluteDelta = 0.0
    canonical.drop(1).forEach { repeat ->
        minimumCosine = minOf(minimumCosine, matchEngine.compare(first, repeat).similarity)
        first.indices.forEach { index ->
            maximumAbsoluteDelta = maxOf(
                maximumAbsoluteDelta,
                abs(first[index].toDouble() - repeat[index].toDouble()),
            )
        }
    }

    return ImageEmbeddingCalibrationFixture(
        id = id,
        role = role,
        sourceSha256 = sourceSha256,
        embedding = first,
        repeatCount = canonical.size,
        repeatCosineMin = minimumCosine,
        repeatMaxAbsDelta = maximumAbsoluteDelta,
    )
}

internal fun ImageEmbeddingCalibrationReport.toCalibrationJson(): String {
    val payload = buildJsonObject {
        put("report_schema_version", IMAGE_EMBEDDING_CALIBRATION_REPORT_SCHEMA_VERSION)
        put("platform", platform)
        putJsonObject("application") {
            put("version", application.version)
            put("build", application.build)
        }
        putJsonObject("device") {
            put("manufacturer", device.manufacturer)
            put("model", device.model)
            put("os", device.os)
        }
        putJsonObject("runtime") {
            put("name", runtime.name)
            put("version", runtime.version)
        }
        putJsonObject("model") {
            put("id", model.id)
            put("sha256", model.sha256)
        }
        putJsonObject("match_policy") {
            put("cosine_threshold", matchPolicy.cosineThreshold)
        }
        putJsonObject("embedding_contract") {
            put("schema_version", IMAGE_EMBEDDING_CONTRACT_VERSION)
            put("dimensions", IMAGE_EMBEDDING_DIMENSIONS)
        }
        putJsonArray("fixtures") {
            fixtures.forEach { fixture ->
                add(
                    buildJsonObject {
                        put("id", fixture.id)
                        put("role", fixture.role)
                        put("source_sha256", fixture.sourceSha256)
                        put(
                            "embedding",
                            buildJsonArray {
                                fixture.embedding.forEach { value -> add(JsonPrimitive(value)) }
                            },
                        )
                        put("repeat_count", fixture.repeatCount)
                        put("repeat_cosine_min", fixture.repeatCosineMin)
                        put("repeat_max_abs_delta", fixture.repeatMaxAbsDelta)
                    },
                )
            }
        }
    }
    return CALIBRATION_JSON.encodeToString(JsonElement.serializer(), payload) + "\n"
}

private val CALIBRATION_JSON = Json {
    prettyPrint = true
}
