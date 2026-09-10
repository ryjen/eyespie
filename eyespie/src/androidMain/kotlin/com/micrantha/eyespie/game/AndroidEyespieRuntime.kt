package com.micrantha.eyespie.game

import android.content.Context
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.identity.PlatformSigningIdentity
import com.micrantha.eyespie.identity.TelemetryPlayerIdentityRepository
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
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import com.micrantha.eyespie.telemetry.androidDiagnosticIdentity
import java.util.UUID

fun createAndroidEyespieRuntime(context: Context): EyespieRuntime =
    bootstrapAndroidEyespieRuntime(context).requireRuntime()

fun bootstrapAndroidEyespieRuntime(context: Context): EyespieRuntimeBootstrapResult {
    val applicationContext = context.applicationContext
    val diagnosticSink = BoundedDiagnosticSink()
    val telemetry = OperationalTelemetry(diagnosticSink)
    val diagnosticExport = DiagnosticExportService(
        history = diagnosticSink,
        identityProvider = DiagnosticIdentityProvider { androidDiagnosticIdentity() },
    )
    val bootstrapDiagnostics = EyespieBootstrapDiagnostics(
        history = diagnosticSink,
        export = diagnosticExport,
        telemetry = telemetry,
    )

    return try {
        val runtime = telemetry.observeSync(DiagnosticOperation.RUNTIME_INITIALIZE) { root ->
            val database = telemetry.observeSync(
                operation = DiagnosticOperation.DATABASE_OPEN,
                parent = root,
            ) {
                AndroidEyespieDatabaseFactory(applicationContext).create()
            }
            val signingIdentity = telemetry.observeSync(
                operation = DiagnosticOperation.IDENTITY_PROVIDER_INITIALIZE,
                parent = root,
            ) {
                PlatformSigningIdentity()
            }
            val identityRepository = TelemetryPlayerIdentityRepository(
                delegate = LocalPlayerIdentityRepository(signingIdentity),
                telemetry = telemetry,
            )
            val gameRepository = SqlGameRepository(database)
            val embeddingModel = telemetry.observeSync(
                operation = DiagnosticOperation.EMBEDDING_MODEL_LOAD,
                parent = root,
            ) {
                loadAndroidImageEmbeddingModel(applicationContext)
            }

            EyespieRuntime(
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
        EyespieRuntimeBootstrapResult.Ready(runtime)
    } catch (exception: Exception) {
        EyespieRuntimeBootstrapResult.Failed(
            diagnostics = bootstrapDiagnostics,
            cause = exception,
        )
    }
}

private class AndroidLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${UUID.randomUUID()}")
    override fun nextThingId(): ThingId = ThingId("thing:${UUID.randomUUID()}")
}
