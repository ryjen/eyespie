package com.micrantha.eyespie.features.onboarding.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

interface OnboardingRepository {
    suspend fun setHasRunOnce(value: Boolean = true)
    suspend fun hasRunOnce(): Boolean
    suspend fun setGenAiModel(model: String)
    suspend fun hasGenAI(): Boolean
    suspend fun genAiModel(): String?
}

internal class DataOnboardingRepository(
    private val localSource: OnboardingLocalSource
) : OnboardingRepository {
    private val hasRunOnceKey = booleanPreferencesKey("has_run_once")
    private val genAiModelKey = stringPreferencesKey("model")

    override suspend fun setHasRunOnce(value: Boolean) {
        localSource.dataStore.edit { prefs ->
            prefs[hasRunOnceKey] = value
        }
    }

    override suspend fun hasRunOnce(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasRunOnceKey] ?: false
    }

    override suspend fun setGenAiModel(model: String) {
        localSource.dataStore.edit { prefs ->
            prefs[genAiModelKey] = model
        }
    }

    override suspend fun hasGenAI(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[genAiModelKey].isNullOrBlank().not()
    }

    override suspend fun genAiModel(): String? {
        val prefs = localSource.dataStore.data.first()
        return prefs[genAiModelKey]
    }
}
