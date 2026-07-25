package com.micrantha.eyespie.features.onboarding.arch

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Reducer
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilitiesLoaded
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestFailed
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestResolved
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.CapabilityRequestStarted
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Done
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Error
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Init
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.Loaded
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.PageChanged
import com.micrantha.eyespie.features.onboarding.entities.OnboardingAction.SelectModel
import com.micrantha.eyespie.features.onboarding.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.entities.OnboardingState

class OnboardingReducer : Reducer<OnboardingState> {
    override fun reduce(
        state: OnboardingState,
        action: Action,
    ): OnboardingState = when (action) {
        is Init -> state.copy(
            isInitializing = true,
            error = null,
        )

        is Error -> state.copy(
            error = action.error,
            isInitializing = false,
        )

        is SelectModel -> state.copy(
            selectedModel = action.name,
        )

        is PageChanged -> OnboardingPage.entries.getOrNull(action.page)?.let {
            state.copy(page = it)
        } ?: state

        is Done -> state.copy(
            isInitializing = false,
        )

        is Loaded -> state.copy(
            models = action.models,
            isInitializing = false,
            error = null,
        )

        is CapabilitiesLoaded -> state.copy(
            capabilities = action.capabilities,
        )

        is CapabilityRequestStarted -> if (
            state.requestInFlight == null &&
            state.capabilities.any {
                it.capability == action.capability && it.canRequestDuringOnboarding
            }
        ) {
            state.copy(requestInFlight = action.capability)
        } else {
            state
        }

        is CapabilityRequestResolved -> if (state.requestInFlight == action.capability) {
            state.copy(
                capabilities = state.capabilities.map {
                    if (it.capability == action.capability) {
                        it.copy(authorization = action.authorization)
                    } else {
                        it
                    }
                },
                requestInFlight = null,
            )
        } else {
            state
        }

        is CapabilityRequestFailed -> if (state.requestInFlight == action.capability) {
            state.copy(requestInFlight = null)
        } else {
            state
        }

        else -> state
    }
}
