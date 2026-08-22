package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameOutput
import com.micrantha.eyespie.features.gamedetail.GameDetailOutput
import com.micrantha.eyespie.features.home.HomeOutput
import com.micrantha.eyespie.features.onboarding.OnboardingOutput
import com.micrantha.eyespie.features.play.PlayGameOutput

class AppCoordinator(
    private val navigator: AppNavigator,
) {
    fun onHomeOutput(output: HomeOutput) {
        navigator.navigate(
            when (output) {
                HomeOutput.OnboardingRequested -> AppRoute.Onboarding
                HomeOutput.CreateRequested -> AppRoute.Create
                is HomeOutput.GameRequested -> AppRoute.GameDetail(output.gameId)
            },
        )
    }

    fun onOnboardingOutput(output: OnboardingOutput) {
        when (output) {
            OnboardingOutput.Completed,
            OnboardingOutput.Dismissed -> navigator.navigate(AppRoute.Home)
        }
    }

    fun onCreateGameOutput(output: CreateGameOutput) {
        when (output) {
            CreateGameOutput.Created -> navigator.navigate(AppRoute.Home)
            is CreateGameOutput.ClueAdded -> navigator.navigate(AppRoute.GameDetail(output.gameId))
            is CreateGameOutput.Cancelled -> navigator.navigate(
                output.returnGameId?.let(AppRoute::GameDetail) ?: AppRoute.Home,
            )
        }
    }

    fun onGameDetailOutput(output: GameDetailOutput) {
        when (output) {
            GameDetailOutput.Closed -> navigator.navigate(AppRoute.Home)
            is GameDetailOutput.AuthorClueRequested -> navigator.navigate(AppRoute.AuthorClue(output.gameId))
            is GameDetailOutput.PlayRequested -> navigator.navigate(AppRoute.Play(output.gameId, output.thingId))
        }
    }

    fun onPlayGameOutput(output: PlayGameOutput) {
        when (output) {
            is PlayGameOutput.Closed -> navigator.navigate(AppRoute.GameDetail(output.gameId))
        }
    }
}
