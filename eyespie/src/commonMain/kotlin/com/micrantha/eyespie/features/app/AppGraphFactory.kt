package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringFactory
import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.gamedetail.GameDetailLoader
import com.micrantha.eyespie.features.gamedetail.GameSharer
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.home.HomeLoader
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.features.play.PlayGameLoader
import com.micrantha.eyespie.features.utility.UtilityFactory
import com.micrantha.eyespie.features.utility.UtilityLoader
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.sharing.GameDocumentTransfer

object AppGraphFactory {
    fun fromRuntime(
        runtime: EyespieRuntime,
        navigator: AppNavigator = StateFlowAppNavigator(),
        documentTransfer: GameDocumentTransfer? = null,
    ): AppGraph {
        val capabilities = LocalGameAdapter(runtime, documentTransfer)
        return fromCapabilities(
            homeLoader = capabilities,
            gameImportPreparer = capabilities,
            gameImportConfirmer = capabilities,
            gameImportCanceller = capabilities,
            utilityLoader = capabilities,
            gameCreator = capabilities,
            clueAuthor = capabilities,
            playGameLoader = capabilities,
            guessSubmitter = capabilities,
            onboardingPreferences = runtime.onboardingPreferences,
            navigator = navigator,
            gameDetailLoader = capabilities,
            gameSharer = capabilities,
        )
    }

    fun fromCapabilities(
        homeLoader: HomeLoader,
        gameImportPreparer: GameImportPreparer,
        gameImportConfirmer: GameImportConfirmer,
        gameImportCanceller: GameImportCanceller,
        utilityLoader: UtilityLoader,
        gameCreator: GameCreator,
        clueAuthor: ClueAuthor,
        playGameLoader: PlayGameLoader,
        guessSubmitter: GuessSubmitter,
        onboardingPreferences: OnboardingPreferenceStore,
        navigator: AppNavigator = StateFlowAppNavigator(),
        gameDetailLoader: GameDetailLoader = HomeBackedGameDetailLoader(homeLoader),
        gameSharer: GameSharer = UnavailableGameSharer,
    ): AppGraph {
        val coordinator = AppCoordinator(navigator)
        return AppGraph(
            navigator = navigator,
            homeFactory = HomeFactory(
                loader = homeLoader,
                importPreparer = gameImportPreparer,
                importConfirmer = gameImportConfirmer,
                importCanceller = gameImportCanceller,
                output = coordinator::onHomeOutput,
            ),
            onboardingFactory = OnboardingFactory(
                onboardingPreferences,
                coordinator::onOnboardingOutput,
            ),
            utilityFactory = UtilityFactory(utilityLoader, coordinator::onUtilityOutput),
            createGameFactory = CreateGameFactory(gameCreator, coordinator::onCreateGameOutput),
            gameDetailFactory = GameDetailFactory(
                loader = gameDetailLoader,
                sharer = gameSharer,
                output = coordinator::onGameDetailOutput,
            ),
            clueAuthoringFactory = ClueAuthoringFactory(
                clueAuthor,
                coordinator::onClueAuthoringOutput,
            ),
            playGameFactory = PlayGameFactory(
                loader = playGameLoader,
                guessSubmitter = guessSubmitter,
                output = coordinator::onPlayGameOutput,
            ),
        )
    }
}
