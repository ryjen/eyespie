package com.micrantha.eyespie.features.create

sealed interface CreateGameOutput {
    data object Created : CreateGameOutput
    data object Cancelled : CreateGameOutput
}
