package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.game.EyespieRuntime

object AppGraphFactory {
    fun fromRuntime(
        runtime: EyespieRuntime,
        navigator: AppNavigator = StateFlowAppNavigator(),
    ): AppGraph {
        val adapter = LocalGameAdapter(runtime)
        val coordinator = AppCoordinator(navigator)
        return AppGraph(
            navigator = navigator,
            homeFactory = HomeFactory(adapter, coordinator::onHomeOutput),
            onboardingFactory = OnboardingFactory(coordinator::onOnboardingOutput),
            createGameFactory = CreateGameFactory(adapter, coordinator::onCreateGameOutput),
            playGameFactory = PlayGameFactory(adapter, coordinator::onPlayGameOutput),
        )
    }
}
