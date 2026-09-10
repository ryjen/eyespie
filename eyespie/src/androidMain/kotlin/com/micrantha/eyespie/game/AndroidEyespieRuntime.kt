package com.micrantha.eyespie.game

import android.content.Context
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.identity.PlatformSigningIdentity
import com.micrantha.eyespie.imaging.AndroidImageRotator
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.SkiaThumbnailCodec
import com.micrantha.eyespie.imaging.loadAndroidImageEmbeddingModel
import com.micrantha.eyespie.persistence.AndroidEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlOnboardingPreferenceStore
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import com.micrantha.eyespie.sharing.GameBundleService
import com.micrantha.eyespie.telemetry.BoundedDiagnosticSink
import com.micrantha.eyespie.telemetry.DiagnosticExportService
import com.micrantha.eyespie.telemetry.DiagnosticIdentityProvider
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import com.micrantha.eyespie.telemetry.androidDiagnosticIdentity
import java.util.UUID

fun createAndroidEyespieRuntime(context: Context): EyespieRuntime {
    val applicationContext = context.applicationContext
    val database = AndroidEyespieDatabaseFactory(applicationContext).create()
    val signingIdentity = PlatformSigningIdentity()
    val identityRepository = LocalPlayerIdentityRepository(signingIdentity)
    val gameRepository = SqlGameRepository(database)
    val embeddingModel = loadAndroidImageEmbeddingModel(applicationContext)
    val diagnosticSink = BoundedDiagnosticSink()
    val telemetry = OperationalTelemetry(diagnosticSink)
    val diagnosticExport = DiagnosticExportService(
        history = diagnosticSink,
        identityProvider = DiagnosticIdentityProvider { androidDiagnosticIdentity() },
    )

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
            thumbnailCodec = SkiaThumbnailCodec,
            imageRotator = AndroidImageRotator,
            telemetry = telemetry,
        ),
        bundleService = GameBundleService(
            identityRepository = identityRepository,
            signingIdentity = signingIdentity,
            gameRepository = gameRepository,
            telemetry = telemetry,
        ),
        onboardingPreferences = SqlOnboardingPreferenceStore(database),
        gameThumbnailCache = gameRepository,
        diagnostics = diagnosticSink,
        diagnosticExport = diagnosticExport,
        telemetry = telemetry,
    )
}

private class AndroidLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${UUID.randomUUID()}")
    override fun nextThingId(): ThingId = ThingId("thing:${UUID.randomUUID()}")
}
