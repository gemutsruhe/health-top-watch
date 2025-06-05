package com.suisei.healthtopwatch.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val IS_FIRST_RUN_KEY = booleanPreferencesKey("is_first_run")

val Context.isFirstRun: Flow<Boolean>
    get() = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_FIRST_RUN_KEY] ?: true // 기본값 true
        }

suspend fun Context.setFirstRunDone() {
    dataStore.edit { preferences ->
        preferences[IS_FIRST_RUN_KEY] = false
    }
}