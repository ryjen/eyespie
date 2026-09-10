package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.GameSnapshotLoader
import kotlinx.coroutines.CoroutineScope

class UtilityFactory(
    private val snapshotLoader: GameSnapshotLoader,
    private val output: (UtilityOutput) -> Unit,
    private val diagnosticsExporter: DiagnosticsExporter = UnavailableDiagnosticsExporter,
) {
    fun create(
        scope: CoroutineScope,
        initialState: UtilityState = UtilityState(),
    ): UtilityInteractor = UtilityInteractor(
        snapshotLoader = snapshotLoader,
        diagnosticsExporter = diagnosticsExporter,
        scope = scope,
        output = output,
        initialState = initialState,
    )
}
