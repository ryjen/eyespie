package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.mvi.Reducer

object HomeReducer : Reducer<HomeState, HomeIntent> {
    override fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        HomeIntent.Refresh -> state.copy(
            loading = true,
            refreshGeneration = state.refreshGeneration + 1,
        )
        HomeIntent.DismissFailure -> state.copy(failure = null)
        HomeIntent.DismissImportResult -> state.copy(importResult = null)
        HomeIntent.ImportSelected -> if (state.importInProgress) {
            state
        } else {
            state.copy(importInProgress = true, importResult = null)
        }
        is HomeIntent.ImportFinished -> state.copy(
            importInProgress = false,
            importResult = if (intent.result == HomeImportResult.Cancelled) null else intent.result,
        )
        HomeIntent.OnboardingSelected,
        HomeIntent.CreateSelected,
        is HomeIntent.GameSelected -> state
        is HomeIntent.ContentLoaded -> if (intent.generation == state.refreshGeneration) {
            state.copy(content = intent.content, loading = false, failure = null)
        } else {
            state
        }
        is HomeIntent.OperationFailed -> if (intent.generation == state.refreshGeneration) {
            state.copy(loading = false, failure = intent.failure)
        } else {
            state
        }
    }
}
