package com.breathein.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.breathein.core.common.ShortFormApps
import com.breathein.core.model.MonitoredApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User settings persisted in DataStore. */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    /** Apps to interrupt and their limits. Until the user changes anything, the short-form apps are monitored. */
    val monitoredApps: Flow<List<MonitoredApp>> = dataStore.data.map { prefs ->
        prefs[MONITORED_APPS]?.let(::decode) ?: DEFAULT_MONITORED_APPS
    }

    val monitoringEnabled: Flow<Boolean> = dataStore.data.map { it[MONITORING_ENABLED] ?: false }

    /** Adds [packageName] or changes its limit. */
    suspend fun setMonitoredApp(packageName: String, limitSeconds: Int) {
        updateMonitoredApps { apps -> apps.filterNot { it.packageName == packageName } + MonitoredApp(packageName, limitSeconds) }
    }

    suspend fun removeMonitoredApp(packageName: String) {
        updateMonitoredApps { apps -> apps.filterNot { it.packageName == packageName } }
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { it[MONITORING_ENABLED] = enabled }
    }

    private suspend fun updateMonitoredApps(change: (List<MonitoredApp>) -> List<MonitoredApp>) {
        dataStore.edit { prefs ->
            val current = prefs[MONITORED_APPS]?.let(::decode) ?: DEFAULT_MONITORED_APPS
            prefs[MONITORED_APPS] = encode(change(current))
        }
    }

    // Stored as a string set of "package=seconds"; package names never contain '='.
    private fun encode(apps: List<MonitoredApp>): Set<String> =
        apps.map { "${it.packageName}=${it.limitSeconds}" }.toSet()

    private fun decode(entries: Set<String>): List<MonitoredApp> = entries.mapNotNull { entry ->
        val packageName = entry.substringBefore('=')
        val seconds = entry.substringAfter('=').toIntOrNull()
        if (packageName.isNotEmpty() && seconds != null) MonitoredApp(packageName, seconds) else null
    }

    companion object {
        /** Debug builds also offer 10 s and 30 s so interruptions can be tested quickly. */
        val LIMIT_OPTIONS_SECONDS: List<Int> =
            (if (BuildConfig.DEBUG) listOf(10, 30) else emptyList()) +
                listOf(60, 2 * 60, 5 * 60, 10 * 60, 15 * 60, 20 * 60, 30 * 60, 45 * 60, 60 * 60)

        val DEFAULT_LIMIT_SECONDS = if (BuildConfig.DEBUG) 60 else 10 * 60

        private val DEFAULT_MONITORED_APPS =
            ShortFormApps.all.map { MonitoredApp(it.packageName, DEFAULT_LIMIT_SECONDS) }

        private val MONITORED_APPS = stringSetPreferencesKey("monitored_apps")
        private val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
    }
}
