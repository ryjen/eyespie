package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.onboarding.OnboardingIntent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class AppGraphFactoryTest {
    @Test
    fun from_ports_binds_feature_factories_to_one_coordinator() = runTest {
        val navigator = StateFlowAppNavigator()
        val graph = AppGraphFactory.fromPorts(
            homePort = AppTestPorts,
            createGamePort = AppTestPorts,
            playGamePort = AppTestPorts,
            navigator = navigator,
        )

        graph.homeFactory.create(this).dispatch(HomeIntent.OnboardingSelected)
        assertEquals(AppRoute.Onboarding, navigator.route.value)

        graph.onboardingFactory.create().dispatch(OnboardingIntent.Done)
        assertEquals(AppRoute.Home, navigator.route.value)
    }
}
