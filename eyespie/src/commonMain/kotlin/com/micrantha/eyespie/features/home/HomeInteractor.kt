package com.micrantha.eyespie.features.home

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeInteractor(
    private val port: HomePort,
    private val scope: CoroutineScope,
    private val output: (HomeOutput) -> Unit,
    initialState: HomeState = HomeState(),
) : Interactor<HomeState, HomeIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<HomeState> = mutableState.asStateFlow()

    override fun dispatch(intent: HomeIntent) {
        val previousState = mutableState.value
        mutableState.value = HomeReducer.reduce(previousState, intent)
        when (intent) {
            HomeIntent.Refresh -> {
                val generation = mutableState.value.refreshGeneration
                scope.launch {
                    when (val result = port.load()) {
                        is LocalGameResult.Success -> dispatch(HomeIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(HomeIntent.OperationFailed(result.failure, generation))
                    }
                }
            }
            HomeIntent.ImportSelected -> if (!previousState.importInProgress) {
                scope.launch {
                    val result = port.importGame()
                    dispatch(HomeIntent.ImportFinished(result))
                    if (result == HomeImportResult.Imported || result == HomeImportResult.AlreadyPresent) {
                        dispatch(HomeIntent.Refresh)
                    }
                }
            }
            HomeIntent.OnboardingSelected -> output(HomeOutput.OnboardingRequested)
            HomeIntent.UtilitySelected -> output(HomeOutput.UtilityRequested)
            HomeIntent.CreateSelected -> output(HomeOutput.CreateRequested)
            is HomeIntent.GameSelected -> output(HomeOutput.GameRequested(intent.gameId))
            else -> Unit
        }
    }
}
