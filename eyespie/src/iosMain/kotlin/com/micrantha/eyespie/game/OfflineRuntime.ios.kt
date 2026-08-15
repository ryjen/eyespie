package com.micrantha.eyespie.game

import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.IosEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import platform.Foundation.NSUUID

fun createIosOfflineRuntime(): OfflineRuntimeState = try {
    val database = IosEyespieDatabaseFactory().create()
    OfflineRuntimeState.Ready(
        OfflineGameCoordinator(
            identityRepository = LocalPlayerIdentityRepository(),
            gameRepository = SqlGameRepository(database),
            progressRepository = SqlThingProgressRepository(database),
            embeddingGenerator = MediaPipeImageEmbeddingGenerator(),
            idGenerator = { NSUUID().UUIDString },
        ),
    )
} catch (_: Throwable) {
    OfflineRuntimeState.Unavailable(OfflineRuntimeUnavailableReason.LOCAL_STORAGE_UNAVAILABLE)
}
