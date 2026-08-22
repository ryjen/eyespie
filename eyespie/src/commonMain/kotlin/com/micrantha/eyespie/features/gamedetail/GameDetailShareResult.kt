package com.micrantha.eyespie.features.gamedetail

sealed interface GameDetailShareResult {
    data object Shared : GameDetailShareResult
    data object NotLocalCreator : GameDetailShareResult
    data object TooLarge : GameDetailShareResult
    data object Busy : GameDetailShareResult
    data object Cancelled : GameDetailShareResult
    data object Failed : GameDetailShareResult
    data object Unavailable : GameDetailShareResult
}
