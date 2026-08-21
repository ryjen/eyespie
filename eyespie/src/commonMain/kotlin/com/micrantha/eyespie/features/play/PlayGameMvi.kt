package com.micrantha.eyespie.features.play

import com.micrantha.eyespie.features.app.AppFailure
import com.micrantha.eyespie.features.app.AppGameUseCases
import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.game.LocalGameSummary
import com.micrantha.eyespie.game.PlayableThingSummary
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.mvi.Interactor
import com.micrantha.eyespie.mvi.Reducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayGameState(
    val game: LocalGameSummary,
    val thing: PlayableThingSummary,
    val busy: Boolean = false,
    val failure: AppFailure? = null,
    val latestOutcome: GuessOutcome? = null,
)

sealed interface PlayGameIntent {
    data class GuessCaptured(val image: CapturedImage) : PlayGameIntent
    data class GuessCompleted(
        val outcome: GuessOutcome,
        val game: LocalGameSummary,
        val thing: PlayableThingSummary,
    ) : PlayGameIntent
    data class GuessCompletedWithRefreshFailure(
        val outcome: GuessOutcome,
        val failure: LocalGameFailure,
    ) : PlayGameIntent
    data class OperationFailed(val failure: LocalGameFailure) : PlayGameIntent
    data object CameraFailed : PlayGameIntent
    data object DismissFailure : PlayGameIntent
    data object Back : PlayGameIntent
}

object PlayGameReducer : Reducer<PlayGameState, PlayGameIntent> {
    override fun reduce(state: PlayGameState, intent: PlayGameIntent): PlayGameState = when (intent) {
        is PlayGameIntent.GuessCaptured -> state.copy(busy = true, failure = null)
        is PlayGameIntent.GuessCompleted -> state.copy(
            game = intent.game,
            thing = intent.thing,
            busy = false,
            failure = null,
            latestOutcome = intent.outcome,
        )
        is PlayGameIntent.GuessCompletedWithRefreshFailure -> state.copy(
            busy = false,
            failure = AppFailure.Game(intent.failure),
            latestOutcome = intent.outcome,
        )
        is PlayGameIntent.OperationFailed -> state.copy(
            busy = false,
            failure = AppFailure.Game(intent.failure),
        )
        PlayGameIntent.CameraFailed -> state.copy(failure = AppFailure.CameraUnavailable)
        PlayGameIntent.DismissFailure -> state.copy(failure = null)
        PlayGameIntent.Back -> state
    }
}

class PlayGameInteractor(
    private val useCases: AppGameUseCases,
    private val scope: CoroutineScope,
    private val navigate: (AppScreen) -> Unit,
    private val adoptSnapshot: (LocalGameSnapshot) -> Unit,
    initialState: PlayGameState,
) : Interactor<PlayGameState, PlayGameIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<PlayGameState> = mutableState.asStateFlow()

    override fun dispatch(intent: PlayGameIntent) {
        mutableState.value = PlayGameReducer.reduce(mutableState.value, intent)
        val stateAfterReduce = mutableState.value
        when (intent) {
            is PlayGameIntent.GuessCaptured -> scope.launch {
                when (
                    val result = useCases.guess(
                        gameId = stateAfterReduce.game.id,
                        thingId = stateAfterReduce.thing.id,
                        guessImage = intent.image,
                    )
                ) {
                    is LocalGameResult.Success -> when (val snapshot = useCases.loadSnapshot()) {
                        is LocalGameResult.Success -> {
                            val game = snapshot.value.games.firstOrNull { it.id == result.value.gameId }
                            val thing = game?.things?.firstOrNull { it.id == result.value.thingId }
                            if (game == null || thing == null) {
                                dispatch(
                                    PlayGameIntent.OperationFailed(
                                        LocalGameFailure(com.micrantha.eyespie.game.LocalGameFailureCode.THING_NOT_FOUND),
                                    ),
                                )
                            } else {
                                adoptSnapshot(snapshot.value)
                                dispatch(PlayGameIntent.GuessCompleted(result.value, game, thing))
                            }
                        }
                        is LocalGameResult.Failure -> dispatch(
                            PlayGameIntent.GuessCompletedWithRefreshFailure(
                                outcome = result.value,
                                failure = snapshot.failure,
                            ),
                        )
                    }
                    is LocalGameResult.Failure -> dispatch(PlayGameIntent.OperationFailed(result.failure))
                }
            }
            PlayGameIntent.Back -> navigate(AppScreen.Home)
            else -> Unit
        }
    }
}
