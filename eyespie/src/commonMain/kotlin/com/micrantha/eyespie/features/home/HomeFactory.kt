package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.GameThumbnailCache
import kotlinx.coroutines.CoroutineScope

class HomeFactory(
    private val snapshotLoader: GameSnapshotLoader,
    private val importPreparer: GameImportPreparer,
    private val importConfirmer: GameImportConfirmer,
    private val importCanceller: GameImportCanceller,
    private val thumbnailCache: GameThumbnailCache,
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
        thumbnailCache = thumbnailCache,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
