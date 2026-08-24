package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.gamedetail.GameDetailShareResult
import com.micrantha.eyespie.features.gamedetail.GameSharer
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeImportPreparationResult
import com.micrantha.eyespie.features.home.HomeImportPreview
import com.micrantha.eyespie.features.home.HomeImportResult
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.game.AuthoredThing
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.sharing.GameBundleExportFailureCode
import com.micrantha.eyespie.sharing.GameBundleExportResult
import com.micrantha.eyespie.sharing.GameBundleImportPreviewResult
import com.micrantha.eyespie.sharing.GameBundleImportResult
import com.micrantha.eyespie.sharing.GameDocumentReadResult
import com.micrantha.eyespie.sharing.GameDocumentTransfer
import com.micrantha.eyespie.sharing.GameDocumentWriteResult
import com.micrantha.eyespie.sharing.suggestedGameBundleFileName
import kotlin.coroutines.cancellation.CancellationException

internal class LocalGameAdapter(
    runtime: EyespieRuntime,
    private val documentTransfer: GameDocumentTransfer? = null,
) : GameImportPreparer,
    GameImportConfirmer,
    GameImportCanceller,
    GameCreator,
    ClueAuthor,
    GameSharer,
    GuessSubmitter {
    private val gameLoop = runtime.gameLoop
    private val bundleService = runtime.bundleService
    private var pendingImportBytes: ByteArray? = null

    override suspend fun prepareImport(): HomeImportPreparationResult {
        cancelImport()
        val transfer = documentTransfer
            ?: return HomeImportPreparationResult.Terminal(HomeImportResult.Unavailable)
        return try {
            when (val read = transfer.read()) {
                is GameDocumentReadResult.Success -> when (val preview = bundleService.previewImport(read.bytes)) {
                    is GameBundleImportPreviewResult.Ready -> {
                        pendingImportBytes = read.bytes.copyOf()
                        HomeImportPreparationResult.Ready(
                            HomeImportPreview(
                                gameName = preview.preview.gameName,
                                clueCount = preview.preview.thingCount,
                                creatorIdSuffix = preview.preview.creatorPlayerId.value.takeLast(12),
                                gameIdSuffix = preview.preview.gameId.value.takeLast(12),
                            ),
                        )
                    }
                    is GameBundleImportPreviewResult.InvalidFormat ->
                        HomeImportPreparationResult.Terminal(HomeImportResult.InvalidFile)
                    is GameBundleImportPreviewResult.Failure ->
                        HomeImportPreparationResult.Terminal(HomeImportResult.Failed)
                }
                GameDocumentReadResult.Cancelled -> HomeImportPreparationResult.Terminal(HomeImportResult.Cancelled)
                GameDocumentReadResult.Busy -> HomeImportPreparationResult.Terminal(HomeImportResult.Busy)
                GameDocumentReadResult.TooLarge -> HomeImportPreparationResult.Terminal(HomeImportResult.TooLarge)
                GameDocumentReadResult.Failed -> HomeImportPreparationResult.Terminal(HomeImportResult.Failed)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            cancelImport()
            HomeImportPreparationResult.Terminal(HomeImportResult.Failed)
        }
    }

    override suspend fun confirmImport(): HomeImportResult {
        val bytes = pendingImportBytes ?: return HomeImportResult.Failed
        pendingImportBytes = null
        return try {
            mapImportResult(bundleService.import(bytes))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            HomeImportResult.Failed
        } finally {
            bytes.fill(0)
        }
    }

    override fun cancelImport() {
        pendingImportBytes?.fill(0)
        pendingImportBytes = null
    }

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = gameLoop.createGame(name, clueText, expectedAnswer, targetImage)

    override suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<AuthoredThing> = gameLoop.addClue(gameId, clueText, expectedAnswer, targetImage)

    override suspend fun share(gameId: GameId, gameName: String): GameDetailShareResult {
        val transfer = documentTransfer ?: return GameDetailShareResult.Unavailable
        return try {
            when (val exported = bundleService.export(gameId)) {
                is GameBundleExportResult.Success -> when (
                    transfer.write(
                        suggestedGameBundleFileName(gameName, gameId.value),
                        exported.bytes,
                    )
                ) {
                    GameDocumentWriteResult.Success -> GameDetailShareResult.Shared
                    GameDocumentWriteResult.Cancelled -> GameDetailShareResult.Cancelled
                    GameDocumentWriteResult.Busy -> GameDetailShareResult.Busy
                    GameDocumentWriteResult.TooLarge -> GameDetailShareResult.TooLarge
                    GameDocumentWriteResult.Failed -> GameDetailShareResult.Failed
                }
                is GameBundleExportResult.Failure -> when (exported.code) {
                    GameBundleExportFailureCode.NOT_LOCAL_CREATOR -> GameDetailShareResult.NotLocalCreator
                    else -> GameDetailShareResult.Failed
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GameDetailShareResult.Failed
        }
    }

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = gameLoop.guess(gameId, thingId, guessImage)
}

internal object UnavailableGameSharer : GameSharer {
    override suspend fun share(gameId: GameId, gameName: String): GameDetailShareResult =
        GameDetailShareResult.Unavailable
}

private fun GameBundleImportResult.toHomeImportResult(): HomeImportResult = when (this) {
    is GameBundleImportResult.Imported -> HomeImportResult.Imported
    is GameBundleImportResult.AlreadyPresent -> HomeImportResult.AlreadyPresent
    is GameBundleImportResult.Conflict -> HomeImportResult.Conflict
    is GameBundleImportResult.InvalidFormat -> HomeImportResult.InvalidFile
    is GameBundleImportResult.Failure -> HomeImportResult.Failed
}

private fun mapImportResult(result: GameBundleImportResult): HomeImportResult = result.toHomeImportResult()
