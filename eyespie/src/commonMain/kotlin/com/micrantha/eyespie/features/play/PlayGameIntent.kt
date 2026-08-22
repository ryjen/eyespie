package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.imaging.CapturedImage

sealed interface PlayGameIntent {
    data object Load : PlayGameIntent
    data class ContentLoaded(val content: PlayGameContent) : PlayGameIntent
    data class LoadFailed(val failure: LocalGameFailure) : PlayGameIntent
    data class GuessCaptured(val image: CapturedImage) : PlayGameIntent
    data class GuessCompleted(val outcome: GuessOutcome) : PlayGameIntent
    data class OperationFailed(val failure: LocalGameFailure) : PlayGameIntent
    data object CameraFailed : PlayGameIntent
    data object DismissFailure : PlayGameIntent
    data object Back : PlayGameIntent
}
