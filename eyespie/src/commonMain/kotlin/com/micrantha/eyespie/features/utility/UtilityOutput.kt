package com.micrantha.eyespie.features.utility

sealed interface UtilityOutput {
    data object Closed : UtilityOutput
    data object OnboardingRequested : UtilityOutput
}
