package com.micrantha.eyespie.features.utility

import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.mvi.Interactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UtilityInteractor(
    private val port: UtilityPort,
    private val scope: CoroutineScope,
    private val output: (UtilityOutput) -> Unit,
    initialState: UtilityState = UtilityState(),
) : Interactor<UtilityState, UtilityIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<UtilityState> = mutableState.asStateFlow()

    override fun dispatch(intent: UtilityIntent) {
        mutableState.value = UtilityReducer.reduce(mutableState.value, intent)
        when (intent) {
            UtilityIntent.Load,
            UtilityIntent.Retry -> {
                val generation = mutableState.value.loadGeneration
                scope.launch {
                    when (val result = port.load()) {
                        is LocalGameResult.Success -> dispatch(UtilityIntent.ContentLoaded(generation, result.value))
                        is LocalGameResult.Failure -> dispatch(UtilityIntent.LoadFailed(generation, result.failure))
                    }
                }
            }
            UtilityIntent.OnboardingSelected -> output(UtilityOutput.OnboardingRequested)
            UtilityIntent.Back -> output(UtilityOutput.Closed)
            else -> Unit
        }
    }
}
