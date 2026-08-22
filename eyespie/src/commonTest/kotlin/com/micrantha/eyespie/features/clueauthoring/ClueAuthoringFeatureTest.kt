package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.AuthoredThing
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class ClueAuthoringFeatureTest {
    private val gameId = GameId("game-1")

    @Test
    fun reducer_locks_editing_while_capture_is_in_progress() {
        val editing = ClueAuthoringState(clue = "Find stripes", expectedAnswer = "crosswalk")
        val busy = ClueAuthoringReducer.reduce(editing, ClueAuthoringIntent.TargetCaptured(image()))

        assertTrue(busy.busy)
        assertEquals(
            busy,
            ClueAuthoringReducer.reduce(busy, ClueAuthoringIntent.ClueChanged("changed")),
        )
        assertEquals(
            busy,
            ClueAuthoringReducer.reduce(busy, ClueAuthoringIntent.ExpectedAnswerChanged("changed")),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun capture_uses_current_fields_and_completes_after_port_success() = runTest {
        val port = RecordingClueAuthoringPort(LocalGameResult.Success(authoredThing()))
        val outputs = mutableListOf<ClueAuthoringOutput>()
        val interactor = ClueAuthoringInteractor(port, this, gameId, outputs::add)

        interactor.dispatch(ClueAuthoringIntent.ClueChanged("Find stripes"))
        interactor.dispatch(ClueAuthoringIntent.ExpectedAnswerChanged("crosswalk"))
        interactor.dispatch(ClueAuthoringIntent.TargetCaptured(image()))

        assertTrue(interactor.state.value.busy)
        assertTrue(outputs.isEmpty())

        advanceUntilIdle()

        assertEquals(gameId, port.gameId)
        assertEquals("Find stripes", port.clueText)
        assertEquals("crosswalk", port.expectedAnswer)
        assertEquals(
            listOf<ClueAuthoringOutput>(ClueAuthoringOutput.Completed(gameId)),
            outputs,
        )
        assertEquals(ClueAuthoringState(), interactor.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun port_failure_remains_on_editor_and_is_retryable() = runTest {
        val failure = LocalGameFailure(LocalGameFailureCode.NOT_LOCAL_CREATOR)
        val port = RecordingClueAuthoringPort(LocalGameResult.Failure(failure))
        val outputs = mutableListOf<ClueAuthoringOutput>()
        val interactor = ClueAuthoringInteractor(port, this, gameId, outputs::add)

        interactor.dispatch(ClueAuthoringIntent.ClueChanged("Find stripes"))
        interactor.dispatch(ClueAuthoringIntent.ExpectedAnswerChanged("crosswalk"))
        interactor.dispatch(ClueAuthoringIntent.TargetCaptured(image()))
        advanceUntilIdle()

        assertFalse(interactor.state.value.busy)
        assertEquals(ClueAuthoringFailure.Game(failure), interactor.state.value.failure)
        assertEquals("Find stripes", interactor.state.value.clue)
        assertEquals("crosswalk", interactor.state.value.expectedAnswer)
        assertTrue(outputs.isEmpty())
    }

    @Test
    fun back_emits_semantic_close_for_current_game() = runTest {
        val outputs = mutableListOf<ClueAuthoringOutput>()
        val interactor = ClueAuthoringInteractor(
            RecordingClueAuthoringPort(LocalGameResult.Success(authoredThing())),
            this,
            gameId,
            outputs::add,
        )

        interactor.dispatch(ClueAuthoringIntent.Back)

        assertEquals(
            listOf<ClueAuthoringOutput>(ClueAuthoringOutput.Closed(gameId)),
            outputs,
        )
    }

    private fun image(): CapturedImage = CapturedImage.fromEncoded(byteArrayOf(1))

    private fun authoredThing(): AuthoredThing {
        val authority = when (val result = ClueAuthority.manual("Find stripes", "crosswalk")) {
            is ClueAuthoringResult.Accepted -> result.authority
            is ClueAuthoringResult.Rejected -> error("expected valid clue fixture")
        }
        return AuthoredThing(gameId, ThingId("thing-2"), authority.playable())
    }
}

private class RecordingClueAuthoringPort(
    private val result: LocalGameResult<AuthoredThing>,
) : ClueAuthoringPort {
    var gameId: GameId? = null
        private set
    var clueText: String? = null
        private set
    var expectedAnswer: String? = null
        private set

    override suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<AuthoredThing> {
        this.gameId = gameId
        this.clueText = clueText
        this.expectedAnswer = expectedAnswer
        return result
    }
}
