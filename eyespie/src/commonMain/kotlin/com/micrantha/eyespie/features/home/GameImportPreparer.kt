package com.micrantha.eyespie.features.home

interface GameImportPreparer {
    suspend fun prepareImport(): HomeImportPreparationResult
}
