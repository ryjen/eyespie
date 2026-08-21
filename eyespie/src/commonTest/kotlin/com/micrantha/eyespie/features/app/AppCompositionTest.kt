package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.create.CreateGamePort
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.home.HomePort
import com.micrantha.eyespie.features.onboarding.OnboardingIntent
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGamePort
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class AppCompositionTest {
    @Test
    fun coordinator_translates_feature_outputs_to_app_routes() = runTest {
        val navigator = StateFlowAppNavigator()
        val graph = AppGraphFactory.fromPorts(
            homePort = StubPorts,
            createGamePort = StubPorts,
            playGamePort = StubPorts,
            navigator = navigator,
        )
        val home = graph.homeFactory.create(this)

        home.dispatch(HomeIntent.CreateSelected)
        assertEquals(AppRoute.Create, navigator.route.value)

        home.dispatch(HomeIntent.PlaySelected(testGameId, testThingId))
        assertEquals(AppRoute.Play(testGameId, testThingId), navigator.route.value)
    }

    @Test
    fun graph_factory_binds_feature_factories_without_global_interactors() = runTest {
        val navigator = StateFlowAppNavigator(AppRoute.Onboarding)
        val graph = AppGraphFactory.fromPorts(
            homePort = StubPorts,
            createGamePort = StubPorts,
            playGamePort = StubPorts,
            navigator = navigator,
        )
        val onboarding = graph.onboardingFactory.create()

        onboarding.dispatch(OnboardingIntent.Done)

        assertEquals(AppRoute.Home, navigator.route.value)
    }
}

private object StubPorts : HomePort, CreateGamePort, PlayGamePort {
    override suspend fun load(): LocalGameResult<HomeContent> =
        LocalGameResult.Success(HomeContent("Agent", "player-1", emptyList()))

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = error("not used")

    override suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent> =
        LocalGameResult.Success(PlayGameContent("Trip", "Find it", false, null))

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = error("not used")
}
