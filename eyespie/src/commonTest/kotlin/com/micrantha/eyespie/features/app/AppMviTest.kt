package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.clue.ClueAuthority
import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.MatchResult
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.core.ThingProgress
import com.micrantha.eyespie.features.create.CreateGameIntent
import com.micrantha.eyespie.features.create.CreateGameReducer
import com.micrantha.eyespie.features.create.CreateGameState
import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.home.HomeInteractor
import com.micrantha.eyespie.features.home.HomeReducer
import com.micrantha.eyespie.features.home.HomeState
import com.micrantha.eyespie.features.onboarding.OnboardingIntent
import com.micrantha.eyespie.features.onboarding.OnboardingPage
import com.micrantha.eyespie.features.onboarding.OnboardingReducer
import com.micrantha.eyespie.features.onboarding.OnboardingState
import com.micrantha.eyespie.features.play.PlayGameIntent
import com.micrantha.eyespie.features.play.PlayGameReducer
import com.micrantha.eyespie.features.play.PlayGameState
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.game.LocalGameSummary
import com.micrantha.eyespie.game.PlayableThingSummary
import com.micrantha.eyespie.imaging.CapturedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class AppMviTest {
    @Test
    fun create_reducer_keeps_form_state_immutable() {
        val initial = CreateGameState()
        val named = CreateGameReducer.reduce(initial, CreateGameIntent.NameChanged("Trip"))
        val clued = CreateGameReducer.reduce(named, CreateGameIntent.ClueChanged("Find the red door"))

        assertEquals("", initial.name)
        assertEquals("Trip", named.name)
        assertEquals("Find the red door", clued.clue)
    }

    @Test
    fun create_back_resets_retained_feature_state() {
        val edited = CreateGameState(name = "Trip", clue = "Find it", expectedAnswer = "it")
        assertEquals(CreateGameState(), CreateGameReducer.reduce(edited, CreateGameIntent.Back))
    }

    @Test
    fun onboarding_reducer_moves_between_local_first_pages() {
        val welcome = OnboardingState()
        val create = OnboardingReducer.reduce(welcome, OnboardingIntent.Next)
        val play = OnboardingReducer.reduce(create, OnboardingIntent.Next)
        val back = OnboardingReducer.reduce(play, OnboardingIntent.Previous)

        assertEquals(OnboardingPage.Welcome, welcome.page)
        assertEquals(OnboardingPage.Create, create.page)
        assertEquals(OnboardingPage.Play, play.page)
        assertEquals(OnboardingPage.Create, back.page)
    }

    @Test
    fun home_reducer_adopts_external_snapshot_without_side_effects() {
        val snapshot = snapshot()
        val next = HomeReducer.reduce(HomeState(), HomeIntent.AdoptSnapshot(snapshot))

        assertEquals(snapshot, next.snapshot)
        assertFalse(next.loading)
    }

    @Test
    fun home_reducer_rejects_refresh_completion_older_than_adopted_snapshot() {
        val stale = snapshot("Stale")
        val adopted = snapshot("Current")
        val refreshing = HomeReducer.reduce(HomeState(snapshot = stale), HomeIntent.Refresh)
        val generation = refreshing.refreshGeneration
        val current = HomeReducer.reduce(refreshing, HomeIntent.AdoptSnapshot(adopted))
        val completedLate = HomeReducer.reduce(
            current,
            HomeIntent.SnapshotLoaded(generation, stale),
        )

        assertEquals(adopted, completedLate.snapshot)
        assertEquals(current.refreshGeneration, completedLate.refreshGeneration)
    }

    @Test
    fun play_reducer_preserves_successful_guess_when_refresh_fails() {
        val gameId = GameId("game-1")
        val thingId = ThingId("thing-1")
        val thing = playableThing(gameId, thingId)
        val game = LocalGameSummary(gameId, "Trip", listOf(thing))
        val outcome = GuessOutcome(
            gameId = gameId,
            thingId = thingId,
            clue = thing.clue,
            match = MatchResult(similarity = 0.9, matched = true),
            progress = thing.progress!!,
        )
        val failure = LocalGameFailure(LocalGameFailureCode.PERSISTENCE_FAILED)

        val next = PlayGameReducer.reduce(
            PlayGameState(game = game, thing = thing, busy = true),
            PlayGameIntent.GuessCompletedWithRefreshFailure(outcome, failure),
        )

        assertEquals(outcome, next.latestOutcome)
        assertFalse(next.busy)
        assertIs<AppFailure.Game>(next.failure)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun home_interactor_reduces_before_inducing_refresh() = runTest {
        val snapshot = snapshot()
        val useCases = FakeAppGameUseCases(snapshot)
        val interactor = HomeInteractor(
            useCases = useCases,
            scope = this,
            navigate = {},
            initialState = HomeState(loading = false),
        )

        interactor.dispatch(HomeIntent.Refresh)
        assertTrue(interactor.state.value.loading)
        assertEquals(1L, interactor.state.value.refreshGeneration)

        advanceUntilIdle()

        assertFalse(interactor.state.value.loading)
        assertEquals(snapshot, interactor.state.value.snapshot)
        assertEquals(1, useCases.loadCount)
    }

    private fun snapshot(displayName: String = "Agent") = LocalGameSnapshot(
        identity = PlayerIdentity(PlayerId("player-1"), displayName),
        games = emptyList(),
    )

    private fun playableThing(gameId: GameId, thingId: ThingId): PlayableThingSummary {
        val playerId = PlayerId("player-1")
        return PlayableThingSummary(
            id = thingId,
            clue = ClueAuthority.manual("Find it", "it").let { authored ->
                when (authored) {
                    is com.micrantha.eyespie.clue.ClueAuthoringResult.Accepted -> authored.authority.playable()
                    is com.micrantha.eyespie.clue.ClueAuthoringResult.Rejected -> error("fixture clue rejected")
                }
            },
            progress = ThingProgress(
                gameId = gameId,
                thingId = thingId,
                playerId = playerId,
                matched = true,
                bestSimilarity = 0.9,
            ),
        )
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
