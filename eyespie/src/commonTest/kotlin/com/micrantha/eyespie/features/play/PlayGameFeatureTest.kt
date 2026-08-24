package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testGameSnapshot
import com.micrantha.eyespie.testsupport.testGameSummary
import com.micrantha.eyespie.testsupport.testGuessOutcome
import com.micrantha.eyespie.testsupport.testImage
import com.micrantha.eyespie.testsupport.testPlayableThingSummary
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
            content = PlayGameContent("Trip", "Find it", false, 0.2),
            loading = false,
        )
        val captured = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCaptured(testImage()))
        assertTrue(captured.busy)
        assertEquals(captured, PlayGameReducer.reduce(captured, PlayGameIntent.GuessCompleted(0L, stale)))
        val completed = PlayGameReducer.reduce(captured, PlayGameIntent.GuessCompleted(captured.guessGeneration, current))
        assertFalse(completed.busy)
        assertEquals(current, completed.latestOutcome)
        assertTrue(completed.matched)
    }

    @Test
    fun persisted_match_remains_authoritative_when_current_similarity_misses() {
        val initial = PlayGameState(testGameId, testThingId, PlayGameContent("Trip", "Find it", false, 0.2), loading = false)
        val captured = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCaptured(testImage()))
        val stickyMatch = testGuessOutcome(similarity = 0.4, matched = false).copy(
            progress = testGuessOutcome(similarity = 0.91, matched = true).progress,
        )
        val completed = PlayGameReducer.reduce(captured, PlayGameIntent.GuessCompleted(captured.guessGeneration, stickyMatch))
        assertTrue(completed.matched)
        assertFalse(completed.busy)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_maps_snapshot_then_submits_guess() = runTest {
        val snapshot = testGameSnapshot(
            games = listOf(testGameSummary(things = listOf(testPlayableThingSummary("Find it", bestSimilarity = 0.2)))),
        )
        val capabilities = FakePlayCapabilities(snapshot)
        val interactor = PlayGameFactory(capabilities, capabilities, {}).create(this, testGameId, testThingId)
        interactor.dispatch(PlayGameIntent.Load)
        assertTrue(interactor.state.value.loading)
        advanceUntilIdle()
        assertEquals("Trip", interactor.state.value.content?.gameName)
        assertEquals("Find it", interactor.state.value.content?.clueText)
        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        advanceUntilIdle()
        assertEquals(capabilities.outcome, interactor.state.value.latestOutcome)
        assertEquals(1, capabilities.loads)
        assertEquals(1, capabilities.guesses)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun terminal_match_stays_on_completion_until_back() = runTest {
        val snapshot = testGameSnapshot(games = listOf(testGameSummary(things = listOf(testPlayableThingSummary("Last clue")))))
        val capabilities = FakePlayCapabilities(snapshot)
        val outputs = mutableListOf<PlayGameOutput>()
        val interactor = PlayGameFactory(capabilities, capabilities, outputs::add).create(this, testGameId, testThingId)
        interactor.dispatch(PlayGameIntent.Load)
        advanceUntilIdle()
        interactor.dispatch(PlayGameIntent.GuessCaptured(testImage()))
        advanceUntilIdle()
        assertTrue(interactor.state.value.completed)
        interactor.dispatch(PlayGameIntent.NextClueSelected)
        assertTrue(outputs.isEmpty())
        interactor.dispatch(PlayGameIntent.Back)
        assertEquals(listOf(PlayGameOutput.Closed(testGameId)), outputs)
    }

    @Test
    fun stale_failure_cannot_replace_newer_success() {
        val initial = PlayGameState(testGameId, testThingId, PlayGameContent("Trip", "Find it", false, null), loading = false)
        val firstCapture = PlayGameReducer.reduce(initial, PlayGameIntent.GuessCaptured(testImage()))
        val currentOutcome = testGuessOutcome(matched = true)
        val succeeded = PlayGameReducer.reduce(firstCapture, PlayGameIntent.GuessCompleted(firstCapture.guessGeneration, currentOutcome))
        val staleFailure = PlayGameReducer.reduce(
            succeeded,
            PlayGameIntent.OperationFailed(
                0L,
                com.micrantha.eyespie.game.LocalGameFailure(com.micrantha.eyespie.game.LocalGameFailureCode.GUESS_EMBEDDING_FAILED),
            ),
        )
        assertEquals(succeeded, staleFailure)
        assertNull(staleFailure.failure)
    }
}

private class FakePlayCapabilities(
    private val snapshot: LocalGameSnapshot,
    val outcome: GuessOutcome = testGuessOutcome(),
) : GameSnapshotLoader, GuessSubmitter {
    var loads = 0
    var guesses = 0
    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> {
        loads += 1
        return LocalGameResult.Success(snapshot)
    }
    override suspend fun guess(
        gameId: com.micrantha.eyespie.core.GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> {
        guesses += 1
        return LocalGameResult.Success(outcome)
    }
}
