package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.imaging.CapturedImage

sealed interface ClueAuthoringIntent {
    data class ClueChanged(val value: String) : ClueAuthoringIntent
    data class ExpectedAnswerChanged(val value: String) : ClueAuthoringIntent
    data object CaptureStarted : ClueAuthoringIntent
    data class TargetCaptured(val image: CapturedImage) : ClueAuthoringIntent
    data object CameraFailed : ClueAuthoringIntent
    data object DismissFailure : ClueAuthoringIntent
    data object Back : ClueAuthoringIntent
    data object Added : ClueAuthoringIntent
    data class OperationFailed(val failure: LocalGameFailure) : ClueAuthoringIntent
}
