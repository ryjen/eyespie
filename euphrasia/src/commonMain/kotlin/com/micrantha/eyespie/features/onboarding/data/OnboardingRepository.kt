package com.micrantha.eyespie.features.onboarding.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class OnboardingRepository(
    private val localSource: OnboardingLocalSource
) {
    val hasRunOnce = booleanPreferencesKey("has_run_once")

    val genAiModel = stringPreferencesKey("model")

    suspend fun setHasRunOnce(value: Boolean = true) {
        localSource.dataStore.edit { prefs ->
            prefs[hasRunOnce] = value
        }
    }

    suspend fun hasRunOnce(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasRunOnce] ?: false
    }

    suspend fun setGenAiModel(model: String) {
        localSource.dataStore.edit { prefs ->
            prefs[genAiModel] = model
        }
    }

    suspend fun hasGenAI(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[genAiModel].isNullOrBlank().not()
    }

    suspend fun genAiModel(): String? {
        val prefs = localSource.dataStore.data.first()
        return prefs[genAiModel]
    }

}
