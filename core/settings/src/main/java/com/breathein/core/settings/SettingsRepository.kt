package com.breathein.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User settings persisted in DataStore. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /** Minutes of continuous use before the interruption fires. */
    val thresholdMinutes: Flow<Int> = dataStore.data.map { it[THRESHOLD_MINUTES] ?: DEFAULT_THRESHOLD_MINUTES }

    val monitoringEnabled: Flow<Boolean> = dataStore.data.map { it[MONITORING_ENABLED] ?: false }

    suspend fun setThresholdMinutes(minutes: Int) {
        dataStore.edit { it[THRESHOLD_MINUTES] = minutes.coerceIn(MIN_THRESHOLD_MINUTES, MAX_THRESHOLD_MINUTES) }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[MONITORING_ENABLED] = enabled }
    }

    companion object {
        const val MIN_THRESHOLD_MINUTES = 1
        const val MAX_THRESHOLD_MINUTES = 30
        val DEFAULT_THRESHOLD_MINUTES = if (BuildConfig.DEBUG) 1 else 10

        private val THRESHOLD_MINUTES = intPreferencesKey("threshold_minutes")
        private val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
    }
}
