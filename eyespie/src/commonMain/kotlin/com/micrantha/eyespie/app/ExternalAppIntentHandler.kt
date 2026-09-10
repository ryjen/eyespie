package com.micrantha.eyespie.app

import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameResult

class ExternalAppIntentHandler(
    private val snapshotLoader: GameSnapshotLoader,
    private val navigation: AppNavigation,
    private val importCanceller: GameImportCanceller,
) {
    suspend fun handle(intent: ExternalAppIntent): ExternalAppIntentResult = when (intent) {
        is ExternalAppIntent.OpenLocalGame -> openLocalGame(intent)
    }

    private suspend fun openLocalGame(intent: ExternalAppIntent.OpenLocalGame): ExternalAppIntentResult {
        val snapshot = when (val loaded = snapshotLoader.loadSnapshot()) {
            is LocalGameResult.Success -> loaded.value
            is LocalGameResult.Failure -> return ExternalAppIntentResult.Failed
        }
        if (snapshot.games.none { it.id == intent.gameId }) {
            return ExternalAppIntentResult.NotFound
        }

        // An external navigation request must not leave unconfirmed import authority hanging around.
        importCanceller.cancelImport()
        navigation.replaceAll(AppRoute.Home)
        navigation.push(AppRoute.GameDetail(intent.gameId))
        return ExternalAppIntentResult.Opened
    }
}

sealed interface ExternalAppIntentResult {
    data object Opened : ExternalAppIntentResult
    data object NotFound : ExternalAppIntentResult
    data object Failed : ExternalAppIntentResult
}
