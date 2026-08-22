package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameOutput
import com.micrantha.eyespie.features.gamedetail.GameDetailOutput
import com.micrantha.eyespie.features.home.HomeOutput
import com.micrantha.eyespie.features.onboarding.OnboardingOutput
import com.micrantha.eyespie.features.play.PlayGameOutput
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals

class AppCoordinatorTest {
    @Test
    fun product_outputs_map_to_product_routes() {
        val navigator = StateFlowAppNavigator()
        val coordinator = AppCoordinator(navigator)

        coordinator.onHomeOutput(HomeOutput.CreateRequested)
        assertEquals(AppRoute.Create, navigator.route.value)

        coordinator.onHomeOutput(HomeOutput.GameRequested(testGameId))
        assertEquals(AppRoute.GameDetail(testGameId), navigator.route.value)

        coordinator.onGameDetailOutput(GameDetailOutput.PlayRequested(testGameId, testThingId))
        assertEquals(AppRoute.Play(testGameId, testThingId), navigator.route.value)

        coordinator.onPlayGameOutput(PlayGameOutput.Advance(testGameId, testThingId))
        assertEquals(AppRoute.Play(testGameId, testThingId), navigator.route.value)
    }

    @Test
    fun terminal_outputs_return_to_the_owning_surface() {
        val navigator = StateFlowAppNavigator(AppRoute.Create)
        val coordinator = AppCoordinator(navigator)

        coordinator.onCreateGameOutput(CreateGameOutput.Cancelled)
        assertEquals(AppRoute.Home, navigator.route.value)

        navigator.navigate(AppRoute.Onboarding)
        coordinator.onOnboardingOutput(OnboardingOutput.Completed)
        assertEquals(AppRoute.Home, navigator.route.value)

        navigator.navigate(AppRoute.GameDetail(testGameId))
        coordinator.onGameDetailOutput(GameDetailOutput.Closed)
        assertEquals(AppRoute.Home, navigator.route.value)

        navigator.navigate(AppRoute.Play(testGameId, testThingId))
        coordinator.onPlayGameOutput(PlayGameOutput.Closed(testGameId))
        assertEquals(AppRoute.GameDetail(testGameId), navigator.route.value)
    }
}
