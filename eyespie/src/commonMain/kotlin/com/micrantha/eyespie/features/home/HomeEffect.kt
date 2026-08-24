package com.micrantha.eyespie.features.home

sealed interface HomeEffect {
    data class ImportFinished(val result: HomeImportResult) : HomeEffect
}
