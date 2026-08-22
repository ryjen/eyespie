package com.micrantha.eyespie.features.home

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

sealed interface HomeImportPreparationResult {
    data class Ready(val preview: HomeImportPreview) : HomeImportPreparationResult
    data class Terminal(val result: HomeImportResult) : HomeImportPreparationResult
}
