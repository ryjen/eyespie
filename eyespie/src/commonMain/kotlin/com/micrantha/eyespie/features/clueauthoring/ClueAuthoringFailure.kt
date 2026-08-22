package com.micrantha.eyespie.features.clueauthoring

import com.micrantha.eyespie.game.LocalGameFailure

sealed interface ClueAuthoringFailure {
    data object CameraUnavailable : ClueAuthoringFailure
    data class Game(val failure: LocalGameFailure) : ClueAuthoringFailure
}
