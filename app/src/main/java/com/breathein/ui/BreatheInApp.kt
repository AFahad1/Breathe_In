package com.breathein.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.breathein.AppContainer
import com.breathein.feature.apps.MonitoredAppsScreen
import com.breathein.feature.debug.DebugScreen
import com.breathein.feature.onboarding.OnboardingScreen

private enum class Screen { Onboarding, Debug, MonitoredApps }

/** Top-level navigation: onboarding while any permission is missing, otherwise the debug screen. */
@Composable
fun BreatheInApp(container: AppContainer) {
    val permissionChecker = container.permissionChecker
    var screen by rememberSaveable {
        mutableStateOf(if (permissionChecker.allGranted()) Screen.Debug else Screen.Onboarding)
    }

    // A permission can be revoked in Settings at any time; send the user back to onboarding.
    LifecycleResumeEffect(permissionChecker) {
        if (!permissionChecker.allGranted()) screen = Screen.Onboarding
        onPauseOrDispose { }
    }

    BackHandler(enabled = screen == Screen.MonitoredApps) { screen = Screen.Debug }

    Scaffold { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (screen) {
            Screen.Onboarding -> OnboardingScreen(
                permissionChecker = permissionChecker,
                onStartMonitoring = {
                    container.monitorController.start()
                    screen = Screen.Debug
                },
                modifier = modifier,
            )
            Screen.Debug -> DebugScreen(
                permissionChecker = permissionChecker,
                usageRepository = container.usageRepository,
                settingsRepository = container.settingsRepository,
                scrollFeatureDetector = container.scrollFeatureDetector,
                monitorController = container.monitorController,
                interruptionTrigger = container.interruptionTrigger,
                onOpenMonitoredApps = { screen = Screen.MonitoredApps },
                modifier = modifier,
            )
            Screen.MonitoredApps -> MonitoredAppsScreen(
                usageRepository = container.usageRepository,
                settingsRepository = container.settingsRepository,
                onDone = { screen = Screen.Debug },
                modifier = modifier,
            )
        }
    }
}
