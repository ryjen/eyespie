package com.micrantha.eyespie.features.onboarding

sealed interface OnboardingOutput {
    data object Completed : OnboardingOutput
    data object Dismissed : OnboardingOutput
}
