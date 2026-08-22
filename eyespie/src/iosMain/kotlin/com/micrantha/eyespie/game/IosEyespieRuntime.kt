package com.micrantha.eyespie.game

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.identity.PlatformSigningIdentity
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.persistence.IosEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlOnboardingPreferenceStore
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import com.micrantha.eyespie.sharing.GameBundleService
import platform.Foundation.NSUUID

fun createIosEyespieRuntime(): EyespieRuntime {
    val database = IosEyespieDatabaseFactory().create()
    val signingIdentity = PlatformSigningIdentity()
    val identityRepository = LocalPlayerIdentityRepository(signingIdentity)
    val gameRepository = SqlGameRepository(database)

    return EyespieRuntime(
        gameLoop = LocalGameLoop(
            identityRepository = identityRepository,
            gameRepository = gameRepository,
            progressRepository = SqlThingProgressRepository(database),
            embeddingGenerator = MediaPipeImageEmbeddingGenerator(),
            idGenerator = IosLocalGameIdGenerator(),
        ),
        bundleService = GameBundleService(
            identityRepository = identityRepository,
            signingIdentity = signingIdentity,
            gameRepository = gameRepository,
        ),
        onboardingPreferences = SqlOnboardingPreferenceStore(database),
    )
}

private class IosLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${NSUUID().UUIDString}")
    override fun nextThingId(): ThingId = ThingId("thing:${NSUUID().UUIDString}")
}
