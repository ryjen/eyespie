package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.create.CreateGamePort
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomePort
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGamePort
import com.micrantha.eyespie.game.CreatedClue
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

internal object AppTestPorts : HomePort, CreateGamePort, PlayGamePort {
    override suspend fun load(): LocalGameResult<HomeContent> =
        LocalGameResult.Success(HomeContent("Agent", "player-1", emptyList()))

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = error("not used")

    override suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedClue> = error("not used")

    override suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent> =
        LocalGameResult.Success(PlayGameContent("Trip", "Find it", false, null))

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = error("not used")
}
