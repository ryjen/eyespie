package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.features.create.CreateGameOutput
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
                is HomeOutput.PlayRequested -> AppRoute.Play(output.gameId, output.thingId)
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
            CreateGameOutput.Created,
            CreateGameOutput.Cancelled -> navigator.navigate(AppRoute.Home)
        }
    }

    fun onPlayGameOutput(output: PlayGameOutput) {
        when (output) {
            PlayGameOutput.Closed -> navigator.navigate(AppRoute.Home)
        }
    }
}
