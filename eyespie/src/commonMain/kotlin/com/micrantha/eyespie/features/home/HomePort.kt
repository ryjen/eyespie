package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.LocalGameResult

sealed interface HomeImportResult {
    data object Imported : HomeImportResult
    data object AlreadyPresent : HomeImportResult
    data object Conflict : HomeImportResult
    data object InvalidFile : HomeImportResult
    data object TooLarge : HomeImportResult
    data object Busy : HomeImportResult
    data object Cancelled : HomeImportResult
    data object Failed : HomeImportResult
    data object Unavailable : HomeImportResult
}

interface HomePort {
    suspend fun load(): LocalGameResult<HomeContent>

    suspend fun importGame(): HomeImportResult = HomeImportResult.Unavailable
}
