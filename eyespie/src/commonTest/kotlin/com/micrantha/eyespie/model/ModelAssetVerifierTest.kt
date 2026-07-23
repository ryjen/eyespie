package com.micrantha.eyespie.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ModelAssetVerifierTest {
    private val fileSystem = FakeFileSystem()
    private val verifier = ModelAssetVerifier(fileSystem)
    private val manifestPath = "/model/manifest.json".toPath()
    private val modelPath = "/model/offline-model.task".toPath()

    @Test
    fun verifiesValidArtifact() = runTest {
        writeArtifact()

        val result = verifier.verify(manifestPath, modelPath, descriptor, runtime)

        val verified = assertIs<ModelAssetVerificationResult.Verified>(result)
        assertEquals(descriptor, verified.descriptor)
        assertEquals(modelPath.toString(), verified.localPath)
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun rejectsMalformedManifest() = runTest {
        writeArtifact(manifest = "not-json")

        assertInvalid(
            verifier.verify(manifestPath, modelPath, descriptor, runtime),
            FailureStage.Verification,
            "manifest.invalid_json",
        )
    }

    @Test
    fun rejectsReleaseVersionMismatch() = runTest {
        writeArtifact(manifest = manifest.replace(VERSION, "2026.07.21-2"))

        assertInvalid(
            verifier.verify(manifestPath, modelPath, descriptor, runtime),
            FailureStage.Verification,
            "verification.version_mismatch",
        )
    }

    @Test
    fun rejectsActualSizeMismatch() = runTest {
        writeArtifact(modelContent = "$MODEL_CONTENT-extra")

        assertInvalid(
            verifier.verify(manifestPath, modelPath, descriptor, runtime),
            FailureStage.Verification,
            "verification.size_mismatch",
        )
    }

    @Test
    fun rejectsActualDigestMismatch() = runTest {
        writeArtifact(modelContent = "different payload!")

        assertInvalid(
            verifier.verify(manifestPath, modelPath, descriptor, runtime),
            FailureStage.Verification,
            "verification.digest_mismatch",
        )
    }

    @Test
    fun rejectsUnsupportedRuntimeVersion() = runTest {
        writeArtifact()

        assertInvalid(
            verifier.verify(
                manifestPath,
                modelPath,
                descriptor,
                runtime.copy(version = "0.10.34"),
            ),
            FailureStage.Compatibility,
            "verification.unsupported_runtime_version",
        )
    }

    @Test
    fun rejectsUnsupportedRuntimeAbi() = runTest {
        writeArtifact()

        assertInvalid(
            verifier.verify(
                manifestPath,
                modelPath,
                descriptor,
                runtime.copy(modelAbi = 0),
            ),
            FailureStage.Compatibility,
            "manifest.unsupported_model_abi",
        )
    }

    @Test
    fun stopsHashingWhenCancellationIsDetectedBetweenChunks() = runTest {
        val modelContent = "x".repeat(20_000)
        val largeDescriptor = descriptor.copy(expectedBytes = modelContent.length.toLong())
        writeArtifact(
            manifest = manifest
                .replace("\"sizeBytes\": 18", "\"sizeBytes\": ${modelContent.length}"),
            modelContent = modelContent,
        )
        var checks = 0
        val cancellableVerifier = ModelAssetVerifier(fileSystem) {
            checks += 1
            if (checks == 3) throw CancellationException("cancel verification")
        }

        assertFailsWith<CancellationException> {
            cancellableVerifier.verify(manifestPath, modelPath, largeDescriptor, runtime)
        }

        assertEquals(3, checks)
        fileSystem.checkNoOpenFiles()
    }

    private fun writeArtifact(
        manifest: String = Companion.manifest,
        modelContent: String = MODEL_CONTENT,
    ) {
        fileSystem.createDirectories(manifestPath.parent!!)
        fileSystem.write(manifestPath) { writeUtf8(manifest) }
        fileSystem.write(modelPath) { writeUtf8(modelContent) }
    }

    private fun assertInvalid(
        result: ModelAssetVerificationResult,
        stage: FailureStage,
        diagnosticCode: String,
    ) {
        assertEquals(
            ModelAssetVerificationResult.Invalid(stage, diagnosticCode),
            result,
        )
        fileSystem.checkNoOpenFiles()
    }

    private companion object {
        const val MODEL_CONTENT = "test model payload"
        const val MODEL_DIGEST = "bae77ae8633e61e7906d62148fecbf0f322507fe9b145afb5e3081af6b0e8b88"
        const val VERSION = "2026.07.20-1"

        val descriptor = ModelAssetDescriptor(
            id = "eyespie-offline-model",
            version = VERSION,
            filename = "offline-model.task",
            expectedBytes = 18L,
            sha256 = MODEL_DIGEST,
            runtime = ModelRuntimeCompatibility(
                engine = "mediapipe",
                minimumRuntimeVersion = "0.10.35",
                minimumModelAbi = 1,
            ),
        )
        val runtime = ModelRuntimeCapabilities(
            engine = "mediapipe",
            version = "0.10.35",
            modelAbi = 1,
        )
        val manifest = """
            {
              "schemaVersion": 1,
              "modelId": "eyespie-offline-model",
              "version": "$VERSION",
              "filename": "offline-model.task",
              "sizeBytes": 18,
              "sha256": "$MODEL_DIGEST",
              "runtime": {
                "engine": "mediapipe",
                "minimumRuntimeVersion": "0.10.35",
                "minimumModelAbi": 1
              }
            }
        """.trimIndent()
    }
}
