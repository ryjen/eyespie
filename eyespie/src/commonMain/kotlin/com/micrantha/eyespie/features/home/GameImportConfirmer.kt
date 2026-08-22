package com.micrantha.eyespie.features.home

interface GameImportConfirmer {
    suspend fun confirmImport(): HomeImportResult
}
