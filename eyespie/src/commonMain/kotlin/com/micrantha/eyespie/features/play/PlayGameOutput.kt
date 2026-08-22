package com.micrantha.eyespie.features.play

sealed interface PlayGameOutput {
    data object Closed : PlayGameOutput
}
