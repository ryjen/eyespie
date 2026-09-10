package com.micrantha.eyespie.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.gamedetail.GameDetailSaveResult
import com.micrantha.eyespie.features.gamedetail.GameDetailShareResult
import com.micrantha.eyespie.features.gamedetail.GameSaver
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
import com.micrantha.eyespie.sharing.ExternalGameDocumentSource
import com.micrantha.eyespie.sharing.GameBundleExportFailureCode
import com.micrantha.eyespie.sharing.GameBundleExportResult
import com.micrantha.eyespie.sharing.GameBundleImportPreviewResult
import com.micrantha.eyespie.sharing.GameBundleImportResult
import com.micrantha.eyespie.sharing.GameDocumentReadResult
import com.micrantha.eyespie.sharing.GameDocumentTransfer
import com.micrantha.eyespie.sharing.GameDocumentWriteResult
import com.micrantha.eyespie.sharing.GameSharePresentationResult
import com.micrantha.eyespie.sharing.GameSharePresenter
import com.micrantha.eyespie.sharing.suggestedGameBundleFileName
import kotlin.coroutines.cancellation.CancellationException

internal class LocalGameAdapter(
    runtime: EyespieRuntime,
    private val documentTransfer: GameDocumentTransfer? = null,
    private val sharePresenter: GameSharePresenter? = null,
    private val externalDocumentSource: ExternalGameDocumentSource? = null,
) : GameImportPreparer,
    GameImportConfirmer,
    GameImportCanceller,
    GameCreator,
    ClueAuthor,
    GameSharer,
    GameSaver,
    GuessSubmitter {
    private val gameLoop = runtime.gameLoop
    private val bundleService = runtime.bundleService
    private var pendingImportBytes: ByteArray? = null
    private var pendingImportFromExternalDocument = false

    override suspend fun prepareImport(): HomeImportPreparationResult {
        cancelImport()
        val transfer = documentTransfer
            ?: return HomeImportPreparationResult.Terminal(HomeImportResult.Unavailable)
        val externalDocumentWasPending = externalDocumentSource?.pending?.value == true
        var externalReadCompleted = false
        var acknowledgeExternalDocument = false

        return try {
            val read = transfer.read()
            externalReadCompleted = externalDocumentWasPending && read != GameDocumentReadResult.Busy

            when (read) {
                is GameDocumentReadResult.Success -> try {
                    when (val preview = bundleService.previewImport(read.bytes)) {
                        is GameBundleImportPreviewResult.Ready -> {
                            pendingImportBytes = read.bytes.copyOf()
                            pendingImportFromExternalDocument = externalDocumentWasPending
                            // A verified preview is still transient in-memory state. Keep an external
                            // URI pending until confirmation or explicit discard so recreation can
                            // re-run the same bounded verification path.
                            HomeImportPreparationResult.Ready(
                                HomeImportPreview(
                                    gameName = preview.preview.gameName,
                                    clueCount = preview.preview.thingCount,
                                    creatorIdSuffix = preview.preview.creatorPlayerId.value.takeLast(12),
                                    gameIdSuffix = preview.preview.gameId.value.takeLast(12),
                                ),
                            )
                        }
                        is GameBundleImportPreviewResult.InvalidFormat -> {
                            acknowledgeExternalDocument = externalReadCompleted
                            HomeImportPreparationResult.Terminal(HomeImportResult.InvalidFile)
                        }
                        is GameBundleImportPreviewResult.Failure -> {
                            acknowledgeExternalDocument = externalReadCompleted
                            HomeImportPreparationResult.Terminal(HomeImportResult.Failed)
                        }
                    }
                } finally {
                    read.bytes.fill(0)
                }
                GameDocumentReadResult.Cancelled -> {
                    acknowledgeExternalDocument = externalReadCompleted
                    HomeImportPreparationResult.Terminal(HomeImportResult.Cancelled)
                }
                GameDocumentReadResult.Busy -> HomeImportPreparationResult.Terminal(HomeImportResult.Busy)
                GameDocumentReadResult.TooLarge -> {
                    acknowledgeExternalDocument = externalReadCompleted
                    HomeImportPreparationResult.Terminal(HomeImportResult.TooLarge)
                }
                GameDocumentReadResult.Failed -> {
                    acknowledgeExternalDocument = externalReadCompleted
                    HomeImportPreparationResult.Terminal(HomeImportResult.Failed)
                }
            }
        } catch (cancelled: CancellationException) {
            // Cancellation never commits external consumption; recreation/navigation can retry.
            acknowledgeExternalDocument = false
            throw cancelled
        } catch (_: Exception) {
            cancelImport()
            acknowledgeExternalDocument = externalReadCompleted
            HomeImportPreparationResult.Terminal(HomeImportResult.Failed)
        } finally {
            if (acknowledgeExternalDocument) {
                externalDocumentSource?.acknowledgePendingDocument()
            }
        }
    }

    override suspend fun confirmImport(): HomeImportResult {
        val bytes = pendingImportBytes ?: return HomeImportResult.Failed
        val importWasExternal = pendingImportFromExternalDocument
        pendingImportBytes = null
        pendingImportFromExternalDocument = false
        var acknowledgeExternalDocument = false

        return try {
            val result = mapImportResult(bundleService.import(bytes))
            acknowledgeExternalDocument = importWasExternal
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            acknowledgeExternalDocument = importWasExternal
            HomeImportResult.Failed
        } finally {
            bytes.fill(0)
            if (acknowledgeExternalDocument) {
                externalDocumentSource?.acknowledgePendingDocument()
            }
        }
    }

    override fun cancelImport() {
        pendingImportBytes?.fill(0)
        pendingImportBytes = null
        pendingImportFromExternalDocument = false
    }

    override fun discardImport() {
        val discardExternalDocument = pendingImportFromExternalDocument
        cancelImport()
        if (discardExternalDocument) {
            externalDocumentSource?.acknowledgePendingDocument()
        }
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
        if (sharePresenter == null && documentTransfer == null) return GameDetailShareResult.Unavailable
        return try {
            when (val exported = bundleService.export(gameId)) {
                is GameBundleExportResult.Success -> try {
                    val fileName = suggestedGameBundleFileName(gameName, gameId.value)
                    val presenter = sharePresenter
                    if (presenter != null) {
                        when (presenter.present(fileName, exported.bytes)) {
                            GameSharePresentationResult.Presented -> GameDetailShareResult.Presented
                            GameSharePresentationResult.Busy -> GameDetailShareResult.Busy
                            GameSharePresentationResult.TooLarge -> GameDetailShareResult.TooLarge
                            GameSharePresentationResult.Failed -> GameDetailShareResult.Failed
                        }
                    } else {
                        mapLegacyShareWrite(documentTransfer?.write(fileName, exported.bytes))
                    }
                } finally {
                    exported.bytes.fill(0)
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

    override suspend fun save(gameId: GameId, gameName: String): GameDetailSaveResult {
        val transfer = documentTransfer ?: return GameDetailSaveResult.Unavailable
        return try {
            when (val exported = bundleService.export(gameId)) {
                is GameBundleExportResult.Success -> try {
                    when (
                        transfer.write(
                            suggestedGameBundleFileName(gameName, gameId.value),
                            exported.bytes,
                        )
                    ) {
                        GameDocumentWriteResult.Success -> GameDetailSaveResult.Saved
                        GameDocumentWriteResult.Cancelled -> GameDetailSaveResult.Cancelled
                        GameDocumentWriteResult.Busy -> GameDetailSaveResult.Busy
                        GameDocumentWriteResult.TooLarge -> GameDetailSaveResult.TooLarge
                        GameDocumentWriteResult.Failed -> GameDetailSaveResult.Failed
                    }
                } finally {
                    exported.bytes.fill(0)
                }
                is GameBundleExportResult.Failure -> when (exported.code) {
                    GameBundleExportFailureCode.NOT_LOCAL_CREATOR -> GameDetailSaveResult.NotLocalCreator
                    else -> GameDetailSaveResult.Failed
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GameDetailSaveResult.Failed
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

private fun mapLegacyShareWrite(result: GameDocumentWriteResult?): GameDetailShareResult = when (result) {
    GameDocumentWriteResult.Success -> GameDetailShareResult.Shared
    GameDocumentWriteResult.Cancelled -> GameDetailShareResult.Cancelled
    GameDocumentWriteResult.Busy -> GameDetailShareResult.Busy
    GameDocumentWriteResult.TooLarge -> GameDetailShareResult.TooLarge
    GameDocumentWriteResult.Failed -> GameDetailShareResult.Failed
    null -> GameDetailShareResult.Unavailable
}

private fun GameBundleImportResult.toHomeImportResult(): HomeImportResult = when (this) {
    is GameBundleImportResult.Imported -> HomeImportResult.Imported
    is GameBundleImportResult.AlreadyPresent -> HomeImportResult.AlreadyPresent
    is GameBundleImportResult.Conflict -> HomeImportResult.Conflict
    is GameBundleImportResult.InvalidFormat -> HomeImportResult.InvalidFile
    is GameBundleImportResult.Failure -> HomeImportResult.Failed
}

private fun mapImportResult(result: GameBundleImportResult): HomeImportResult = result.toHomeImportResult()
