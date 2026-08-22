package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameFailure

sealed interface UtilityIntent {
    data object Load : UtilityIntent
    data object Retry : UtilityIntent
    data object DismissFailure : UtilityIntent
    data object OnboardingSelected : UtilityIntent
    data object Back : UtilityIntent
    data class ContentLoaded(val generation: Long, val content: UtilityContent) : UtilityIntent
    data class LoadFailed(val generation: Long, val failure: LocalGameFailure) : UtilityIntent
}
