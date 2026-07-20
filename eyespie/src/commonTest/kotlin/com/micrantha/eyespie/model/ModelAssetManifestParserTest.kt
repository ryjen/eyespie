package com.micrantha.eyespie.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ModelAssetManifestParserTest {
    private val parser = ModelAssetManifestParser()

    @Test
    fun parsesValidImmutableManifest() {
        val result = parser.parseAndValidate(validManifest, expectedModelId = "eyespie-offline-model")

        val valid = assertIs<ManifestValidationResult.Valid>(result)
        assertEquals("2026.07.20-1", valid.descriptor.version)
        assertEquals(612_368_384L, valid.descriptor.expectedBytes)
        assertEquals("mediapipe", valid.descriptor.runtime.engine)
    }

    @Test
    fun rejectsLatestVersion() {
        val result = parser.parseAndValidate(validManifest.replace("2026.07.20-1", "latest"))

        assertEquals(
            ManifestValidationResult.Invalid("manifest.mutable_version"),
            result,
        )
    }

    @Test
    fun rejectsLatestVersionWithWhitespace() {
        val result = parser.parseAndValidate(validManifest.replace("2026.07.20-1", " latest "))

        assertEquals(
            ManifestValidationResult.Invalid("manifest.mutable_version"),
            result,
        )
    }

    @Test
    fun rejectsVersionWithLeadingOrTrailingWhitespace() {
        val result = parser.parseAndValidate(validManifest.replace("2026.07.20-1", " 2026.07.20-1 "))

        assertEquals(
            ManifestValidationResult.Invalid("manifest.mutable_version"),
            result,
        )
    }

    @Test
    fun rejectsModelIdWithLeadingOrTrailingWhitespace() {
        val result = parser.parseAndValidate(
            validManifest.replace("eyespie-offline-model", " eyespie-offline-model "),
        )

        assertEquals(
            ManifestValidationResult.Invalid("manifest.missing_model_id"),
            result,
        )
    }

    @Test
    fun rejectsModelIdMismatch() {
        val result = parser.parseAndValidate(validManifest, expectedModelId = "different-model")

        assertEquals(
            ManifestValidationResult.Invalid("manifest.model_id_mismatch"),
            result,
        )
    }

    @Test
    fun rejectsInvalidDigest() {
        val result = parser.parseAndValidate(validManifest.replace("a".repeat(64), "not-a-digest"))

        assertEquals(
            ManifestValidationResult.Invalid("manifest.invalid_sha256"),
            result,
        )
    }

    @Test
    fun rejectsUnsupportedModelAbi() {
        val result = parser.parseAndValidate(validManifest, supportedModelAbi = 0)

        assertEquals(
            ManifestValidationResult.Invalid("manifest.unsupported_model_abi"),
            result,
        )
    }

    @Test
    fun rejectsDotPathComponentFilename() {
        assertInvalidFilename(".")
    }

    @Test
    fun rejectsParentPathComponentFilename() {
        assertInvalidFilename("..")
    }

    @Test
    fun rejectsFilenameWithPathSeparator() {
        assertInvalidFilename("models/gemma.task")
    }

    @Test
    fun rejectsFilenameWithLeadingOrTrailingWhitespace() {
        assertInvalidFilename("  gemma.task  ")
    }

    private fun assertInvalidFilename(filename: String) {
        val result = parser.parseAndValidate(manifestWithFilename(filename))

        assertEquals(
            ManifestValidationResult.Invalid("manifest.invalid_filename"),
            result,
        )
    }

    private fun manifestWithFilename(filename: String) =
        validManifest.replace("offline-model.task", filename)

    private companion object {
        val validManifest = """
            {
              "schemaVersion": 1,
              "modelId": "eyespie-offline-model",
              "version": "2026.07.20-1",
              "filename": "offline-model.task",
              "sizeBytes": 612368384,
              "sha256": "${"a".repeat(64)}",
              "runtime": {
                "engine": "mediapipe",
                "minimumRuntimeVersion": "0.10.26",
                "minimumModelAbi": 1
              }
            }
        """.trimIndent()
    }
}
