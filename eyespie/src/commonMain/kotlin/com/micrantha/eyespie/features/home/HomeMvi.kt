package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.features.app.AppFailure
import com.micrantha.eyespie.features.app.AppGameUseCases
import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.mvi.Interactor
import com.micrantha.eyespie.mvi.Reducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val snapshot: LocalGameSnapshot? = null,
    val loading: Boolean = true,
    val failure: AppFailure? = null,
)

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object DismissFailure : HomeIntent
    data object CreateSelected : HomeIntent
    data class PlaySelected(val screen: AppScreen.Play) : HomeIntent
    data class SnapshotLoaded(val snapshot: LocalGameSnapshot) : HomeIntent
    data class OperationFailed(val failure: LocalGameFailure) : HomeIntent
    data class AdoptSnapshot(val snapshot: LocalGameSnapshot) : HomeIntent
}

object HomeReducer : Reducer<HomeState, HomeIntent> {
    override fun reduce(state: HomeState, intent: HomeIntent): HomeState = when (intent) {
        HomeIntent.Refresh -> state.copy(loading = true)
        HomeIntent.DismissFailure -> state.copy(failure = null)
        HomeIntent.CreateSelected,
        is HomeIntent.PlaySelected -> state
        is HomeIntent.SnapshotLoaded -> state.copy(
            snapshot = intent.snapshot,
            loading = false,
            failure = null,
        )
        is HomeIntent.AdoptSnapshot -> state.copy(
            snapshot = intent.snapshot,
            loading = false,
            failure = null,
        )
        is HomeIntent.OperationFailed -> state.copy(
            loading = false,
            failure = AppFailure.Game(intent.failure),
        )
    }
}

class HomeInteractor(
    private val useCases: AppGameUseCases,
    private val scope: CoroutineScope,
    private val navigate: (AppScreen) -> Unit,
    initialState: HomeState = HomeState(),
) : Interactor<HomeState, HomeIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<HomeState> = mutableState.asStateFlow()

    override fun dispatch(intent: HomeIntent) {
        mutableState.value = HomeReducer.reduce(mutableState.value, intent)
        when (intent) {
            HomeIntent.Refresh -> scope.launch {
                when (val result = useCases.loadSnapshot()) {
                    is LocalGameResult.Success -> dispatch(HomeIntent.SnapshotLoaded(result.value))
                    is LocalGameResult.Failure -> dispatch(HomeIntent.OperationFailed(result.failure))
                }
            }
            HomeIntent.CreateSelected -> navigate(AppScreen.Create)
            is HomeIntent.PlaySelected -> navigate(intent.screen)
            else -> Unit
        }
    }
}
