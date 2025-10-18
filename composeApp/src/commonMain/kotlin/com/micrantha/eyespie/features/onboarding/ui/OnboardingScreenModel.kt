package com.micrantha.eyespie.features.onboarding.ui

import com.micrantha.bluebell.com.micrantha.eyespie.features.onboarding.ui.OnboardingEffects
import com.micrantha.bluebell.com.micrantha.eyespie.features.onboarding.ui.OnboardingReducer
import com.micrantha.bluebell.ui.screen.MappedScreenModel
import com.micrantha.bluebell.ui.screen.ScreenContext
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingState
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingUiState

class OnboardingScreenModel(
    context: ScreenContext,
    private val effects: OnboardingEffects,
    private val reducer: OnboardingReducer = OnboardingReducer(),
    private val mapper: OnboardingMapper = OnboardingMapper(context),
    initialState: OnboardingState = OnboardingState()
) : MappedScreenModel<OnboardingState, OnboardingUiState>(
    context, initialState, mapper::map
) {
    init {
        store.addReducer(reducer::reduce)
            .applyEffect(effects::invoke)
    }
}
