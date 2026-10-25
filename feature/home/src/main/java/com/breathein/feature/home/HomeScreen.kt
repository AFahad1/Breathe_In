package com.breathein.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.breathein.core.common.MonitorController
import com.breathein.core.common.ShortFormApps
import com.breathein.core.common.UsageRepository
import com.breathein.core.model.MonitoredApp
import com.breathein.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The main screen: today's time in monitored apps, monitoring on/off and the app list.
 *
 * @param onOpenDeveloperTools shows a "Developer tools" link when set (debug builds only).
 */
@Composable
fun HomeScreen(
    usageRepository: UsageRepository,
    settingsRepository: SettingsRepository,
    monitorController: MonitorController,
    onOpenMonitoredApps: () -> Unit,
    onOpenDeveloperTools: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val isMonitoring by monitorController.isRunning.collectAsStateWithLifecycle()
    val monitoredApps by settingsRepository.monitoredApps.collectAsStateWithLifecycle(initialValue = null)
    var apps by remember { mutableStateOf<List<AppToday>?>(null) }

    // Usage data changes while the user is away, so refresh it periodically while the screen is visible.
    LaunchedEffect(monitoredApps, lifecycle) {
        val monitored = monitoredApps ?: return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                apps = withContext(Dispatchers.IO) { loadAppsToday(usageRepository, monitored) }
                delay(REFRESH_MILLIS)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Text(
                text = stringResource(R.string.home_app_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        }

        TodayRing(
            totalMinutes = apps?.sumOf { it.minutesToday },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        MonitoringCard(
            isMonitoring = isMonitoring,
            onToggle = { on -> if (on) monitorController.start() else monitorController.stop() },
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.home_your_apps),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
            MonitoredAppsCard(apps)
        }

        Button(
            onClick = onOpenMonitoredApps,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(stringResource(R.string.home_choose_apps))
        }

        if (onOpenDeveloperTools != null) {
            TextButton(
                onClick = onOpenDeveloperTools,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = stringResource(R.string.home_developer_tools),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** One usage query for all apps, most-used first. */
private fun loadAppsToday(usageRepository: UsageRepository, monitored: List<MonitoredApp>): List<AppToday> {
    val minutesByPackage = usageRepository.todayUsage().associate { it.packageName to it.minutes }
    return monitored.map { app ->
        val shortForm = ShortFormApps.find(app.packageName)
        AppToday(
            packageName = app.packageName,
            label = shortForm?.label ?: usageRepository.labelFor(app.packageName),
            feedName = shortForm?.feature?.let { shortForm.feedName },
            minutesToday = minutesByPackage[app.packageName] ?: 0,
            limitSeconds = app.limitSeconds,
        )
    }.sortedByDescending { it.minutesToday }
}

private const val REFRESH_MILLIS = 30_000L
