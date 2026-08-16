package com.micrantha.eyespie.game

import android.content.Context
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.identity.PlatformSigningIdentity
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.loadAndroidImageEmbeddingModel
import com.micrantha.eyespie.persistence.AndroidEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import com.micrantha.eyespie.sharing.GameBundleService
import java.util.UUID

fun createAndroidEyespieRuntime(context: Context): EyespieRuntime {
    val applicationContext = context.applicationContext
    val database = AndroidEyespieDatabaseFactory(applicationContext).create()
    val signingIdentity = PlatformSigningIdentity()
    val identityRepository = LocalPlayerIdentityRepository(signingIdentity)
    val gameRepository = SqlGameRepository(database)
    val embeddingModel = loadAndroidImageEmbeddingModel(applicationContext)

    return EyespieRuntime(
        gameLoop = LocalGameLoop(
            identityRepository = identityRepository,
            gameRepository = gameRepository,
            progressRepository = SqlThingProgressRepository(database),
            embeddingGenerator = MediaPipeImageEmbeddingGenerator(
                context = applicationContext,
                modelBuffer = embeddingModel.directBuffer(),
            ),
            idGenerator = AndroidLocalGameIdGenerator(),
        ),
        bundleService = GameBundleService(
            identityRepository = identityRepository,
            signingIdentity = signingIdentity,
            gameRepository = gameRepository,
        ),
    )
}

private class AndroidLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${UUID.randomUUID()}")
    override fun nextThingId(): ThingId = ThingId("thing:${UUID.randomUUID()}")
}
