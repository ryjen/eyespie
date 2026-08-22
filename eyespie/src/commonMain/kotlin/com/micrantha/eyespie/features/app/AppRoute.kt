package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId

/**
 * Implemented application destinations. The broader product route map follows
 * docs/design/eyespie-app-mockups; transient result variants remain feature state.
 */
sealed interface AppRoute {
    data object Home : AppRoute
    data object Onboarding : AppRoute
    data object Utility : AppRoute
    data object Create : AppRoute
    data class GameDetail(val gameId: GameId) : AppRoute
    data class ClueAuthoring(val gameId: GameId) : AppRoute
    data class Play(val gameId: GameId, val thingId: ThingId) : AppRoute
}
