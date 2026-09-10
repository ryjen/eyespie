package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.GameThumbnailCache
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.sharing.ExternalGameDocumentSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class HomeExternalDocumentTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pending_external_document_enters_existing_verified_preview_flow() = runTest {
        val preview = HomeImportPreview("Shared mission", 2, "creator-1234", "game-5678")
        val capabilities = ExternalImportCapabilities(
            HomeImportPreparationResult.Ready(preview),
        )
        val source = FakeExternalGameDocumentSource(pending = true)
        val interactor = HomeFactory(
            snapshotLoader = capabilities,
            importPreparer = capabilities,
            importConfirmer = capabilities,
            importCanceller = capabilities,
            thumbnailCache = capabilities,
            output = {},
            externalDocumentSource = source,
        ).create(
            scope = this,
            initialState = HomeState(loading = false),
        )

        advanceUntilIdle()

        assertEquals(1, capabilities.prepares)
        assertEquals(preview, interactor.state.value.importPreview)
    }
}

private class FakeExternalGameDocumentSource(
    pending: Boolean,
) : ExternalGameDocumentSource {
    override val pending: StateFlow<Boolean> = MutableStateFlow(pending)
}

private class ExternalImportCapabilities(
    private val preparation: HomeImportPreparationResult,
) : GameSnapshotLoader, GameImportPreparer, GameImportConfirmer, GameImportCanceller, GameThumbnailCache {
    var prepares = 0

    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
        LocalGameResult.Success(
            LocalGameSnapshot(
                identity = PlayerIdentity(PlayerId("player-1"), "Agent"),
                games = emptyList(),
            ),
        )

    override suspend fun thumbnailsForGame(gameId: GameId): Map<ThingId, ByteArray> = emptyMap()

    override suspend fun prepareImport(): HomeImportPreparationResult {
        prepares += 1
        return preparation
    }

    override suspend fun confirmImport(): HomeImportResult = HomeImportResult.Unavailable

    override fun cancelImport() = Unit
}
