package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.clue.ClueValidationError
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage

sealed interface AppScreen {
    data object Home : AppScreen
    data object Onboarding : AppScreen
    data object Create : AppScreen
    data class Play(val gameId: GameId, val thingId: ThingId) : AppScreen
}

sealed interface AppFailure {
    data class Game(val failure: LocalGameFailure) : AppFailure
    data object CameraUnavailable : AppFailure
}

interface AppGameUseCases {
    suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot>
    suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame>
    suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome>
}

internal class RuntimeAppGameUseCases(runtime: EyespieRuntime) : AppGameUseCases {
    private val gameLoop = runtime.gameLoop

    override suspend fun loadSnapshot() = gameLoop.loadSnapshot()

    override suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ) = gameLoop.createGame(name, clueText, expectedAnswer, targetImage)

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ) = gameLoop.guess(gameId, thingId, guessImage)
}

internal fun clueFailureMessage(error: ClueValidationError?): String = when (error) {
    ClueValidationError.BLANK_CLUE -> "Enter a clue before capturing the target."
    ClueValidationError.CLUE_TOO_LONG -> "The clue is too long."
    ClueValidationError.BLANK_EXPECTED_ANSWER -> "Enter the creator-only expected answer."
    ClueValidationError.EXPECTED_ANSWER_TOO_LONG -> "The expected answer is too long."
    null -> "The clue authority is invalid."
}
