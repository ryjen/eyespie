package com.micrantha.eyespie.features.onboarding

import com.micrantha.eyespie.features.app.AppScreen
import com.micrantha.eyespie.mvi.Interactor
import com.micrantha.eyespie.mvi.Reducer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OnboardingPage {
    Welcome,
    Create,
    Play,
}

data class OnboardingState(
    val page: OnboardingPage = OnboardingPage.Welcome,
)

sealed interface OnboardingIntent {
    data object Next : OnboardingIntent
    data object Previous : OnboardingIntent
    data object Done : OnboardingIntent
    data object Back : OnboardingIntent
}

object OnboardingReducer : Reducer<OnboardingState, OnboardingIntent> {
    override fun reduce(
        state: OnboardingState,
        intent: OnboardingIntent,
    ): OnboardingState = when (intent) {
        OnboardingIntent.Next -> state.copy(
            page = when (state.page) {
                OnboardingPage.Welcome -> OnboardingPage.Create
                OnboardingPage.Create -> OnboardingPage.Play
                OnboardingPage.Play -> OnboardingPage.Play
            },
        )
        OnboardingIntent.Previous -> state.copy(
            page = when (state.page) {
                OnboardingPage.Welcome -> OnboardingPage.Welcome
                OnboardingPage.Create -> OnboardingPage.Welcome
                OnboardingPage.Play -> OnboardingPage.Create
            },
        )
        OnboardingIntent.Done,
        OnboardingIntent.Back -> state
    }
}

class OnboardingInteractor(
    private val navigate: (AppScreen) -> Unit,
    initialState: OnboardingState = OnboardingState(),
) : Interactor<OnboardingState, OnboardingIntent> {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<OnboardingState> = mutableState.asStateFlow()

    override fun dispatch(intent: OnboardingIntent) {
        mutableState.value = OnboardingReducer.reduce(mutableState.value, intent)
        when (intent) {
            OnboardingIntent.Done,
            OnboardingIntent.Back -> navigate(AppScreen.Home)
            else -> Unit
        }
    }
}
