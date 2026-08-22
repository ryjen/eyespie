package com.micrantha.eyespie.features.onboarding

sealed interface OnboardingIntent {
    data object Next : OnboardingIntent
    data object Previous : OnboardingIntent
    data object Skip : OnboardingIntent
    data object Done : OnboardingIntent
    data object Back : OnboardingIntent
}
