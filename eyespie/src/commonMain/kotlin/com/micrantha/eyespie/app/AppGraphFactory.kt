package com.micrantha.eyespie.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringFactory
import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.gamedetail.GameSaver
import com.micrantha.eyespie.features.gamedetail.GameSharer
import com.micrantha.eyespie.features.gamedetail.UnavailableGameSaver
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.features.utility.DiagnosticExportResult
import com.micrantha.eyespie.features.utility.DiagnosticsExporter
import com.micrantha.eyespie.features.utility.UnavailableDiagnosticsExporter
import com.micrantha.eyespie.features.utility.UtilityFactory
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.GameThumbnailCache
import com.micrantha.eyespie.sharing.ExternalGameDocumentSource
import com.micrantha.eyespie.sharing.GameDocumentTransfer
import com.micrantha.eyespie.sharing.GameSharePresenter
import com.micrantha.eyespie.sharing.TelemetryGameDocumentTransfer
import com.micrantha.eyespie.sharing.TelemetryGameSharePresenter
import com.micrantha.eyespie.telemetry.DIAGNOSTIC_ARTIFACT_FILE_NAME
import com.micrantha.eyespie.telemetry.DiagnosticArtifactWriteResult
import com.micrantha.eyespie.telemetry.DiagnosticArtifactWriter
import kotlin.coroutines.cancellation.CancellationException

object AppGraphFactory {
    fun fromRuntime(
        runtime: EyespieRuntime,
        navigation: AppNavigation,
        documentTransfer: GameDocumentTransfer? = null,
        externalDocumentSource: ExternalGameDocumentSource? = null,
        sharePresenter: GameSharePresenter? = null,
        diagnosticArtifactWriter: DiagnosticArtifactWriter? = null,
    ): AppGraph {
        val observedDocumentTransfer = documentTransfer?.let {
            TelemetryGameDocumentTransfer(it, runtime.telemetry)
        }
        val observedSharePresenter = sharePresenter?.let {
            TelemetryGameSharePresenter(it, runtime.telemetry)
        }
        val capabilities = LocalGameAdapter(
            runtime = runtime,
            documentTransfer = observedDocumentTransfer,
            sharePresenter = observedSharePresenter,
            externalDocumentSource = externalDocumentSource,
        )
        return fromCapabilities(
            gameSnapshotLoader = runtime.gameLoop,
            gameThumbnailCache = runtime.gameThumbnailCache,
            gameImportPreparer = capabilities,
            gameImportConfirmer = capabilities,
            gameImportCanceller = capabilities,
            gameCreator = capabilities,
            clueAuthor = capabilities,
            guessSubmitter = capabilities,
            onboardingPreferences = runtime.onboardingPreferences,
            navigation = navigation,
            gameSharer = capabilities,
            gameSaver = capabilities,
            externalDocumentSource = externalDocumentSource,
            separateSaveAction = sharePresenter != null && documentTransfer != null,
            diagnosticsExporter = RuntimeDiagnosticsExporter(runtime, diagnosticArtifactWriter),
        )
    }

    fun fromCapabilities(
        gameSnapshotLoader: GameSnapshotLoader,
        gameThumbnailCache: GameThumbnailCache,
        gameImportPreparer: GameImportPreparer,
        gameImportConfirmer: GameImportConfirmer,
        gameImportCanceller: GameImportCanceller,
        gameCreator: GameCreator,
        clueAuthor: ClueAuthor,
        guessSubmitter: GuessSubmitter,
        onboardingPreferences: OnboardingPreferenceStore,
        navigation: AppNavigation,
        gameSharer: GameSharer = UnavailableGameSharer,
        gameSaver: GameSaver = UnavailableGameSaver,
        externalDocumentSource: ExternalGameDocumentSource? = null,
        separateSaveAction: Boolean = false,
        diagnosticsExporter: DiagnosticsExporter = UnavailableDiagnosticsExporter,
    ): AppGraph {
        val coordinator = AppCoordinator(navigation)
        return AppGraph(
            homeFactory = HomeFactory(
                snapshotLoader = gameSnapshotLoader,
                importPreparer = gameImportPreparer,
                importConfirmer = gameImportConfirmer,
                importCanceller = gameImportCanceller,
                thumbnailCache = gameThumbnailCache,
                output = coordinator::onHomeOutput,
                externalDocumentSource = externalDocumentSource,
            ),
            onboardingFactory = OnboardingFactory(
                onboardingPreferences,
                coordinator::onOnboardingOutput,
            ),
            utilityFactory = UtilityFactory(
                snapshotLoader = gameSnapshotLoader,
                output = coordinator::onUtilityOutput,
                diagnosticsExporter = diagnosticsExporter,
            ),
            createGameFactory = CreateGameFactory(gameCreator, coordinator::onCreateGameOutput),
            gameDetailFactory = GameDetailFactory(
                snapshotLoader = gameSnapshotLoader,
                sharer = gameSharer,
                thumbnailCache = gameThumbnailCache,
                output = coordinator::onGameDetailOutput,
                saver = gameSaver,
                separateSaveAction = separateSaveAction,
            ),
            clueAuthoringFactory = ClueAuthoringFactory(
                clueAuthor,
                coordinator::onClueAuthoringOutput,
            ),
            playGameFactory = PlayGameFactory(
                snapshotLoader = gameSnapshotLoader,
                guessSubmitter = guessSubmitter,
                output = coordinator::onPlayGameOutput,
            ),
            externalAppIntentHandler = ExternalAppIntentHandler(
                snapshotLoader = gameSnapshotLoader,
                navigation = navigation,
                importCanceller = gameImportCanceller,
            ),
        )
    }
}

private class RuntimeDiagnosticsExporter(
    private val runtime: EyespieRuntime,
    private val writer: DiagnosticArtifactWriter?,
) : DiagnosticsExporter {
    override suspend fun export(): DiagnosticExportResult {
        val writer = writer ?: return DiagnosticExportResult.Unavailable
        val bytes = try {
            runtime.diagnosticExport.encodeJson()
        } catch (_: Exception) {
            return DiagnosticExportResult.Failed
        }

        return try {
            when (writer.write(DIAGNOSTIC_ARTIFACT_FILE_NAME, bytes)) {
                DiagnosticArtifactWriteResult.Success -> DiagnosticExportResult.Exported
                DiagnosticArtifactWriteResult.Cancelled -> DiagnosticExportResult.Cancelled
                DiagnosticArtifactWriteResult.Busy -> DiagnosticExportResult.Busy
                DiagnosticArtifactWriteResult.TooLarge -> DiagnosticExportResult.TooLarge
                DiagnosticArtifactWriteResult.Failed -> DiagnosticExportResult.Failed
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DiagnosticExportResult.Failed
        } finally {
            bytes.fill(0)
        }
    }
}
