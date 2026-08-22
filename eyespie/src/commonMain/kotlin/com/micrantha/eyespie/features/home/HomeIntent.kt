package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.game.LocalGameFailure

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object DismissFailure : HomeIntent
    data object DismissImportResult : HomeIntent
    data object ImportSelected : HomeIntent
    data object OnboardingSelected : HomeIntent
    data object UtilitySelected : HomeIntent
    data object CreateSelected : HomeIntent
    data class GameSelected(val gameId: GameId) : HomeIntent
    data class ContentLoaded(val generation: Long, val content: HomeContent) : HomeIntent
    data class OperationFailed(val failure: LocalGameFailure, val generation: Long) : HomeIntent
    data class ImportFinished(val result: HomeImportResult) : HomeIntent
}
