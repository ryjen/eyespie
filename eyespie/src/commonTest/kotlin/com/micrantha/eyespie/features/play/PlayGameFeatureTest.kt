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
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class PlayGameFeatureTest {
    private val nextThingId = ThingId("thing-2")

    @Test
    fun reducer_maps_match_to_safe_feedback_and_progress() {
        val initial = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent(
                gameName = "Trip",
                clueText = "Find it",
                matched = false,
                bestSimilarity = 0.2,
                foundCount = 0,
                totalCount = 2,
                nextThingId = nextThingId,
            ),
            loading = false,
            busy = true,
            guessGeneration = 3,
        )

        val next = PlayGameReducer.reduce(
            initial,
            PlayGameIntent.GuessCompleted(3, testGuessOutcome()),
        )

        val feedback = assertIs<PlayFeedback.Matched>(next.feedback)
        assertEquals(1, feedback.foundCount)
        assertEquals(2, feedback.totalCount)
        assertEquals(nextThingId, feedback.nextThingId)
        assertEquals(0.9, feedback.similarity)
        assertEquals(1, next.content?.foundCount)
        assertEquals(true, next.content?.matched)
        assertFalse(next.busy)
    }

    @Test
    fun reducer_ignores_stale_load_and_guess_callbacks() {
        val content = PlayGameContent("Trip", "Find it", false, null)
        val state = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            loading = true,
            busy = true,
            loadGeneration = 4,
            guessGeneration = 7,
        )

        assertEquals(
            state,
            PlayGameReducer.reduce(state, PlayGameIntent.ContentLoaded(3, content)),
        )
        assertEquals(
            state,
            PlayGameReducer.reduce(state, PlayGameIntent.GuessCompleted(6, testGuessOutcome())),
        )
    }

    @Test
    fun mismatch_keeps_route_retryable_without_marking_progress_found() {
        val initial = PlayGameState(
            gameId = testGameId,
            thingId = testThingId,
            content = PlayGameContent("Trip", "Find it", false, 0.2, foundCount = 0, totalCount = 2),
            loading = false,
            busy = true,
            guessGeneration = 1,
        )

        val next = PlayGameReducer.reduce(
            initial,
            PlayGameIntent.GuessCompleted(1, testGuessOutcome(similarity = 0.5, matched = false)),
        )

        assertIs<PlayFeedback.Mismatch>(next.feedback)
        assertEquals(0, next.content?.foundCount)
        assertEquals(false, next.content?.matched)
        assertFalse(next.busy)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun match_persists_once_then_advances_semantically_and_blocks_repeat_capture() = runTest {
        val port = FakePlayPort(
            content = PlayGameContent(
                "Trip",
                "Find it",
                matched = false,
                bestSimilarity = 0.2,
                foundCount = 0,
                totalCount = 2,
                nextThingId = nextThingId,
            ),
        )
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(port, outputs::add).create(this, testGameId, testThingId)

        interactor.dispatch(PlayGameIntent.Load)
        advanceUntilIdle()

        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        assertTrue(interactor.state.value.busy)
        advanceUntilIdle()

        assertIs<PlayFeedback.Matched>(interactor.state.value.feedback)
        assertEquals(1, port.guesses)

        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        advanceUntilIdle()
        assertEquals(1, port.guesses)

        interactor.dispatch(PlayGameIntent.Continue)
        assertEquals(
            listOf<PlayGameOutput>(PlayGameOutput.NextRequested(testGameId, nextThingId)),
            outputs,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mismatch_allows_retry_and_completion_returns_to_game_detail() = runTest {
        val port = FakePlayPort(
            content = PlayGameContent(
                "Trip",
                "Find it",
                matched = false,
                bestSimilarity = null,
                foundCount = 0,
                totalCount = 1,
                nextThingId = null,
            ),
            outcomes = ArrayDeque(
                listOf(
                    testGuessOutcome(similarity = 0.4, matched = false),
                    testGuessOutcome(similarity = 0.9, matched = true),
                ),
            ),
        )
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(port, outputs::add).create(this, testGameId, testThingId)

        interactor.dispatch(PlayGameIntent.Load)
        advanceUntilIdle()

        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        advanceUntilIdle()
        assertIs<PlayFeedback.Mismatch>(interactor.state.value.feedback)

        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        advanceUntilIdle()
        val matched = assertIs<PlayFeedback.Matched>(interactor.state.value.feedback)
        assertTrue(matched.completed)
        assertEquals(2, port.guesses)

        interactor.dispatch(PlayGameIntent.Continue)
        assertEquals(listOf<PlayGameOutput>(PlayGameOutput.Closed(testGameId)), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun restored_matched_clue_can_continue_without_repeating_guess() = runTest {
        val port = FakePlayPort(
            content = PlayGameContent(
                "Trip",
                "Find it",
                matched = true,
                bestSimilarity = 0.91,
                foundCount = 1,
                totalCount = 2,
                nextThingId = nextThingId,
            ),
        )
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(port, outputs::add).create(this, testGameId, testThingId)

        interactor.dispatch(PlayGameIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        interactor.dispatch(PlayGameIntent.Continue)

        assertEquals(0, port.guesses)
        assertEquals(
            listOf<PlayGameOutput>(PlayGameOutput.NextRequested(testGameId, nextThingId)),
            outputs,
        )
    }
}

private class FakePlayPort(
    val content: PlayGameContent = PlayGameContent("Trip", "Find it", matched = false, bestSimilarity = 0.2),
    private val outcomes: ArrayDeque<GuessOutcome> = ArrayDeque(listOf(testGuessOutcome())),
) : PlayGamePort {
    var loads = 0
        private set
    var guesses = 0
        private set

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
        return LocalGameResult.Success(outcomes.removeFirst())
    }
}
