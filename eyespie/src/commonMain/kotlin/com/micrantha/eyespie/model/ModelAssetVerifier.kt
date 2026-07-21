package com.micrantha.eyespie.model

import okio.Buffer
import okio.FileSystem
import okio.HashingSource
import okio.Path
import okio.buffer

/** Runtime capabilities of the inference implementation bundled with the app. */
data class ModelRuntimeCapabilities(
    val engine: String,
    val version: String,
    val modelAbi: Int,
)

sealed interface ModelAssetVerificationResult {
    data class Verified(
        val descriptor: ModelAssetDescriptor,
        val localPath: String,
    ) : ModelAssetVerificationResult

    data class Invalid(
        val stage: FailureStage,
        val diagnosticCode: String,
    ) : ModelAssetVerificationResult
}

/**
 * Performs deterministic validation that is independent of Play Asset Delivery or Apple delivery APIs.
 * Runtime loading and bounded inference smoke testing remain a separate stage.
 */
class ModelAssetVerifier(
    private val fileSystem: FileSystem,
    private val manifestParser: ModelAssetManifestParser = ModelAssetManifestParser(),
) {
    fun verify(
        manifestPath: Path,
        modelPath: Path,
        expectedDescriptor: ModelAssetDescriptor,
        runtime: ModelRuntimeCapabilities,
    ): ModelAssetVerificationResult {
        val manifestContent = runCatching {
            fileSystem.source(manifestPath).buffer().use { it.readUtf8() }
        }.getOrElse {
            return invalid(FailureStage.Verification, "verification.manifest_unreadable")
        }

        val parsed = manifestParser.parseAndValidate(
            content = manifestContent,
            expectedModelId = expectedDescriptor.id,
            supportedEngines = setOf(runtime.engine),
            supportedModelAbi = runtime.modelAbi,
        )
        val descriptor = when (parsed) {
            is ManifestValidationResult.Valid -> parsed.descriptor
            is ManifestValidationResult.Invalid -> {
                val stage = if (
                    parsed.diagnosticCode == "manifest.unsupported_engine" ||
                    parsed.diagnosticCode == "manifest.unsupported_model_abi"
                ) {
                    FailureStage.Compatibility
                } else {
                    FailureStage.Verification
                }
                return invalid(stage, parsed.diagnosticCode)
            }
        }

        when {
            descriptor.version != expectedDescriptor.version ->
                return invalid(FailureStage.Verification, "verification.version_mismatch")
            descriptor.filename != expectedDescriptor.filename ->
                return invalid(FailureStage.Verification, "verification.filename_mismatch")
            descriptor.expectedBytes != expectedDescriptor.expectedBytes ->
                return invalid(FailureStage.Verification, "verification.declared_size_mismatch")
            !descriptor.sha256.equals(expectedDescriptor.sha256, ignoreCase = true) ->
                return invalid(FailureStage.Verification, "verification.declared_digest_mismatch")
            !descriptor.runtime.engine.equals(expectedDescriptor.runtime.engine, ignoreCase = true) ->
                return invalid(FailureStage.Compatibility, "verification.runtime_engine_mismatch")
            descriptor.runtime.minimumRuntimeVersion != expectedDescriptor.runtime.minimumRuntimeVersion ->
                return invalid(FailureStage.Compatibility, "verification.runtime_version_mismatch")
            descriptor.runtime.minimumModelAbi != expectedDescriptor.runtime.minimumModelAbi ->
                return invalid(FailureStage.Compatibility, "verification.model_abi_mismatch")
        }

        if (!runtime.engine.equals(descriptor.runtime.engine, ignoreCase = true)) {
            return invalid(FailureStage.Compatibility, "verification.unsupported_engine")
        }
        if (runtime.modelAbi < descriptor.runtime.minimumModelAbi) {
            return invalid(FailureStage.Compatibility, "verification.unsupported_model_abi")
        }
        val runtimeVersion = parseVersion(runtime.version)
            ?: return invalid(FailureStage.Compatibility, "verification.invalid_runtime_version")
        val minimumVersion = parseVersion(descriptor.runtime.minimumRuntimeVersion)
            ?: return invalid(FailureStage.Compatibility, "verification.invalid_minimum_runtime_version")
        if (compareVersions(runtimeVersion, minimumVersion) < 0) {
            return invalid(FailureStage.Compatibility, "verification.unsupported_runtime_version")
        }

        val hashingSource = runCatching {
            HashingSource.sha256(fileSystem.source(modelPath))
        }.getOrElse {
            return invalid(FailureStage.Verification, "verification.model_unreadable")
        }
        var actualBytes = 0L
        val readResult = runCatching {
            hashingSource.use { source ->
                val buffer = Buffer()
                while (true) {
                    val read = source.read(buffer, BUFFER_SIZE)
                    if (read == -1L) break
                    actualBytes += read
                    buffer.clear()
                }
            }
            hashingSource.hash.hex()
        }
        val actualDigest = readResult.getOrElse {
            return invalid(FailureStage.Verification, "verification.model_unreadable")
        }

        if (actualBytes != descriptor.expectedBytes) {
            return invalid(FailureStage.Verification, "verification.size_mismatch")
        }
        if (!actualDigest.equals(descriptor.sha256, ignoreCase = true)) {
            return invalid(FailureStage.Verification, "verification.digest_mismatch")
        }

        return ModelAssetVerificationResult.Verified(
            descriptor = descriptor,
            localPath = modelPath.toString(),
        )
    }

    private fun invalid(stage: FailureStage, code: String) =
        ModelAssetVerificationResult.Invalid(stage, code)

    private fun parseVersion(value: String): List<Int>? {
        if (value.isBlank() || value != value.trim()) return null
        return value.split('.').map { segment ->
            if (segment.isEmpty() || segment.any { !it.isDigit() }) return null
            segment.toIntOrNull() ?: return null
        }
    }

    private fun compareVersions(left: List<Int>, right: List<Int>): Int {
        val size = maxOf(left.size, right.size)
        repeat(size) { index ->
            val comparison = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val BUFFER_SIZE = 8_192L
    }
}
