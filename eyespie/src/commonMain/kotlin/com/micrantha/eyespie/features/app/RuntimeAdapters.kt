package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.create.CreateGamePort
import com.micrantha.eyespie.features.gamedetail.GameDetailContent
import com.micrantha.eyespie.features.gamedetail.GameDetailPort
import com.micrantha.eyespie.features.gamedetail.GameDetailThing
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeGame
import com.micrantha.eyespie.features.home.HomePort
import com.micrantha.eyespie.features.home.HomeThing
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGamePort
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

internal class LocalGameAdapter(
    runtime: EyespieRuntime,
) : HomePort, CreateGamePort, PlayGamePort {
    private val gameLoop = runtime.gameLoop

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
                    )
                },
            ),
        )
        is LocalGameResult.Failure -> result
    }

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = gameLoop.createGame(name, clueText, expectedAnswer, targetImage)

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
            LocalGameResult.Success(
                GameDetailContent(
                    name = game.name,
                    things = game.things.map { thing ->
                        GameDetailThing(
                            id = thing.id,
                            clueText = thing.clueText,
                            matched = thing.matched,
                            bestSimilarity = thing.bestSimilarity,
                        )
                    },
                ),
            )
        }
    }
}
