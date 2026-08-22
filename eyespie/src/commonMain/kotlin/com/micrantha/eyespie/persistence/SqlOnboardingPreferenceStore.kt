package com.micrantha.eyespie.persistence

import com.micrantha.eyespie.data.EyesPieDatabase
import com.micrantha.eyespie.features.onboarding.OnboardingPreferenceStore

class SqlOnboardingPreferenceStore(
    database: EyesPieDatabase,
) : OnboardingPreferenceStore {
    private val queries = database.eyesPieQueries

    override suspend fun isCompleted(): Boolean =
        queries.selectPreference(ONBOARDING_COMPLETED_KEY).executeAsOneOrNull() == TRUE_VALUE

    override suspend fun markCompleted() {
        queries.upsertPreference(ONBOARDING_COMPLETED_KEY, TRUE_VALUE)
    }

    private companion object {
        const val ONBOARDING_COMPLETED_KEY = "onboarding.completed"
        const val TRUE_VALUE = "true"
    }
}
