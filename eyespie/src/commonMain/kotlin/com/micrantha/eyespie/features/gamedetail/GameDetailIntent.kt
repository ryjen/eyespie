package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.LocalGameFailure

sealed interface GameDetailIntent {
    data object Load : GameDetailIntent
    data object Back : GameDetailIntent
    data object DismissFailure : GameDetailIntent
    data class PlaySelected(val thingId: ThingId) : GameDetailIntent
    data class ContentLoaded(val generation: Long, val content: GameDetailContent) : GameDetailIntent
    data class OperationFailed(val generation: Long, val failure: LocalGameFailure) : GameDetailIntent
}
