package com.micrantha.eyespie.game

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.IosEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import platform.Foundation.NSUUID

fun createIosEyespieRuntime(): EyespieRuntime {
    val database = IosEyespieDatabaseFactory().create()
    return EyespieRuntime(
        LocalGameLoop(
            identityRepository = LocalPlayerIdentityRepository(),
            gameRepository = SqlGameRepository(database),
            progressRepository = SqlThingProgressRepository(database),
            embeddingGenerator = MediaPipeImageEmbeddingGenerator(),
            idGenerator = IosLocalGameIdGenerator(),
        ),
    )
}

private class IosLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${NSUUID().UUIDString}")
    override fun nextThingId(): ThingId = ThingId("thing:${NSUUID().UUIDString}")
}
