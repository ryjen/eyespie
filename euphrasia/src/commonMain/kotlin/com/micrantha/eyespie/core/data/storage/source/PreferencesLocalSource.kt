package com.micrantha.eyespie.core.data.storage.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.micrantha.bluebell.platform.Platform
import kotlinx.coroutines.flow.map

class PreferencesLocalSource(private val platform: Platform) {
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { platform.filesPath().resolve("default.preferences_pb") }
        )
    }

    operator fun get(key: String) = dataStore.data.map { it[stringPreferencesKey(key)] }

    suspend operator fun set(key: String, value: String) =
        dataStore.edit { it[stringPreferencesKey(key)] = value }
}
