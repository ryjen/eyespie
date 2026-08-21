package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.EyespieRuntime
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface AppGameUseCases {
    suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot>
    suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame>
    suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome>
}

private class RuntimeAppGameUseCases(
    runtime: EyespieRuntime,
) : AppGameUseCases {
    private val gameLoop = runtime.gameLoop

    override suspend fun loadSnapshot() = gameLoop.loadSnapshot()

    override suspend fun createGame(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ) = gameLoop.createGame(name, clueText, expectedAnswer, targetImage)

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ) = gameLoop.guess(gameId, thingId, guessImage)
}

class AppInteractor(
    private val useCases: AppGameUseCases,
    private val scope: CoroutineScope,
    initialState: AppState = AppState(),
) : Interactor<AppState, AppIntent> {
    constructor(
        runtime: EyespieRuntime,
        scope: CoroutineScope,
        initialState: AppState = AppState(),
    ) : this(RuntimeAppGameUseCases(runtime), scope, initialState)

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
                        val result = useCases.createGame(
                            name = form.name,
                            clueText = form.clue,
                            expectedAnswer = form.expectedAnswer,
                            targetImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> when (val snapshot = useCases.loadSnapshot()) {
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
                        val result = useCases.guess(
                            gameId = selected.gameId,
                            thingId = selected.thingId,
                            guessImage = intent.image,
                        )
                    ) {
                        is LocalGameResult.Success -> when (val snapshot = useCases.loadSnapshot()) {
                            is LocalGameResult.Success -> dispatch(
                                AppIntent.GuessCompleted(result.value, snapshot.value),
                            )
                            is LocalGameResult.Failure -> dispatch(
                                AppIntent.GuessCompletedWithRefreshFailure(
                                    outcome = result.value,
                                    failure = snapshot.failure,
                                ),
                            )
                        }
                        is LocalGameResult.Failure -> dispatch(AppIntent.OperationFailed(result.failure))
                    }
                }
            }

            else -> Unit
        }
    }

    private suspend fun refreshSnapshot() {
        when (val result = useCases.loadSnapshot()) {
            is LocalGameResult.Success -> dispatch(AppIntent.SnapshotLoaded(result.value))
            is LocalGameResult.Failure -> dispatch(AppIntent.OperationFailed(result.failure))
        }
    }
}
