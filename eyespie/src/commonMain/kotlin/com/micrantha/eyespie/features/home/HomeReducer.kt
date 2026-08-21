package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.mvi.Reducer

object HomeReducer : Reducer<HomeState, HomeIntent> {
    override fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        HomeIntent.Refresh -> state.copy(
            loading = true,
            refreshGeneration = state.refreshGeneration + 1,
        )
        HomeIntent.DismissFailure -> state.copy(failure = null)
        HomeIntent.OnboardingSelected,
        HomeIntent.CreateSelected,
        is HomeIntent.PlaySelected -> state
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
