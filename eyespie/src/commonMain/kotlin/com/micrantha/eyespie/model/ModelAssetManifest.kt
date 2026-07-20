package com.micrantha.eyespie.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ModelAssetManifest(
    val schemaVersion: Int,
    val modelId: String,
    val version: String,
    val filename: String,
    val sizeBytes: Long,
    val sha256: String,
    val runtime: ModelRuntimeCompatibility,
) {
    fun toDescriptor(): ModelAssetDescriptor = ModelAssetDescriptor(
        id = modelId,
        version = version,
        filename = filename,
        expectedBytes = sizeBytes,
        sha256 = sha256.lowercase(),
        runtime = runtime,
    )
}

sealed interface ManifestValidationResult {
    data class Valid(val descriptor: ModelAssetDescriptor) : ManifestValidationResult
    data class Invalid(val diagnosticCode: String) : ManifestValidationResult
}

class ModelAssetManifestParser(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) {
    fun parseAndValidate(
        content: String,
        expectedModelId: String? = null,
        supportedEngines: Set<String> = setOf("mediapipe", "tflite"),
        supportedModelAbi: Int = 1,
    ): ManifestValidationResult {
        val manifest = runCatching {
            json.decodeFromString<ModelAssetManifest>(content)
        }.getOrElse {
            return ManifestValidationResult.Invalid("manifest.invalid_json")
        }
        val normalizedSupportedEngines = supportedEngines.mapTo(mutableSetOf()) { it.lowercase() }
        val filename = manifest.filename.trim()

        val diagnosticCode = when {
            manifest.schemaVersion != 1 -> "manifest.unsupported_schema"
            manifest.modelId.isBlank() -> "manifest.missing_model_id"
            expectedModelId != null && manifest.modelId != expectedModelId -> "manifest.model_id_mismatch"
            manifest.version.isBlank() || manifest.version.equals("latest", ignoreCase = true) ->
                "manifest.mutable_version"
            filename.isEmpty() ||
                filename == "." ||
                filename == ".." ||
                filename.contains('/') ||
                filename.contains('\\') ||
                filename.contains('\u0000') -> "manifest.invalid_filename"
            manifest.sizeBytes <= 0L -> "manifest.invalid_size"
            !SHA_256.matches(manifest.sha256) -> "manifest.invalid_sha256"
            manifest.runtime.engine.lowercase() !in normalizedSupportedEngines -> "manifest.unsupported_engine"
            manifest.runtime.minimumRuntimeVersion.isBlank() -> "manifest.missing_runtime_version"
            manifest.runtime.minimumModelAbi <= 0 -> "manifest.invalid_model_abi"
            manifest.runtime.minimumModelAbi > supportedModelAbi -> "manifest.unsupported_model_abi"
            else -> null
        }

        return if (diagnosticCode == null) {
            ManifestValidationResult.Valid(manifest.copy(filename = filename).toDescriptor())
        } else {
            ManifestValidationResult.Invalid(diagnosticCode)
        }
    }

    private companion object {
        val SHA_256 = Regex("^[0-9a-fA-F]{64}$")
    }
}
