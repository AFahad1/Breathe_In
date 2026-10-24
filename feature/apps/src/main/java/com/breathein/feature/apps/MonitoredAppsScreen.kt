package com.breathein.feature.apps

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.breathein.core.common.ShortFormApps
import com.breathein.core.common.UsageRepository
import com.breathein.core.model.AppInfo
import com.breathein.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lets the user pick which apps get breathing breaks and how long a session may run in each. */
@Composable
fun MonitoredAppsScreen(
    usageRepository: UsageRepository,
    settingsRepository: SettingsRepository,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val monitored by settingsRepository.monitoredApps.collectAsStateWithLifecycle(initialValue = emptyList())
    val installed by produceState<List<AppInfo>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { usageRepository.launchableApps() }
    }
    val limits = monitored.associate { it.packageName to it.limitSeconds }
    val installedPackages = installed.orEmpty().mapTo(HashSet()) { it.packageName }
    val otherApps = installed.orEmpty().filter { ShortFormApps.find(it.packageName) == null }

    fun enable(packageName: String) = scope.launch {
        settingsRepository.setMonitoredApp(packageName, SettingsRepository.DEFAULT_LIMIT_SECONDS)
    }
    fun disable(packageName: String) = scope.launch { settingsRepository.removeMonitoredApp(packageName) }
    fun setLimit(packageName: String, seconds: Int) = scope.launch {
        settingsRepository.setMonitoredApp(packageName, seconds)
    }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            TextButton(onClick = onDone) { Text(stringResource(R.string.apps_done)) }
            Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.apps_body), style = MaterialTheme.typography.bodyMedium)
            SectionHeader(stringResource(R.string.apps_short_form))
        }
        items(ShortFormApps.all, key = { it.packageName }) { app ->
            val isInstalled = installed == null || app.packageName in installedPackages
            AppLimitRow(
                title = stringResource(R.string.apps_feed_title, app.label, app.feedName),
                subtitle = if (isInstalled) feedSubtitle(app.feature != null) else stringResource(R.string.apps_not_installed),
                limitSeconds = limits[app.packageName],
                limitOptions = SettingsRepository.LIMIT_OPTIONS_SECONDS,
                onEnable = { enable(app.packageName) },
                onDisable = { disable(app.packageName) },
                onLimitChange = { setLimit(app.packageName, it) },
            )
        }
        item { SectionHeader(stringResource(R.string.apps_other)) }
        if (installed == null) {
            item { Text(stringResource(R.string.apps_loading)) }
        }
        items(otherApps, key = { it.packageName }) { app ->
            AppLimitRow(
                title = app.label,
                subtitle = stringResource(R.string.apps_whole_app),
                limitSeconds = limits[app.packageName],
                limitOptions = SettingsRepository.LIMIT_OPTIONS_SECONDS,
                onEnable = { enable(app.packageName) },
                onDisable = { disable(app.packageName) },
                onLimitChange = { setLimit(app.packageName, it) },
            )
        }
    }
}

@Composable
private fun feedSubtitle(detectedInApp: Boolean): String =
    stringResource(if (detectedInApp) R.string.apps_feed_only else R.string.apps_whole_app)

@Composable
private fun SectionHeader(text: String) {
    HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}
