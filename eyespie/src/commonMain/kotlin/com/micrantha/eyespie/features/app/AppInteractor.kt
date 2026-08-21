package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppInteractor(
    private val runtime: EyespieRuntime,
    private val scope: CoroutineScope,
    initialState: AppState = AppState(),
) : Interactor<AppState, AppIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<AppState> = mutableState.asStateFlow()

    override fun dispatch(intent: AppIntent) {
        mutableState.value = AppReducer.reduce(mutableState.value, intent)
        induce(intent, mutableState.value)
    }

    private fun induce(intent: AppIntent, stateAfterReduce: AppState) {
        when (intent) {
            AppIntent.Refresh -> scope.launch { refreshSnapshot() }

            is AppIntent.CreateTargetCaptured -> {
                val form = stateAfterReduce.createForm
                scope.launch {
                    when (
                        val result = runtime.gameLoop.createGame(
                            name = form.name,
                            clueText = form.clue,
                            expectedAnswer = form.expectedAnswer,
                            targetImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> when (val snapshot = runtime.gameLoop.loadSnapshot()) {
                            is LocalGameResult.Success -> dispatch(AppIntent.GameCreated(snapshot.value))
                            is LocalGameResult.Failure -> dispatch(AppIntent.OperationFailed(snapshot.failure))
                        }
                        is LocalGameResult.Failure -> dispatch(AppIntent.OperationFailed(result.failure))
                    }
                }
            }

            is AppIntent.GuessCaptured -> {
                val selected = stateAfterReduce.screen as? AppScreen.Play ?: return
                scope.launch {
                    when (
                        val result = runtime.gameLoop.guess(
                            gameId = selected.gameId,
                            thingId = selected.thingId,
                            guessImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> {
                            val refreshed = when (val snapshot = runtime.gameLoop.loadSnapshot()) {
                                is LocalGameResult.Success -> snapshot.value
                                is LocalGameResult.Failure -> null
                            }
                            dispatch(AppIntent.GuessCompleted(result.value, refreshed))
                        }
                        is LocalGameResult.Failure -> dispatch(AppIntent.OperationFailed(result.failure))
                    }
                }
            }

            else -> Unit
        }
    }

    private suspend fun refreshSnapshot() {
        when (val result = runtime.gameLoop.loadSnapshot()) {
            is LocalGameResult.Success -> dispatch(AppIntent.SnapshotLoaded(result.value))
            is LocalGameResult.Failure -> dispatch(AppIntent.OperationFailed(result.failure))
        }
    }
}
