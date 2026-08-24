package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.GameSnapshotLoader
import kotlinx.coroutines.CoroutineScope

class HomeFactory(
    private val snapshotLoader: GameSnapshotLoader,
    private val importPreparer: GameImportPreparer,
    private val importConfirmer: GameImportConfirmer,
    private val importCanceller: GameImportCanceller,
    private val output: (HomeOutput) -> Unit,
) {
    fun create(
        scope: CoroutineScope,
        initialState: HomeState = HomeState(),
    ): HomeInteractor = HomeInteractor(
        snapshotLoader = snapshotLoader,
        importPreparer = importPreparer,
        importConfirmer = importConfirmer,
        importCanceller = importCanceller,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
