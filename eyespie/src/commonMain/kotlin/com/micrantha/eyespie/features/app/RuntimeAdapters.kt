package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.create.CreateGamePort
import com.micrantha.eyespie.features.gamedetail.GameDetailContent
import com.micrantha.eyespie.features.gamedetail.GameDetailPort
import com.micrantha.eyespie.features.gamedetail.GameDetailShareResult
import com.micrantha.eyespie.features.gamedetail.GameDetailThing
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeGame
import com.micrantha.eyespie.features.home.HomeImportResult
import com.micrantha.eyespie.features.home.HomePort
import com.micrantha.eyespie.features.home.HomeThing
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGamePort
import com.micrantha.eyespie.game.CreatedClue
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.sharing.GameBundleExportFailureCode
import com.micrantha.eyespie.sharing.GameBundleExportResult
import com.micrantha.eyespie.sharing.GameBundleImportResult
import com.micrantha.eyespie.sharing.GameDocumentReadResult
import com.micrantha.eyespie.sharing.GameDocumentTransfer
import com.micrantha.eyespie.sharing.GameDocumentWriteResult
import com.micrantha.eyespie.sharing.suggestedGameBundleFileName
import kotlin.coroutines.cancellation.CancellationException

internal class LocalGameAdapter(
    runtime: EyespieRuntime,
    private val documentTransfer: GameDocumentTransfer? = null,
) : HomePort, CreateGamePort, GameDetailPort, PlayGamePort {
    private val gameLoop = runtime.gameLoop
    private val bundleService = runtime.bundleService

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

    override suspend fun importGame(): HomeImportResult {
        val transfer = documentTransfer ?: return HomeImportResult.Unavailable
        return try {
            when (val read = transfer.read()) {
                is GameDocumentReadResult.Success -> when (bundleService.import(read.bytes)) {
                    is GameBundleImportResult.Imported -> HomeImportResult.Imported
                    is GameBundleImportResult.AlreadyPresent -> HomeImportResult.AlreadyPresent
                    is GameBundleImportResult.Conflict -> HomeImportResult.Conflict
                    is GameBundleImportResult.InvalidFormat -> HomeImportResult.InvalidFile
                    is GameBundleImportResult.Failure -> HomeImportResult.Failed
                }
                GameDocumentReadResult.Cancelled -> HomeImportResult.Cancelled
                GameDocumentReadResult.Busy -> HomeImportResult.Busy
                GameDocumentReadResult.TooLarge -> HomeImportResult.TooLarge
                GameDocumentReadResult.Failed -> HomeImportResult.Failed
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            HomeImportResult.Failed
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
    ): LocalGameResult<CreatedClue> = gameLoop.addClue(gameId, clueText, expectedAnswer, targetImage)

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
                val thing = game.things.firstOrNull { it.id == thingId }
                    ?: return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.THING_NOT_FOUND))
                LocalGameResult.Success(
                    PlayGameContent(
                        gameName = game.name,
                        clueText = thing.clue.clueText,
                        matched = thing.progress?.matched ?: false,
                        bestSimilarity = thing.progress?.bestSimilarity,
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

internal class HomeBackedGameDetailPort(
    private val homePort: HomePort,
) : GameDetailPort {
    override suspend fun load(gameId: GameId): LocalGameResult<GameDetailContent> = when (val result = homePort.load()) {
        is LocalGameResult.Failure -> result
        is LocalGameResult.Success -> {
            val game = result.value.games.firstOrNull { it.id == gameId }
                ?: return LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.GAME_NOT_FOUND))
            LocalGameResult.Success(game.toGameDetailContent())
        }
    }
}

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
