package com.micrantha.eyespie.features.onboarding.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class OnboardingRepository(
    private val localSource: OnboardingLocalSource
) {
    val hasRunOnce = booleanPreferencesKey("has_run_once")

    val hasGenAI = booleanPreferencesKey("has_gen_ai")

    val modelFile = stringPreferencesKey("model")

    suspend fun setHasRunOnce(value: Boolean = true) {
        localSource.dataStore.edit { prefs ->
            prefs[hasRunOnce] = value
        }
    }

    suspend fun hasRunOnce(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasRunOnce] ?: false
    }

    suspend fun setModelFile(model: String) {
        localSource.dataStore.edit { prefs ->
            prefs[modelFile] = model
        }
    }

    suspend fun setHasGenAI(value: Boolean = true) {
        localSource.dataStore.edit { prefs ->
            prefs[hasGenAI] = value
        }
    }

    suspend fun hasGenAI(): Boolean {
        val prefs = localSource.dataStore.data.first()
        return prefs[hasGenAI] ?: false
    }

    suspend fun modelFile(): String? {
        val prefs = localSource.dataStore.data.first()
        return prefs[modelFile]
    }

}
