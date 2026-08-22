package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.create.CreateGamePort
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.gamedetail.GameDetailPort
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.home.HomePort
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.features.play.PlayGamePort
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.sharing.GameDocumentTransfer

object AppGraphFactory {
    fun fromRuntime(
        runtime: EyespieRuntime,
        navigator: AppNavigator = StateFlowAppNavigator(),
        documentTransfer: GameDocumentTransfer? = null,
    ): AppGraph {
        val adapter = LocalGameAdapter(runtime, documentTransfer)
        return fromPorts(
            homePort = adapter,
            createGamePort = adapter,
            playGamePort = adapter,
            onboardingPreferences = runtime.onboardingPreferences,
            navigator = navigator,
            gameDetailPort = adapter,
        )
    }

    fun fromPorts(
        homePort: HomePort,
        createGamePort: CreateGamePort,
        playGamePort: PlayGamePort,
        onboardingPreferences: OnboardingPreferenceStore,
        navigator: AppNavigator = StateFlowAppNavigator(),
        gameDetailPort: GameDetailPort = HomeBackedGameDetailPort(homePort),
    ): AppGraph {
        val coordinator = AppCoordinator(navigator)
        return AppGraph(
            navigator = navigator,
            homeFactory = HomeFactory(homePort, coordinator::onHomeOutput),
            onboardingFactory = OnboardingFactory(
                onboardingPreferences,
                coordinator::onOnboardingOutput,
            ),
            createGameFactory = CreateGameFactory(createGamePort, coordinator::onCreateGameOutput),
            gameDetailFactory = GameDetailFactory(
                gameDetailPort,
                coordinator::onGameDetailOutput,
            ),
            playGameFactory = PlayGameFactory(playGamePort, coordinator::onPlayGameOutput),
        )
    }
}
