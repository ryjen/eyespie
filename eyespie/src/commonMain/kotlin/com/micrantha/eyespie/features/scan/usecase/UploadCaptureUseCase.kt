package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.bluebell.observability.logger
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.entities.floats
import com.micrantha.eyespie.domain.repository.StorageRepository
import com.micrantha.eyespie.domain.repository.ThingRepository
import com.micrantha.eyespie.platform.scan.LoadCameraImageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.coroutines.coroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface UploadCaptureUseCase {
    suspend operator fun invoke(
        proof: Proof,
        image: Path
    ): Result<Thing>
}

class UploadCaptureUseCaseImpl(
    private val storageRepository: StorageRepository,
    private val thingRepository: ThingRepository,
    private val fileSystem: FileSystem,
    private val imageEmbeddingGenerator: ImageEmbeddingGenerator,
    private val loadCameraImageUseCase: LoadCameraImageUseCase,
    private val session: CurrentSession = CurrentSession
) : UploadCaptureUseCase {

<<<<<<< Updated upstream
||||||| Stash base
    override fun close() {
        imageEmbeddingGenerator.close()
    }

=======
    private val log by logger()

    override fun close() {
        imageEmbeddingGenerator.close()
    }

>>>>>>> Stashed changes
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun invoke(
        proof: Proof,
        image: Path
    ): Result<Thing> = dispatchUseCase(coroutineContext) {
        val playerID = session.requirePlayer().id

        val imageData = withContext(Dispatchers.IO) {
            fileSystem.fileRead(image)
        }
        val imageExtension = imageData.captureImageExtension()

        val cameraImage = loadCameraImageUseCase(image).getOrThrow()
        val embedding = imageEmbeddingGenerator.generate(cameraImage)

        log.debug { "generated embedding version $modelVersion with ${embedding.floats().size} dimensions" }

        val imageID = Uuid.random().toString()

        storageRepository.upload(
            "${playerID}/${imageID}.${imageExtension}",
            imageData
        ).onFailure {
            log.error(it) { "failed to upload image" }
        }.map { url ->
            thingRepository.create(
                proof.copy(embedding = embedding),
                url,
                playerID
            ).onFailure {
                log.error(it) { "failed to create thing" }
            }.getOrThrow()
        }.getOrThrow()
    }
}

internal fun ByteArray.captureImageExtension(): String = when {
    hasPrefix(PNG_SIGNATURE) -> "png"
    hasPrefix(JPEG_SIGNATURE) -> "jpg"
    else -> throw IllegalArgumentException("unsupported capture image encoding")
}

private fun ByteArray.hasPrefix(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
)
private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
