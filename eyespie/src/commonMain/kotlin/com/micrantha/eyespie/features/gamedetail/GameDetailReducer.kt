package com.micrantha.eyespie.features.gamedetail

import com.micrantha.eyespie.mvi.Reducer

object GameDetailReducer : Reducer<GameDetailState, GameDetailIntent> {
    override fun reduce(state: GameDetailState, intent: GameDetailIntent): GameDetailState = when (intent) {
        GameDetailIntent.Load -> state.copy(
            loading = true,
            failure = null,
            loadGeneration = state.loadGeneration + 1,
        )
        GameDetailIntent.DismissFailure -> state.copy(failure = null)
        GameDetailIntent.DismissShareResult -> state.copy(shareResult = null)
        GameDetailIntent.ShareSelected -> if (state.shareInProgress || state.content?.localCreator != true) {
            state
        } else {
            state.copy(shareInProgress = true, shareResult = null)
        }
        is GameDetailIntent.ShareFinished -> state.copy(
            shareInProgress = false,
            shareResult = if (intent.result == GameDetailShareResult.Cancelled) null else intent.result,
        )
        is GameDetailIntent.ContentLoaded -> if (intent.generation == state.loadGeneration) {
            state.copy(content = intent.content, loading = false, failure = null)
        } else {
            state
        }
        is GameDetailIntent.OperationFailed -> if (intent.generation == state.loadGeneration) {
            state.copy(loading = false, failure = intent.failure)
        } else {
            state
        }
        GameDetailIntent.Back,
        GameDetailIntent.AddClueSelected,
        is GameDetailIntent.PlaySelected -> state
    }
}
