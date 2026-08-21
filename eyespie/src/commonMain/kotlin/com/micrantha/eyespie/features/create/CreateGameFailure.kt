package com.micrantha.eyespie.features.create

import com.micrantha.eyespie.game.LocalGameFailure

sealed interface CreateGameFailure {
    data object CameraUnavailable : CreateGameFailure
    data class Game(val failure: LocalGameFailure) : CreateGameFailure
}
