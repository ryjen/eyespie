package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.gamedetail.GameDetailContent
import com.micrantha.eyespie.features.gamedetail.GameDetailLoader
import com.micrantha.eyespie.features.gamedetail.GameDetailShareResult
import com.micrantha.eyespie.features.gamedetail.GameDetailThing
import com.micrantha.eyespie.features.gamedetail.GameSharer
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeGame
import com.micrantha.eyespie.features.home.HomeImportPreparationResult
import com.micrantha.eyespie.features.home.HomeImportPreview
import com.micrantha.eyespie.features.home.HomeImportResult
import com.micrantha.eyespie.features.home.HomeLoader
import com.micrantha.eyespie.features.home.HomeThing
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGameLoader
import com.micrantha.eyespie.features.utility.UtilityContent
import com.micrantha.eyespie.features.utility.UtilityLoader
import com.micrantha.eyespie.game.AuthoredThing
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
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
) : HomeLoader,
    GameImportPreparer,
    GameImportConfirmer,
    GameImportCanceller,
    UtilityLoader,
    GameCreator,
    ClueAuthor,
    GameDetailLoader,
    GameSharer,
    PlayGameLoader,
    GuessSubmitter {
    private val gameLoop = runtime.gameLoop
    private val bundleService = runtime.bundleService
    private var pendingImportBytes: ByteArray? = null

    override suspend fun load(): LocalGameResult<HomeContent> = when (val result = gameLoop.loadSnapshot()) {
        is LocalGameResult.Success -> LocalGameResult.Success(
            HomeContent(
                identityDisplayName = result.value.identity.displayName,
                identityIdSuffix = result.value.identity.id.value.takeLast(12),
                games = result.value.games.map { game ->
                    HomeGame(
                        id = game.id,
                        name = game.name,
                        things = game.things.map { thing ->
                            HomeThing(
                                id = thing.id,
                                clueText = thing.clue.clueText,
                                matched = thing.progress?.matched ?: false,
                                bestSimilarity = thing.progress?.bestSimilarity,
                            )
                        },
                        localCreator = game.localCreator,
                    )
                },
            ),
        )
        is LocalGameResult.Failure -> result
    }

    override suspend fun loadUtility(): LocalGameResult<UtilityContent> =
        when (val result = gameLoop.loadSnapshot()) {
            is LocalGameResult.Success -> LocalGameResult.Success(
                UtilityContent(
                    identityDisplayName = result.value.identity.displayName,
                    identityIdSuffix = result.value.identity.id.value.takeLast(12),
                ),
            )
            is LocalGameResult.Failure -> result
        }

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

    override suspend fun load(gameId: GameId): LocalGameResult<GameDetailContent> =
        when (val result = load()) {
            is LocalGameResult.Failure -> result
            is LocalGameResult.Success -> {
                val game = result.value.games.firstOrNull { it.id == gameId }
                    ?: return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.GAME_NOT_FOUND))
                LocalGameResult.Success(game.toGameDetailContent())
            }
        }

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

    override suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent> {
        return when (val result = gameLoop.loadSnapshot()) {
            is LocalGameResult.Failure -> result
            is LocalGameResult.Success -> {
                val game = result.value.games.firstOrNull { it.id == gameId }
                    ?: return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.GAME_NOT_FOUND))
                val currentIndex = game.things.indexOfFirst { it.id == thingId }
                if (currentIndex < 0) {
                    return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.THING_NOT_FOUND))
                }
                val thing = game.things[currentIndex]
                val nextUnmatched = game.things.firstOrNull { candidate ->
                    candidate.id != thingId && candidate.progress?.matched != true
                }
                LocalGameResult.Success(
                    PlayGameContent(
                        gameName = game.name,
                        clueText = thing.clue.clueText,
                        matched = thing.progress?.matched ?: false,
                        bestSimilarity = thing.progress?.bestSimilarity,
                        clueNumber = currentIndex + 1,
                        clueCount = game.things.size,
                        matchedClueCount = game.things.count { it.progress?.matched == true },
                        nextThingId = nextUnmatched?.id,
                    ),
                )
            }
        }
    }

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = gameLoop.guess(gameId, thingId, guessImage)
}

internal class HomeBackedGameDetailLoader(
    private val homeLoader: HomeLoader,
) : GameDetailLoader {
    override suspend fun load(gameId: GameId): LocalGameResult<GameDetailContent> = when (val result = homeLoader.load()) {
        is LocalGameResult.Failure -> result
        is LocalGameResult.Success -> {
            val game = result.value.games.firstOrNull { it.id == gameId }
                ?: return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.GAME_NOT_FOUND))
            LocalGameResult.Success(game.toGameDetailContent())
        }
    }
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

private fun HomeGame.toGameDetailContent(): GameDetailContent = GameDetailContent(
    name = name,
    things = things.map { thing ->
        GameDetailThing(
            id = thing.id,
            clueText = thing.clueText,
            matched = thing.matched,
            bestSimilarity = thing.bestSimilarity,
        )
    },
    localCreator = localCreator,
)
