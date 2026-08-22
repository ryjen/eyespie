package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.CreatedClue
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testImage
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class CreateGameFeatureTest {
    @Test
    fun reducer_keeps_form_state_immutable() {
        val initial = CreateGameState()
        val named = CreateGameReducer.reduce(initial, CreateGameIntent.NameChanged("Trip"))
        val clued = CreateGameReducer.reduce(named, CreateGameIntent.ClueChanged("Find it"))

        assertEquals("", initial.name)
        assertEquals("Trip", named.name)
        assertEquals("Find it", clued.clue)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun factory_injects_create_port_and_emits_created_output() = runTest {
        val port = FakeCreatePort()
        val outputs = mutableListOf<CreateGameOutput>()
        val interactor = CreateGameFactory(port, outputs::add).create(this)
        interactor.dispatch(CreateGameIntent.NameChanged("Trip"))
        interactor.dispatch(CreateGameIntent.ClueChanged("Find it"))
        interactor.dispatch(CreateGameIntent.ExpectedAnswerChanged("it"))

        interactor.dispatch(CreateGameIntent.TargetCaptured(testImage()))
        assertTrue(interactor.state.value.busy)
        advanceUntilIdle()

        assertFalse(interactor.state.value.busy)
        assertEquals(listOf<CreateGameOutput>(CreateGameOutput.Created), outputs)
        assertEquals(1, port.creates)
        assertEquals(CreateGameState(), interactor.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun add_clue_mode_uses_existing_game_and_returns_to_detail() = runTest {
        val port = FakeCreatePort()
        val outputs = mutableListOf<CreateGameOutput>()
        val initial = CreateGameState(mode = CreateGameMode.AddClue(testGameId))
        val interactor = CreateGameFactory(port, outputs::add).create(this, initial)
        interactor.dispatch(CreateGameIntent.ClueChanged("Find the red door"))
        interactor.dispatch(CreateGameIntent.ExpectedAnswerChanged("red door"))

        interactor.dispatch(CreateGameIntent.TargetCaptured(testImage()))
        advanceUntilIdle()

        assertEquals(0, port.creates)
        assertEquals(1, port.cluesAdded)
        assertEquals(listOf<CreateGameOutput>(CreateGameOutput.ClueAdded(testGameId)), outputs)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelling_route_scope_prevents_stale_completion_output() = runTest {
        val result = CompletableDeferred<LocalGameResult<CreatedGame>>()
        val port = object : CreateGamePort {
            override suspend fun create(
                name: String,
                clueText: String,
                expectedAnswer: String,
                targetImage: CapturedImage,
            ): LocalGameResult<CreatedGame> = result.await()

            override suspend fun addClue(
                gameId: GameId,
                clueText: String,
                expectedAnswer: String,
                targetImage: CapturedImage,
            ): LocalGameResult<CreatedClue> = error("not used")
        }
        val routeJob = Job()
        val routeScope = CoroutineScope(StandardTestDispatcher(testScheduler) + routeJob)
        val outputs = mutableListOf<CreateGameOutput>()
        val interactor = CreateGameFactory(port, outputs::add).create(routeScope)
        interactor.dispatch(CreateGameIntent.NameChanged("Trip"))
        interactor.dispatch(CreateGameIntent.ClueChanged("Find it"))
        interactor.dispatch(CreateGameIntent.ExpectedAnswerChanged("it"))
        interactor.dispatch(CreateGameIntent.TargetCaptured(testImage()))
        advanceUntilIdle()

        routeJob.cancel()
        result.complete(LocalGameResult.Success(createdGame()))
        advanceUntilIdle()

        assertTrue(outputs.isEmpty())
    }
}

private class FakeCreatePort : CreateGamePort {
    var creates = 0
    var cluesAdded = 0

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> {
        creates += 1
        return LocalGameResult.Success(createdGame(name, clueText, expectedAnswer))
    }

    override suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedClue> {
        cluesAdded += 1
        return LocalGameResult.Success(createdClue(gameId, clueText, expectedAnswer))
    }
}

private fun createdGame(
    name: String = "Trip",
    clueText: String = "Find it",
    expectedAnswer: String = "it",
): CreatedGame {
    val clue = playableClue(clueText, expectedAnswer)
    return CreatedGame(testGameId, testThingId, name, clue)
}

private fun createdClue(
    gameId: GameId = testGameId,
    clueText: String = "Find it",
    expectedAnswer: String = "it",
): CreatedClue = CreatedClue(
    gameId = gameId,
    thingId = testThingId,
    clue = playableClue(clueText, expectedAnswer),
)

private fun playableClue(clueText: String, expectedAnswer: String) =
    when (val authored = ClueAuthority.manual(clueText, expectedAnswer)) {
        is ClueAuthoringResult.Accepted -> authored.authority.playable()
        is ClueAuthoringResult.Rejected -> error("fixture clue rejected")
    }
