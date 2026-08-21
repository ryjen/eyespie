package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameOutput
import com.micrantha.eyespie.features.home.HomeOutput
import com.micrantha.eyespie.features.onboarding.OnboardingOutput
import com.micrantha.eyespie.features.play.PlayGameOutput
import com.micrantha.eyespie.testsupport.testGameId
import com.micrantha.eyespie.testsupport.testThingId
import kotlin.test.Test
import kotlin.test.assertEquals

class AppCoordinatorTest {
    @Test
    fun home_outputs_map_to_product_routes() {
        val navigator = StateFlowAppNavigator()
        val coordinator = AppCoordinator(navigator)

        coordinator.onHomeOutput(HomeOutput.CreateRequested)
        assertEquals(AppRoute.Create, navigator.route.value)

        coordinator.onHomeOutput(HomeOutput.PlayRequested(testGameId, testThingId))
        assertEquals(AppRoute.Play(testGameId, testThingId), navigator.route.value)
    }

    @Test
    fun terminal_feature_outputs_return_home() {
        val navigator = StateFlowAppNavigator(AppRoute.Create)
        val coordinator = AppCoordinator(navigator)

        coordinator.onCreateGameOutput(CreateGameOutput.Cancelled)
        assertEquals(AppRoute.Home, navigator.route.value)

        navigator.navigate(AppRoute.Onboarding)
        coordinator.onOnboardingOutput(OnboardingOutput.Completed)
        assertEquals(AppRoute.Home, navigator.route.value)

        navigator.navigate(AppRoute.Play(testGameId, testThingId))
        coordinator.onPlayGameOutput(PlayGameOutput.Closed)
        assertEquals(AppRoute.Home, navigator.route.value)
    }
}
