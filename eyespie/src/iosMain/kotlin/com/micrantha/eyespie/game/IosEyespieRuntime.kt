package com.micrantha.eyespie.game

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.identity.LocalPlayerIdentityRepository
import com.micrantha.eyespie.identity.PlatformSigningIdentity
import com.micrantha.eyespie.identity.TelemetryPlayerIdentityRepository
import com.micrantha.eyespie.imaging.IosImageRotator
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.SkiaThumbnailCodec
import com.micrantha.eyespie.imaging.loadIosImageEmbeddingModel
import com.micrantha.eyespie.persistence.IosEyespieDatabaseFactory
import com.micrantha.eyespie.persistence.SqlGameRepository
import com.micrantha.eyespie.persistence.SqlOnboardingPreferenceStore
import com.micrantha.eyespie.persistence.SqlThingProgressRepository
import com.micrantha.eyespie.sharing.GameBundleService
import com.micrantha.eyespie.telemetry.BoundedDiagnosticSink
import com.micrantha.eyespie.telemetry.DiagnosticExportService
import com.micrantha.eyespie.telemetry.DiagnosticIdentityProvider
import com.micrantha.eyespie.telemetry.DiagnosticOperation
import com.micrantha.eyespie.telemetry.OperationalTelemetry
import com.micrantha.eyespie.telemetry.iosDiagnosticIdentity
import platform.Foundation.NSUUID

fun createIosEyespieRuntime(): EyespieRuntime = bootstrapIosEyespieRuntime().requireRuntime()

fun bootstrapIosEyespieRuntime(): EyespieRuntimeBootstrapResult {
    val diagnosticSink = BoundedDiagnosticSink()
    val telemetry = OperationalTelemetry(diagnosticSink)
    val diagnosticExport = DiagnosticExportService(
        history = diagnosticSink,
        identityProvider = DiagnosticIdentityProvider { iosDiagnosticIdentity() },
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
                IosEyespieDatabaseFactory().create()
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
                loadIosImageEmbeddingModel()
            }

            EyespieRuntime(
                gameLoop = LocalGameLoop(
                    identityRepository = identityRepository,
                    gameRepository = gameRepository,
                    progressRepository = SqlThingProgressRepository(database),
                    embeddingGenerator = MediaPipeImageEmbeddingGenerator(
                        modelPathProvider = { embeddingModel.path },
                    ),
                    idGenerator = IosLocalGameIdGenerator(),
                    thumbnailCodec = SkiaThumbnailCodec,
                    imageRotator = IosImageRotator,
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

private class IosLocalGameIdGenerator : LocalGameIdGenerator {
    override fun nextGameId(): GameId = GameId("game:${NSUUID().UUIDString}")
    override fun nextThingId(): ThingId = ThingId("thing:${NSUUID().UUIDString}")
}
