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
    fun from_capabilities_binds_feature_factories_to_one_coordinator() = runTest {
        val navigation = TestNavigation()
        val onboardingPreferences = TestOnboardingPreferences()
        val graph = AppGraphFactory.fromCapabilities(
            gameSnapshotLoader = AppTestCapabilities,
            gameImportPreparer = AppTestCapabilities,
            gameImportConfirmer = AppTestCapabilities,
            gameImportCanceller = AppTestCapabilities,
            gameCreator = AppTestCapabilities,
            clueAuthor = AppTestCapabilities,
            guessSubmitter = AppTestCapabilities,
            onboardingPreferences = onboardingPreferences,
            navigation = navigation,
        )

        graph.homeFactory.create(this).dispatch(HomeIntent.OnboardingSelected)
        assertEquals(AppRoute.Onboarding, navigation.pushed.single())

        graph.onboardingFactory.create(this).dispatch(OnboardingIntent.Done)
        advanceUntilIdle()

        assertEquals(true, onboardingPreferences.completed)
        assertEquals(AppRoute.Home, navigation.replacedAll.single())
    }
}

private class TestNavigation : AppNavigation {
    val pushed = mutableListOf<AppRoute>()
    val replacedAll = mutableListOf<AppRoute>()

    override fun push(route: AppRoute) {
        pushed += route
    }

    override fun replace(route: AppRoute) = Unit

    override fun replaceAll(route: AppRoute) {
        replacedAll += route
    }

    override fun pop() = Unit
}

private class TestOnboardingPreferences : OnboardingPreferenceStore {
    var completed = false

    override suspend fun isCompleted(): Boolean = completed

    override suspend fun markCompleted() {
        completed = true
    }
}
