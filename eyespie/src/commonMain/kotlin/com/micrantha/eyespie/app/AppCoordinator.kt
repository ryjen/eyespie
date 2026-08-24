package com.micrantha.eyespie.app

import com.micrantha.eyespie.features.clueauthoring.ClueAuthoringOutput
import com.micrantha.eyespie.features.create.CreateGameOutput
import com.micrantha.eyespie.features.gamedetail.GameDetailOutput
import com.micrantha.eyespie.features.home.HomeOutput
import com.micrantha.eyespie.features.onboarding.OnboardingOutput
import com.micrantha.eyespie.features.play.PlayGameOutput
import com.micrantha.eyespie.features.utility.UtilityOutput

class AppCoordinator(
    private val navigation: AppNavigation,
) {
    fun onHomeOutput(output: HomeOutput) {
        navigation.push(
            when (output) {
                HomeOutput.OnboardingRequested -> AppRoute.Onboarding
                HomeOutput.UtilityRequested -> AppRoute.Utility
                HomeOutput.CreateRequested -> AppRoute.Create
                is HomeOutput.GameRequested -> AppRoute.GameDetail(output.gameId)
            },
        )
    }

    fun onOnboardingOutput(output: OnboardingOutput) {
        when (output) {
            OnboardingOutput.Completed,
            OnboardingOutput.Dismissed -> navigation.replaceAll(AppRoute.Home)
        }
    }

    fun onUtilityOutput(output: UtilityOutput) {
        when (output) {
            UtilityOutput.Closed -> navigation.pop()
            UtilityOutput.OnboardingRequested -> navigation.push(AppRoute.Onboarding)
        }
    }

    fun onCreateGameOutput(output: CreateGameOutput) {
        when (output) {
            CreateGameOutput.Created,
            CreateGameOutput.Cancelled -> navigation.pop()
        }
    }

    fun onGameDetailOutput(output: GameDetailOutput) {
        when (output) {
            GameDetailOutput.Closed -> navigation.pop()
            is GameDetailOutput.AddClueRequested -> navigation.push(AppRoute.ClueAuthoring(output.gameId))
            is GameDetailOutput.PlayRequested -> navigation.push(AppRoute.Play(output.gameId, output.thingId))
        }
    }

    fun onClueAuthoringOutput(output: ClueAuthoringOutput) {
        when (output) {
            is ClueAuthoringOutput.Closed,
            is ClueAuthoringOutput.Completed -> navigation.pop()
        }
    }

    fun onPlayGameOutput(output: PlayGameOutput) {
        when (output) {
            is PlayGameOutput.Closed -> navigation.pop()
            is PlayGameOutput.Advance -> navigation.replace(AppRoute.Play(output.gameId, output.thingId))
        }
    }
}
