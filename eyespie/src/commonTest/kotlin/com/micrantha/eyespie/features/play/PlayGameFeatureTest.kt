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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class PlayGameFeatureTest {
    @Test
    fun reducer_accepts_current_guess_and_rejects_stale_completion() {
        val current = testGuessOutcome(similarity = 0.91, matched = true)
        val stale = testGuessOutcome(similarity = 0.2, matched = false)
        val initial = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent("Trip", "Find it", matched = false, bestSimilarity = 0.2),
            loading = false,
        )
        val captured = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCaptured(testImage()))

        assertTrue(captured.busy)
        assertEquals(1L, captured.guessGeneration)

        val ignored = PlayGameReducer.reduce(captured, PlayGameIntent.GuessCompleted(0L, stale))
        assertEquals(captured, ignored)

        val completed = PlayGameReducer.reduce(captured, PlayGameIntent.GuessCompleted(1L, current))
        assertFalse(completed.busy)
        assertEquals(current, completed.latestOutcome)
        assertEquals(initial.content, completed.content)
        assertTrue(completed.matched)
    }

    @Test
    fun matched_clue_cannot_start_another_guess() {
        val initial = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent("Trip", "Find it", matched = true, bestSimilarity = 0.9),
            loading = false,
        )

        val next = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCaptured(testImage()))

        assertEquals(initial, next)
        assertFalse(next.busy)
        assertEquals(0L, next.guessGeneration)
    }

    @Test
    fun state_derives_progress_and_terminal_completion_without_creator_authority() {
        val nextThing = ThingId("thing-2")
        val inProgress = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent(
                gameName = "Trip",
                clueText = "Find it",
                matched = false,
                bestSimilarity = null,
                clueNumber = 1,
                clueCount = 2,
                matchedClueCount = 0,
                nextThingId = nextThing,
            ),
            loading = false,
            latestOutcome = testGuessOutcome(matched = true),
        )

        assertEquals(1, inProgress.matchedClues)
        assertFalse(inProgress.completed)

        val terminal = inProgress.copy(
            content = inProgress.content!!.copy(
                clueNumber = 2,
                matchedClueCount = 1,
                nextThingId = null,
            ),
        )
        assertEquals(2, terminal.matchedClues)
        assertTrue(terminal.completed)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_loads_guesses_and_advances_only_after_match() = runTest {
        val nextThing = ThingId("thing-2")
        val port = FakePlayPort(
            content = PlayGameContent(
                gameName = "Trip",
                clueText = "Find it",
                matched = false,
                bestSimilarity = 0.2,
                clueNumber = 1,
                clueCount = 2,
                matchedClueCount = 0,
                nextThingId = nextThing,
            ),
        )
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(port, outputs::add).create(this, testGameId, testThingId)

        interactor.dispatch(PlayGameIntent.Load)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()
        assertEquals(port.content, interactor.state.value.content)

        interactor.dispatch(PlayGameIntent.NextClueSelected)
        assertTrue(outputs.isEmpty())

        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        assertTrue(interactor.state.value.busy)
        advanceUntilIdle()
        assertEquals(port.outcome, interactor.state.value.latestOutcome)
        assertEquals(1, port.loads)
        assertEquals(1, port.guesses)

        interactor.dispatch(PlayGameIntent.NextClueSelected)
        assertEquals(listOf<PlayGameOutput>(PlayGameOutput.Advance(testGameId, nextThing)), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun terminal_match_stays_on_completion_until_back() = runTest {
        val port = FakePlayPort(
            content = PlayGameContent(
                gameName = "Trip",
                clueText = "Last clue",
                matched = false,
                bestSimilarity = null,
                clueNumber = 2,
                clueCount = 2,
                matchedClueCount = 1,
                nextThingId = null,
            ),
        )
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(port, outputs::add).create(this, testGameId, testThingId)

        interactor.dispatch(PlayGameIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        advanceUntilIdle()

        assertTrue(interactor.state.value.completed)
        interactor.dispatch(PlayGameIntent.NextClueSelected)
        assertTrue(outputs.isEmpty())

        interactor.dispatch(PlayGameIntent.Back)
        assertEquals(listOf<PlayGameOutput>(PlayGameOutput.Closed(testGameId)), outputs)
    }

    @Test
    fun stale_failure_cannot_replace_newer_success() {
        val initial = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent("Trip", "Find it", matched = false, bestSimilarity = null),
            loading = false,
        )
        val firstCapture = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCaptured(testImage()))
        val currentOutcome = testGuessOutcome(matched = true)
        val succeeded = PlayGameReducer.reduce(
            firstCapture,
            PlayGameIntent.GuessCompleted(firstCapture.guessGeneration, currentOutcome),
        )

        val staleFailure = PlayGameReducer.reduce(
            succeeded,
            PlayGameIntent.OperationFailed(0L, com.micrantha.eyespie.game.LocalGameFailure(com.micrantha.eyespie.game.LocalGameFailureCode.GUESS_EMBEDDING_FAILED)),
        )

        assertEquals(succeeded, staleFailure)
        assertNull(staleFailure.failure)
        assertEquals(currentOutcome, staleFailure.latestOutcome)
    }
}

private class FakePlayPort(
    val content: PlayGameContent = PlayGameContent("Trip", "Find it", matched = false, bestSimilarity = 0.2),
    val outcome: GuessOutcome = testGuessOutcome(),
) : PlayGamePort {
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
