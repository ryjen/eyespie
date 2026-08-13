package com.micrantha.eyespie.features.onboarding.usecase

import com.micrantha.eyespie.features.onboarding.entities.AiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.HashingSource
import okio.Path

enum class ModelIntegrityFailure(val diagnosticCode: String) {
    MissingExpectedChecksum("model.integrity.missing_checksum"),
    InvalidExpectedChecksum("model.integrity.invalid_checksum"),
    ModelUnavailable("model.integrity.model_unavailable"),
    ChecksumMismatch("model.integrity.checksum_mismatch"),
}

class ModelIntegrityException(
    val failure: ModelIntegrityFailure,
) : IllegalStateException(
    when (failure) {
        ModelIntegrityFailure.MissingExpectedChecksum -> "model checksum is required"
        ModelIntegrityFailure.InvalidExpectedChecksum -> "model checksum format is invalid"
        ModelIntegrityFailure.ModelUnavailable -> "downloaded model is unavailable"
        ModelIntegrityFailure.ChecksumMismatch -> "downloaded model failed integrity verification"
    }
)

/**
 * Verifies remote model artifacts against their configured SHA-256 before they become usable.
 *
 * The URL-derived [AiModel.fileName] remains only a storage key. It is never treated as
 * evidence about the downloaded file contents.
 */
class ModelIntegrityVerifier(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    fun validateExpectedChecksum(model: AiModel): Result<Unit> = try {
        expectedChecksum(model)
        Result.success(Unit)
    } catch (error: ModelIntegrityException) {
        Result.failure(error)
    }

    suspend fun verify(model: AiModel, filePath: Path): Result<Unit> {
        val expected = try {
            expectedChecksum(model)
        } catch (error: ModelIntegrityException) {
            return Result.failure(error)
        }

        val actual = try {
            hashFile(filePath)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return failure(ModelIntegrityFailure.ModelUnavailable)
        }

        if (!actual.equals(expected, ignoreCase = true)) {
            return failure(ModelIntegrityFailure.ChecksumMismatch)
        }

        return Result.success(Unit)
    }

    private fun expectedChecksum(model: AiModel): String {
        val checksum = model.checksum?.trim()?.takeIf(String::isNotEmpty)
            ?: throw ModelIntegrityException(ModelIntegrityFailure.MissingExpectedChecksum)

        if (checksum.length != SHA256_HEX_LENGTH || checksum.any { !it.isHexDigit() }) {
            throw ModelIntegrityException(ModelIntegrityFailure.InvalidExpectedChecksum)
        }

        return checksum
    }

    private suspend fun hashFile(path: Path): String = withContext(Dispatchers.Default) {
        val hashingSource = HashingSource.sha256(fileSystem.source(path))
        try {
            val buffer = Buffer()
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = hashingSource.read(buffer, BUFFER_SIZE)
                if (read == -1L) break
                buffer.clear()
            }
            hashingSource.hash.hex()
        } finally {
            hashingSource.close()
        }
    }

    private fun failure(failure: ModelIntegrityFailure): Result<Unit> =
        Result.failure(ModelIntegrityException(failure))

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private companion object {
        const val SHA256_HEX_LENGTH = 64
        const val BUFFER_SIZE = 8_192L
    }
}
