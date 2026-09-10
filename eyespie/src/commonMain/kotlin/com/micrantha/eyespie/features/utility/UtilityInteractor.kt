package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.BaseInteractor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UtilityInteractor(
    private val snapshotLoader: GameSnapshotLoader,
    private val diagnosticsExporter: DiagnosticsExporter,
    private val scope: CoroutineScope,
    private val output: (UtilityOutput) -> Unit,
    initialState: UtilityState = UtilityState(),
) : BaseInteractor<UtilityState, UtilityIntent>(initialState, UtilityReducer) {
    override fun afterReduce(
        intent: UtilityIntent,
        previousState: UtilityState,
        stateAfterReduce: UtilityState,
    ) {
        when (intent) {
            UtilityIntent.Load,
            UtilityIntent.Retry -> {
                val generation = stateAfterReduce.loadGeneration
                scope.launch {
                    when (val result = snapshotLoader.loadSnapshot()) {
                        is LocalGameResult.Success -> dispatch(
                            UtilityIntent.ContentLoaded(generation, UtilityMapper.map(result.value)),
                        )
                        is LocalGameResult.Failure -> dispatch(UtilityIntent.LoadFailed(generation, result.failure))
                    }
                }
            }
            UtilityIntent.ExportDiagnostics -> if (!previousState.exportingDiagnostics) {
                scope.launch {
                    val result = try {
                        diagnosticsExporter.export()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        DiagnosticExportResult.Failed
                    }
                    dispatch(UtilityIntent.DiagnosticExportCompleted(result))
                }
            }
            UtilityIntent.OnboardingSelected -> output(UtilityOutput.OnboardingRequested)
            UtilityIntent.Back -> output(UtilityOutput.Closed)
            else -> Unit
        }
    }
}
