package com.micrantha.eyespie.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringFactory
import com.micrantha.eyespie.features.create.CreateGameFactory
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.gamedetail.GameDetailFactory
import com.micrantha.eyespie.features.gamedetail.GameSharer
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeFactory
import com.micrantha.eyespie.features.onboarding.OnboardingFactory
import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.features.play.PlayGameFactory
import com.micrantha.eyespie.features.utility.UtilityFactory
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.sharing.GameDocumentTransfer

object AppGraphFactory {
    fun fromRuntime(
        runtime: EyespieRuntime,
        navigation: AppNavigation,
        documentTransfer: GameDocumentTransfer? = null,
    ): AppGraph {
        val capabilities = LocalGameAdapter(runtime, documentTransfer)
        return fromCapabilities(
            gameSnapshotLoader = runtime.gameLoop,
            gameImportPreparer = capabilities,
            gameImportConfirmer = capabilities,
            gameImportCanceller = capabilities,
            gameCreator = capabilities,
            clueAuthor = capabilities,
            guessSubmitter = capabilities,
            onboardingPreferences = runtime.onboardingPreferences,
            navigation = navigation,
            gameSharer = capabilities,
        )
    }

    fun fromCapabilities(
        gameSnapshotLoader: GameSnapshotLoader,
        gameImportPreparer: GameImportPreparer,
        gameImportConfirmer: GameImportConfirmer,
        gameImportCanceller: GameImportCanceller,
        gameCreator: GameCreator,
        clueAuthor: ClueAuthor,
        guessSubmitter: GuessSubmitter,
        onboardingPreferences: OnboardingPreferenceStore,
        navigation: AppNavigation,
        gameSharer: GameSharer = UnavailableGameSharer,
    ): AppGraph {
        val coordinator = AppCoordinator(navigation)
        return AppGraph(
            homeFactory = HomeFactory(
                snapshotLoader = gameSnapshotLoader,
                importPreparer = gameImportPreparer,
                importConfirmer = gameImportConfirmer,
                importCanceller = gameImportCanceller,
                output = coordinator::onHomeOutput,
            ),
            onboardingFactory = OnboardingFactory(
                onboardingPreferences,
                coordinator::onOnboardingOutput,
            ),
            utilityFactory = UtilityFactory(gameSnapshotLoader, coordinator::onUtilityOutput),
            createGameFactory = CreateGameFactory(gameCreator, coordinator::onCreateGameOutput),
            gameDetailFactory = GameDetailFactory(
                snapshotLoader = gameSnapshotLoader,
                sharer = gameSharer,
                output = coordinator::onGameDetailOutput,
            ),
            clueAuthoringFactory = ClueAuthoringFactory(
                clueAuthor,
                coordinator::onClueAuthoringOutput,
            ),
            playGameFactory = PlayGameFactory(
                snapshotLoader = gameSnapshotLoader,
                guessSubmitter = guessSubmitter,
                output = coordinator::onPlayGameOutput,
            ),
        )
    }
}
