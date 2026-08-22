package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.create.CreateGamePort
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.home.HomePort
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.features.play.PlayGamePort
import com.micrantha.eyespie.game.EyespieRuntime

object AppGraphFactory {
    fun fromRuntime(
        runtime: EyespieRuntime,
        navigator: AppNavigator = StateFlowAppNavigator(),
    ): AppGraph {
        val adapter = LocalGameAdapter(runtime)
        return fromPorts(
            homePort = adapter,
            createGamePort = adapter,
            playGamePort = adapter,
            navigator = navigator,
        )
    }

    fun fromPorts(
        homePort: HomePort,
        createGamePort: CreateGamePort,
        playGamePort: PlayGamePort,
        navigator: AppNavigator = StateFlowAppNavigator(),
    ): AppGraph {
        val coordinator = AppCoordinator(navigator)
        return AppGraph(
            navigator = navigator,
            homeFactory = HomeFactory(homePort, coordinator::onHomeOutput),
            onboardingFactory = OnboardingFactory(coordinator::onOnboardingOutput),
            createGameFactory = CreateGameFactory(createGamePort, coordinator::onCreateGameOutput),
            gameDetailFactory = GameDetailFactory(
                HomeBackedGameDetailPort(homePort),
                coordinator::onGameDetailOutput,
            ),
            playGameFactory = PlayGameFactory(playGamePort, coordinator::onPlayGameOutput),
        )
    }
}
