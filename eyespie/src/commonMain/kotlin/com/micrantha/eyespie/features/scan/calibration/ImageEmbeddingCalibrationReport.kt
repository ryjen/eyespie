package com.micrantha.eyespie.features.scan.calibration

import com.micrantha.eyespie.domain.entities.Embedding
import com.micrantha.eyespie.domain.entities.ImageEmbeddingContract
import com.micrantha.eyespie.domain.entities.cosineSimilarity
import com.micrantha.eyespie.domain.entities.floats
import com.micrantha.eyespie.domain.entities.requireCanonical
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

internal const val IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT = 5

@Serializable
internal data class ImageEmbeddingCalibrationDevice(
    val manufacturer: String,
    val model: String,
    val os: String,
)

@Serializable
internal data class ImageEmbeddingCalibrationRuntime(
    val name: String,
    val version: String,
)

@Serializable
internal data class ImageEmbeddingCalibrationModel(
    val sha256: String,
    val id: String = ImageEmbeddingContract.logicalModelId,
)

@Serializable
internal data class ImageEmbeddingCalibrationContract(
    val schema_version: Int = ImageEmbeddingContract.schemaVersion,
    val dimensions: Int = ImageEmbeddingContract.dimensions,
)

@Serializable
internal data class ImageEmbeddingCalibrationMatchPolicy(
    val cosine_threshold: Float,
)

@Serializable
internal data class ImageEmbeddingCalibrationFixture(
    val id: String,
    val role: String,
    val source_sha256: String,
    val embedding: List<Float>,
    val repeat_count: Int,
    val repeat_cosine_min: Float,
    val repeat_max_abs_delta: Float,
)

@Serializable
internal data class ImageEmbeddingCalibrationReport(
    val report_schema_version: Int = 1,
    val platform: String,
    val device: ImageEmbeddingCalibrationDevice,
    val runtime: ImageEmbeddingCalibrationRuntime,
    val model: ImageEmbeddingCalibrationModel,
    val match_policy: ImageEmbeddingCalibrationMatchPolicy,
    val embedding_contract: ImageEmbeddingCalibrationContract = ImageEmbeddingCalibrationContract(),
    val fixtures: List<ImageEmbeddingCalibrationFixture>,
)

internal fun summarizeImageEmbeddingCalibrationFixture(
    id: String,
    role: String,
    sourceSha256: String,
    runs: List<Embedding>,
): ImageEmbeddingCalibrationFixture {
    require(sourceSha256.length == 64 && sourceSha256.all { it in '0'..'9' || it in 'a'..'f' }) {
        "calibration fixture SHA-256 must be 64 lowercase hex characters"
    }
    require(runs.size >= 2) { "calibration requires at least two inference runs" }
    val canonical = runs.map { it.requireCanonical() }
    val first = canonical.first()
    val firstValues = first.floats()

    var minimumCosine = 1f
    var maximumAbsoluteDelta = 0f
    canonical.drop(1).forEach { repeat ->
        minimumCosine = minOf(minimumCosine, first.cosineSimilarity(repeat))
        val values = repeat.floats()
        for (index in firstValues.indices) {
            maximumAbsoluteDelta = maxOf(
                maximumAbsoluteDelta,
                abs(firstValues[index] - values[index]),
            )
        }
    }

    return ImageEmbeddingCalibrationFixture(
        id = id,
        role = role,
        source_sha256 = sourceSha256,
        embedding = firstValues,
        repeat_count = canonical.size,
        repeat_cosine_min = minimumCosine,
        repeat_max_abs_delta = maximumAbsoluteDelta,
    )
}

internal fun ImageEmbeddingCalibrationReport.toCalibrationJson(): String =
    Json {
        prettyPrint = true
        encodeDefaults = true
    }.encodeToString(this)
