package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.home.HomeIntent
import com.micrantha.eyespie.features.onboarding.OnboardingIntent
import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class AppGraphFactoryTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun from_ports_binds_feature_factories_to_one_coordinator() = runTest {
        val navigator = StateFlowAppNavigator()
        val onboardingPreferences = TestOnboardingPreferences()
        val graph = AppGraphFactory.fromPorts(
            homePort = AppTestPorts,
            createGamePort = AppTestPorts,
            playGamePort = AppTestPorts,
            onboardingPreferences = onboardingPreferences,
            navigator = navigator,
        )

        graph.homeFactory.create(this).dispatch(HomeIntent.OnboardingSelected)
        assertEquals(AppRoute.Onboarding, navigator.route.value)

        graph.onboardingFactory.create(this).dispatch(OnboardingIntent.Done)
        advanceUntilIdle()

        assertEquals(true, onboardingPreferences.completed)
        assertEquals(AppRoute.Home, navigator.route.value)
    }
}

private class TestOnboardingPreferences : OnboardingPreferenceStore {
    var completed = false

    override suspend fun isCompleted(): Boolean = completed

    override suspend fun markCompleted() {
        completed = true
    }
}
