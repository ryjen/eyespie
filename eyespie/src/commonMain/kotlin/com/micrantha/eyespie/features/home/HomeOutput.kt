package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.GameId

sealed interface HomeOutput {
    data object OnboardingRequested : HomeOutput
    data object UtilityRequested : HomeOutput
    data object CreateRequested : HomeOutput
    data class GameRequested(val gameId: GameId) : HomeOutput
}
