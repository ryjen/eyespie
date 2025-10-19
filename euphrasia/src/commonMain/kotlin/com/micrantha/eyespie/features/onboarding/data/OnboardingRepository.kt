package com.micrantha.eyespie.features.onboarding.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class OnboardingRepository(
    private val localSource: OnboardingLocalSource
) {
    val hasRunOnce = booleanPreferencesKey("has_run_once")

    val hasGenAI = stringPreferencesKey("has_gen_ai")

    suspend fun setHasRunOnce() {
        localSource.dataStore.edit { prefs ->
            prefs[hasRunOnce] = true
        }
    }

    suspend fun hasRunOnce(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasRunOnce] ?: false
    }

    suspend fun setHasGenAI(model: String) {
        localSource.dataStore.edit { prefs ->
            prefs[hasGenAI] = model
        }
    }

    suspend fun genAiModel(): String? {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasGenAI]
    }

    suspend fun hasGenAI(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasGenAI].isNullOrBlank().not()
    }
}
