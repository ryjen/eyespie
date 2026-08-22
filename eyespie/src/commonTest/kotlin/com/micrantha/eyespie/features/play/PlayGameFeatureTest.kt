package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testGuessOutcome
import com.micrantha.eyespie.testsupport.testImage
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class PlayGameFeatureTest {
    @Test
    fun reducer_preserves_guess_outcome_without_copying_game_snapshot() {
        val outcome = testGuessOutcome()
        val initial = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent("Trip", "Find it", matched = false, bestSimilarity = 0.2),
            loading = false,
            busy = true,
        )

        val next = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCompleted(outcome))

        assertEquals(outcome, next.latestOutcome)
        assertEquals(initial.content, next.content)
        assertFalse(next.busy)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_injects_port_for_load_and_guess() = runTest {
        val port = FakePlayPort()
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(port, outputs::add).create(this, testGameId, testThingId)

        interactor.dispatch(PlayGameIntent.Load)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()
        assertEquals(port.content, interactor.state.value.content)

        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        assertTrue(interactor.state.value.busy)
        advanceUntilIdle()
        assertEquals(port.outcome, interactor.state.value.latestOutcome)
        assertEquals(1, port.loads)
        assertEquals(1, port.guesses)

        interactor.dispatch(PlayGameIntent.Back)
        assertEquals(listOf<PlayGameOutput>(PlayGameOutput.Closed), outputs)
    }
}

private class FakePlayPort : PlayGamePort {
    val content = PlayGameContent("Trip", "Find it", matched = false, bestSimilarity = 0.2)
    val outcome = testGuessOutcome()
    var loads = 0
    var guesses = 0

    override suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent> {
        loads += 1
        return LocalGameResult.Success(content)
    }

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> {
        guesses += 1
        return LocalGameResult.Success(outcome)
    }
}
