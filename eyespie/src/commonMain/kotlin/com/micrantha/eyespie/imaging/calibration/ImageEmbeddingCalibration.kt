package com.micrantha.eyespie.imaging.calibration

import com.micrantha.eyespie.core.MatchEngine
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_CONTRACT_VERSION
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import com.micrantha.eyespie.imaging.canonicalImageEmbedding
import kotlin.math.abs

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

internal fun ImageEmbeddingCalibrationReport.toCalibrationJson(): String = buildString {
    append("{\n")
    append("  \"report_schema_version\": ")
    append(IMAGE_EMBEDDING_CALIBRATION_REPORT_SCHEMA_VERSION)
    append(",\n")
    append("  \"platform\": ")
    appendJsonString(platform)
    append(",\n")
    append("  \"application\": {\"version\": ")
    appendJsonString(application.version)
    append(", \"build\": ")
    append(application.build)
    append("},\n")
    append("  \"device\": {\"manufacturer\": ")
    appendJsonString(device.manufacturer)
    append(", \"model\": ")
    appendJsonString(device.model)
    append(", \"os\": ")
    appendJsonString(device.os)
    append("},\n")
    append("  \"runtime\": {\"name\": ")
    appendJsonString(runtime.name)
    append(", \"version\": ")
    appendJsonString(runtime.version)
    append("},\n")
    append("  \"model\": {\"id\": ")
    appendJsonString(model.id)
    append(", \"sha256\": ")
    appendJsonString(model.sha256)
    append("},\n")
    append("  \"match_policy\": {\"cosine_threshold\": ")
    append(matchPolicy.cosineThreshold)
    append("},\n")
    append("  \"embedding_contract\": {\"schema_version\": ")
    append(IMAGE_EMBEDDING_CONTRACT_VERSION)
    append(", \"dimensions\": ")
    append(IMAGE_EMEDDING_DIMENSIONS_COMPAT)
    append("},\n")
    append("  \"fixtures\": [\n")
    fixtures.forEachIndexed { fixtureIndex, fixture ->
        append("    {\"id\": ")
        appendJsonString(fixture.id)
        append(", \"role\": ")
        appendJsonString(fixture.role)
        append(", \"source_sha256\": ")
        appendJsonString(fixture.sourceSha256)
        append(", \"embedding\": [")
        fixture.embedding.forEachIndexed { index, value ->
            if (index > 0) append(',')
            append(value)
        }
        append("], \"repeat_count\": ")
        append(fixture.repeatCount)
        append(", \"repeat_cosine_min\": ")
        append(fixture.repeatCosineMin)
        append(", \"repeat_max_abs_delta\": ")
        append(fixture.repeatMaxAbsDelta)
        append('}')
        if (fixtureIndex < fixtures.lastIndex) append(',')
        append('\n')
    }
    append("  ]\n")
    append("}\n")
}

// Alias kept local to this evidence encoder so the emitted field name cannot drift from the
// application-owned embedding contract while retaining a readable JSON builder above.
private const val IMAGE_EMEDDING_DIMENSIONS_COMPAT = IMAGE_EMBEDDING_DIMENSIONS

private fun StringBuilder.appendJsonString(value: String) {
    append('"')
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (character.code < 0x20) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
    }
    append('"')
}
