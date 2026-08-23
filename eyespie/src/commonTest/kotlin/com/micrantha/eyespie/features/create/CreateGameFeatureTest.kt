package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.clue.ClueAuthoringResult
import com.micrantha.eyespie.clue.ClueAuthority
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
    fun factory_injects_game_creator_and_emits_created_output() = runTest {
        val creator = FakeGameCreator()
        val outputs = mutableListOf<CreateGameOutput>()
        val interactor = CreateGameFactory(creator, outputs::add).create(this)
        interactor.dispatch(CreateGameIntent.NameChanged("Trip"))
        interactor.dispatch(CreateGameIntent.ClueChanged("Find it"))
        interactor.dispatch(CreateGameIntent.ExpectedAnswerChanged("it"))

        interactor.dispatch(CreateGameIntent.TargetCaptured(testImage()))
        assertTrue(interactor.state.value.busy)
        advanceUntilIdle()

        assertFalse(interactor.state.value.busy)
        assertEquals(listOf<CreateGameOutput>(CreateGameOutput.Created), outputs)
        assertEquals(1, creator.creates)
        assertEquals(CreateGameState(), interactor.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelling_route_scope_prevents_stale_completion_output() = runTest {
        val result = CompletableDeferred<LocalGameResult<CreatedGame>>()
        val creator = object : GameCreator {
            override suspend fun create(
                name: String,
                clueText: String,
                expectedAnswer: String,
                targetImage: CapturedImage,
            ): LocalGameResult<CreatedGame> = result.await()
        }
        val routeJob = Job()
        val routeScope = CoroutineScope(StandardTestDispatcher(testScheduler) + routeJob)
        val outputs = mutableListOf<CreateGameOutput>()
        val interactor = CreateGameFactory(creator, outputs::add).create(routeScope)
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

private class FakeGameCreator : GameCreator {
    var creates = 0

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> {
        creates += 1
        return LocalGameResult.Success(createdGame(name, clueText, expectedAnswer))
    }
}

private fun createdGame(
    name: String = "Trip",
    clueText: String = "Find it",
    expectedAnswer: String = "it",
): CreatedGame {
    val clue = when (val authored = ClueAuthority.manual(clueText, expectedAnswer)) {
        is ClueAuthoringResult.Accepted -> authored.authority.playable()
        is ClueAuthoringResult.Rejected -> error("fixture clue rejected")
    }
    return CreatedGame(testGameId, testThingId, name, clue)
}
