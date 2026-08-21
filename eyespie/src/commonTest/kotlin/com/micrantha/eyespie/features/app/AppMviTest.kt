package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class AppMviTest {
    @Test
    fun reducer_keeps_form_state_immutable_and_typed() {
        val initial = AppState()
        val named = AppReducer.reduce(initial, AppIntent.CreateNameChanged("Trip"))
        val clued = AppReducer.reduce(named, AppIntent.CreateClueChanged("Find the red door"))

        assertEquals("", initial.createForm.name)
        assertEquals("Trip", named.createForm.name)
        assertEquals("Find the red door", clued.createForm.clue)
    }

    @Test
    fun reducer_derives_selected_play_target_from_snapshot() {
        val gameId = GameId("game-1")
        val thingId = ThingId("thing-1")
        val snapshot = LocalGameSnapshot(
            identity = PlayerIdentity(PlayerId("player-1"), "Agent"),
            games = emptyList(),
        )
        val state = AppState(snapshot = snapshot, loading = false)

        val next = AppReducer.reduce(state, AppIntent.NavigatePlay(gameId, thingId))

        assertIs<AppScreen.Play>(next.screen)
        assertEquals(null, next.playGame)
        assertEquals(null, next.playThing)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun interactor_reduces_before_inducing_refresh() = runTest {
        val snapshot = LocalGameSnapshot(
            identity = PlayerIdentity(PlayerId("player-1"), "Agent"),
            games = emptyList(),
        )
        val useCases = FakeAppGameUseCases(snapshot)
        val interactor = AppInteractor(useCases, backgroundScope)

        interactor.dispatch(AppIntent.Refresh)
        assertEquals(true, interactor.state.value.loading)

        advanceUntilIdle()

        assertFalse(interactor.state.value.loading)
        assertEquals(snapshot, interactor.state.value.snapshot)
        assertEquals(1, useCases.loadCount)
    }
}

private class FakeAppGameUseCases(
    private val snapshot: LocalGameSnapshot,
) : AppGameUseCases {
    var loadCount: Int = 0

    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> {
        loadCount += 1
        return LocalGameResult.Success(snapshot)
    }

    override suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = error("not used")

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = error("not used")
}
