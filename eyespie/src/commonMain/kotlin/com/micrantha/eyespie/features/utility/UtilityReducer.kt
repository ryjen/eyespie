package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.mvi.Reducer

object UtilityReducer : Reducer<UtilityState, UtilityIntent> {
    override fun reduce(state: UtilityState, intent: UtilityIntent): UtilityState = when (intent) {
        UtilityIntent.Load,
        UtilityIntent.Retry -> state.copy(
            loading = true,
            failure = null,
            loadGeneration = state.loadGeneration + 1,
        )
        UtilityIntent.DismissFailure -> state.copy(failure = null)
        UtilityIntent.ExportDiagnostics -> if (state.exportingDiagnostics) {
            state
        } else {
            state.copy(
                exportingDiagnostics = true,
                diagnosticExportResult = null,
            )
        }
        is UtilityIntent.DiagnosticExportCompleted -> state.copy(
            exportingDiagnostics = false,
            diagnosticExportResult = intent.result,
        )
        is UtilityIntent.ContentLoaded -> if (intent.generation == state.loadGeneration) {
            state.copy(content = intent.content, loading = false, failure = null)
        } else {
            state
        }
        is UtilityIntent.LoadFailed -> if (intent.generation == state.loadGeneration) {
            state.copy(loading = false, failure = intent.failure)
        } else {
            state
        }
        UtilityIntent.OnboardingSelected,
        UtilityIntent.Back -> state
    }
}
