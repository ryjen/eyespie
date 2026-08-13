package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.eyespie.features.onboarding.entities.AiModel
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelIntegrityVerifierTest {
    private val fileSystem = FakeFileSystem()
    private val verifier = ModelIntegrityVerifier(fileSystem)
    private val modelPath = "/models/test-model.litertlm".toPath()

    @Test
    fun verifiesDownloadedFileBytes() = runTest {
        writeModel()

        assertTrue(verifier.verify(model(), modelPath).isSuccess)
        fileSystem.checkNoOpenFiles()
    }

    @Test
    fun rejectsChecksumMismatch() = runTest {
        writeModel()

        assertFailure(
            verifier.verify(model(checksum = "0".repeat(64)), modelPath),
            ModelIntegrityFailure.ChecksumMismatch,
        )
    }

    @Test
    fun rejectsMissingDownloadedFile() = runTest {
        assertFailure(
            verifier.verify(model(), modelPath),
            ModelIntegrityFailure.ModelUnavailable,
        )
    }

    @Test
    fun rejectsMissingExpectedChecksum() = runTest {
        writeModel()

        assertFailure(
            verifier.verify(model(checksum = null), modelPath),
            ModelIntegrityFailure.MissingExpectedChecksum,
        )
    }

    @Test
    fun rejectsMalformedExpectedChecksumBeforeDownload() {
        val result = verifier.validateExpectedChecksum(model(checksum = "not-a-sha256"))

        assertFailure(result, ModelIntegrityFailure.InvalidExpectedChecksum)
    }

    private fun writeModel() {
        fileSystem.createDirectories(modelPath.parent!!)
        fileSystem.write(modelPath) { writeUtf8(MODEL_CONTENT) }
    }

    private fun model(checksum: String? = MODEL_DIGEST) = AiModel(
        url = "https://example.invalid/test-model.litertlm",
        checksum = checksum,
    )

    private fun assertFailure(result: Result<Unit>, expected: ModelIntegrityFailure) {
        val error = assertIs<ModelIntegrityException>(result.exceptionOrNull())
        assertEquals(expected, error.failure)
        fileSystem.checkNoOpenFiles()
    }

    private companion object {
        const val MODEL_CONTENT = "test model payload"
        const val MODEL_DIGEST = "bae77ae8633e61e7906d62148fecbf0f322507fe9b145afb5e3081af6b0e8b88"
    }
}
