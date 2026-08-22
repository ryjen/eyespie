package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

sealed interface HomeOutput {
    data object OnboardingRequested : HomeOutput
    data object CreateRequested : HomeOutput
    data class PlayRequested(val gameId: GameId, val thingId: ThingId) : HomeOutput
}
