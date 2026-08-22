package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.mvi.Reducer

object PlayGameReducer : Reducer<PlayGameState, PlayGameIntent> {
    override fun reduce(state: PlayGameState, intent: PlayGameIntent): PlayGameState = when (intent) {
        PlayGameIntent.Load -> state.copy(
            loading = true,
            failure = null,
            feedback = null,
            loadGeneration = state.loadGeneration + 1,
        )
        is PlayGameIntent.ContentLoaded -> if (intent.generation == state.loadGeneration) {
            state.copy(
                content = intent.content,
                loading = false,
                failure = null,
                feedback = null,
            )
        } else {
            state
        }
        is PlayGameIntent.LoadFailed -> if (intent.generation == state.loadGeneration) {
            state.copy(
                loading = false,
                failure = PlayGameFailure.Game(intent.failure),
            )
        } else {
            state
        }
        is PlayGameIntent.GuessCaptured -> state.copy(
            busy = true,
            failure = null,
            feedback = null,
            guessGeneration = state.guessGeneration + 1,
        )
        is PlayGameIntent.GuessCompleted -> if (intent.generation != state.guessGeneration) {
            state
        } else {
            val content = state.content
            if (content == null) {
                state.copy(busy = false)
            } else {
                val progress = intent.outcome.progress
                val feedback = if (intent.outcome.match.matched) {
                    val foundCount = if (content.matched) {
                        content.foundCount
                    } else {
                        minOf(content.totalCount, content.foundCount + 1)
                    }
                    PlayFeedback.Matched(
                        similarity = intent.outcome.match.similarity,
                        bestSimilarity = progress.bestSimilarity ?: intent.outcome.match.similarity,
                        foundCount = foundCount,
                        totalCount = content.totalCount,
                        nextThingId = content.nextThingId,
                    )
                } else {
                    PlayFeedback.Mismatch(
                        similarity = intent.outcome.match.similarity,
                        bestSimilarity = progress.bestSimilarity ?: intent.outcome.match.similarity,
                    )
                }
                state.copy(
                    busy = false,
                    failure = null,
                    feedback = feedback,
                    content = content.copy(
                        matched = progress.matched,
                        bestSimilarity = progress.bestSimilarity,
                        foundCount = if (feedback is PlayFeedback.Matched) feedback.foundCount else content.foundCount,
                    ),
                )
            }
        }
        is PlayGameIntent.OperationFailed -> if (intent.generation == state.guessGeneration) {
            state.copy(
                busy = false,
                failure = PlayGameFailure.Game(intent.failure),
            )
        } else {
            state
        }
        PlayGameIntent.CameraFailed -> state.copy(failure = PlayGameFailure.CameraUnavailable)
        PlayGameIntent.DismissFailure -> state.copy(failure = null)
        PlayGameIntent.Continue,
        PlayGameIntent.Back -> state
    }
}
