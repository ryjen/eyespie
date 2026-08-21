package com.micrantha.eyespie.features.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun OnboardingRoute(factory: OnboardingFactory) {
    val interactor = remember(factory) { factory.create() }
    val state by interactor.state.collectAsState()

    OnboardingScreen(
        state = state,
        dispatch = interactor::dispatch,
    )
}
