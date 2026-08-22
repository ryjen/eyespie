package com.micrantha.eyespie.features.onboarding

interface OnboardingPreferenceStore {
    suspend fun isCompleted(): Boolean
    suspend fun markCompleted()
}
