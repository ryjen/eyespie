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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * Feature test triangle:
 * 1. reducer: deterministic state transition;
 * 2. interactor: resolved from AppGraph so production DI wiring is exercised;
 * 3. app wiring/navigation: feature-to-feature routing and graph propagation.
 */
class AppMviTest {
    // Home triangle
    @Test
    fun home_reducer_adopts_external_snapshot_without_side_effects() {
        val snapshot = snapshot()
        val next = HomeReducer.reduce(HomeState(), HomeIntent.AdoptSnapshot(snapshot))
        assertEquals(snapshot, next.snapshot)
        assertFalse(next.loading)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun home_interactor_is_resolved_through_di_and_loads_snapshot() = runTest {
        val expected = snapshot()
        val useCases = FakeAppGameUseCases(expected)
        val graph = AppGraph(useCases, this)

        graph.start()
        assertTrue(graph.homeInteractor.state.value.loading)
        advanceUntilIdle()

        assertEquals(expected, graph.homeInteractor.state.value.snapshot)
        assertEquals(1, useCases.loadCount)
    }

    @Test
    fun home_navigation_is_owned_by_app_graph() = runTest {
        val graph = AppGraph(FakeAppGameUseCases(snapshot()), this)
        graph.homeInteractor.dispatch(HomeIntent.CreateSelected)
        assertEquals(AppScreen.Create, graph.navigator.screen.value)
    }

    // Onboarding triangle
    @Test
    fun onboarding_reducer_moves_between_local_first_pages() {
        val create = OnboardingReducer.reduce(OnboardingState(), OnboardingIntent.Next)
        val play = OnboardingReducer.reduce(create, OnboardingIntent.Next)
        assertEquals(OnboardingPage.Create, create.page)
        assertEquals(OnboardingPage.Play, play.page)
    }

    @Test
    fun onboarding_interactor_is_resolved_through_di() = runTest {
        val graph = AppGraph(FakeAppGameUseCases(snapshot()), this)
        graph.onboardingInteractor.dispatch(OnboardingIntent.Next)
        assertEquals(OnboardingPage.Create, graph.onboardingInteractor.state.value.page)
    }

    @Test
    fun onboarding_navigation_round_trip_is_wired_by_app_graph() = runTest {
        val graph = AppGraph(FakeAppGameUseCases(snapshot()), this)
        graph.homeInteractor.dispatch(HomeIntent.OnboardingSelected)
        assertEquals(AppScreen.Onboarding, graph.navigator.screen.value)

        graph.onboardingInteractor.dispatch(OnboardingIntent.Done)
        assertEquals(AppScreen.Home, graph.navigator.screen.value)
        assertEquals(OnboardingState(), graph.onboardingInteractor.state.value)
    }

    // Create triangle
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
    fun create_interactor_is_resolved_through_di() = runTest {
        val graph = AppGraph(FakeAppGameUseCases(snapshot()), this)
        graph.createGameInteractor.dispatch(CreateGameIntent.NameChanged("Trip"))
        assertEquals("Trip", graph.createGameInteractor.state.value.name)
    }

    @Test
    fun create_back_navigation_and_retained_state_reset_are_app_wired() = runTest {
        val graph = AppGraph(FakeAppGameUseCases(snapshot()), this)
        graph.navigator.navigate(AppScreen.Create)
        graph.createGameInteractor.dispatch(CreateGameIntent.NameChanged("Trip"))
        graph.createGameInteractor.dispatch(CreateGameIntent.Back)

        assertEquals(AppScreen.Home, graph.navigator.screen.value)
        assertEquals(CreateGameState(), graph.createGameInteractor.state.value)
    }

    // Play triangle
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

    @Test
    fun play_interactor_is_created_through_di_from_home_authoritative_snapshot() = runTest {
        val gameId = GameId("game-1")
        val thingId = ThingId("thing-1")
        val gameSnapshot = snapshotWithGame(gameId, thingId)
        val graph = AppGraph(FakeAppGameUseCases(gameSnapshot), this)
        graph.homeInteractor.dispatch(HomeIntent.AdoptSnapshot(gameSnapshot))

        val interactor = graph.playGameInteractor(AppScreen.Play(gameId, thingId))
        assertNotNull(interactor)
        assertEquals(gameId, interactor.state.value.game.id)
        assertEquals(thingId, interactor.state.value.thing.id)
    }

    @Test
    fun play_back_navigation_is_app_wired() = runTest {
        val gameId = GameId("game-1")
        val thingId = ThingId("thing-1")
        val gameSnapshot = snapshotWithGame(gameId, thingId)
        val graph = AppGraph(FakeAppGameUseCases(gameSnapshot), this)
        graph.homeInteractor.dispatch(HomeIntent.AdoptSnapshot(gameSnapshot))
        val screen = AppScreen.Play(gameId, thingId)
        graph.navigator.navigate(screen)

        val interactor = assertNotNull(graph.playGameInteractor(screen))
        interactor.dispatch(PlayGameIntent.Back)
        assertEquals(AppScreen.Home, graph.navigator.screen.value)
    }

    @Test
    fun home_reducer_rejects_refresh_completion_older_than_adopted_snapshot() {
        val stale = snapshot("Stale")
        val adopted = snapshot("Current")
        val refreshing = HomeReducer.reduce(HomeState(snapshot = stale), HomeIntent.Refresh)
        val generation = refreshing.refreshGeneration
        val current = HomeReducer.reduce(refreshing, HomeIntent.AdoptSnapshot(adopted))
        val completedLate = HomeReducer.reduce(current, HomeIntent.SnapshotLoaded(generation, stale))
        assertEquals(adopted, completedLate.snapshot)
    }

    private fun snapshot(displayName: String = "Agent") = LocalGameSnapshot(
        identity = PlayerIdentity(PlayerId("player-1"), displayName),
        games = emptyList(),
    )

    private fun snapshotWithGame(gameId: GameId, thingId: ThingId): LocalGameSnapshot {
        val thing = playableThing(gameId, thingId)
        return LocalGameSnapshot(
            identity = PlayerIdentity(PlayerId("player-1"), "Agent"),
            games = listOf(LocalGameSummary(gameId, "Trip", listOf(thing))),
        )
    }

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
