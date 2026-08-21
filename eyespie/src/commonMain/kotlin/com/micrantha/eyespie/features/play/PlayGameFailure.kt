package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.game.LocalGameFailure

sealed interface PlayGameFailure {
    data object CameraUnavailable : PlayGameFailure
    data class Game(val failure: LocalGameFailure) : PlayGameFailure
}
