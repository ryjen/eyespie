package com.micrantha.eyespie.features.gamedetail

sealed interface GameDetailEffect {
    data class ShareFinished(val result: GameDetailShareResult) : GameDetailEffect
}
