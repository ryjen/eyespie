package com.micrantha.eyespie.features.scan.usecase

import com.micrantha.bluebell.domain.usecase.dispatchUseCase
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.eyespie.core.data.account.model.CurrentSession
import com.micrantha.eyespie.domain.entities.Proof
import com.micrantha.eyespie.domain.entities.Thing
import com.micrantha.eyespie.domain.repository.StorageRepository
import com.micrantha.eyespie.domain.repository.ThingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Path
import kotlin.coroutines.coroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UploadCaptureUseCase(
    private val storageRepository: StorageRepository,
    private val thingRepository: ThingRepository,
    private val fileSystem: FileSystem,
    private val session: CurrentSession = CurrentSession
) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        proof: Proof,
        image: Path
    ): Result<Thing> = dispatchUseCase(coroutineContext) {
        val imageData = withContext(Dispatchers.IO) {
            fileSystem.fileRead(image)
        }

        val playerID = session.requirePlayer().id

        val imageID = Uuid.random().toString()

        storageRepository.upload(
            "${playerID}/${imageID}.jpg",
            imageData
        ).map { url ->
            thingRepository.create(proof, url, playerID).getOrThrow()
        }.getOrThrow()
    }
}
