package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.imaging.CapturedImage

sealed interface CreateGameIntent {
    data class NameChanged(val value: String) : CreateGameIntent
    data class ClueChanged(val value: String) : CreateGameIntent
    data class ExpectedAnswerChanged(val value: String) : CreateGameIntent
    data class TargetCaptured(val image: CapturedImage) : CreateGameIntent
    data object CameraFailed : CreateGameIntent
    data object DismissFailure : CreateGameIntent
    data object Back : CreateGameIntent
    data object Created : CreateGameIntent
    data class OperationFailed(val failure: LocalGameFailure) : CreateGameIntent
}
