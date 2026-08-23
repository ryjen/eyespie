package com.micrantha.eyespie.features.home

import kotlinx.coroutines.CoroutineScope

class HomeFactory(
    private val loader: HomeLoader,
    private val importPreparer: GameImportPreparer,
    private val importConfirmer: GameImportConfirmer,
    private val importCanceller: GameImportCanceller,
    private val output: (HomeOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: HomeState = HomeState(),
    ): HomeInteractor = HomeInteractor(
        loader = loader,
        importPreparer = importPreparer,
        importConfirmer = importConfirmer,
        importCanceller = importCanceller,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
