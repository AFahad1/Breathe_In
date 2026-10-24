package com.breathein.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.breathein.core.common.INSTAGRAM_PACKAGE
import com.breathein.core.common.InterruptionTrigger
import com.breathein.core.common.MonitorController
import com.breathein.core.common.POLL_INTERVAL_MILLIS
import com.breathein.core.common.ScrollFeatureDetector
import com.breathein.core.common.UsageRepository
import com.breathein.core.common.goToHomeScreen
import com.breathein.core.model.AppUsage
import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest
import com.breathein.core.permissions.PermissionChecker
import com.breathein.core.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Developer screen for checking that permissions, usage data, detection, monitoring and the overlay all work. */
@Composable
fun DebugScreen(
    permissionChecker: PermissionChecker,
    usageRepository: UsageRepository,
    settingsRepository: SettingsRepository,
    scrollFeatureDetector: ScrollFeatureDetector,
    monitorController: MonitorController,
    interruptionTrigger: InterruptionTrigger,
    onOpenMonitoredApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()

    val isMonitoring by monitorController.isRunning.collectAsStateWithLifecycle()
    val sessions by monitorController.sessions.collectAsStateWithLifecycle()
    val monitoredApps by settingsRepository.monitoredApps.collectAsStateWithLifecycle(initialValue = emptyList())
    val detectorConnected by scrollFeatureDetector.isConnected.collectAsStateWithLifecycle()
    val activeFeature by scrollFeatureDetector.activeFeature.collectAsStateWithLifecycle()
    val viewIdCapture by scrollFeatureDetector.lastViewIdCapture.collectAsStateWithLifecycle()

    var permissions by remember { mutableStateOf(PermissionSnapshot.from(permissionChecker)) }
    var foregroundPackage by remember { mutableStateOf<String?>(null) }
    var todayUsage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }

    fun refreshUsage() {
        scope.launch { todayUsage = withContext(Dispatchers.IO) { usageRepository.todayUsage() } }
    }

    fun testOverlay(delayMillis: Long) {
        scope.launch {
            delay(delayMillis)
            val (label, minutes) = withContext(Dispatchers.IO) {
                usageRepository.labelFor(INSTAGRAM_PACKAGE) to usageRepository.todayMinutesFor(INSTAGRAM_PACKAGE)
            }
            interruptionTrigger.show(InterruptionRequest(INSTAGRAM_PACKAGE, label, minutes)) { outcome ->
                if (outcome == InterruptionOutcome.ImDone) context.goToHomeScreen()
            }
        }
    }

    fun captureViewIds() {
        scope.launch {
            delay(CAPTURE_DELAY_MILLIS)
            scrollFeatureDetector.requestViewIdCapture()
        }
    }

    LifecycleResumeEffect(permissionChecker) {
        permissions = PermissionSnapshot.from(permissionChecker)
        refreshUsage()
        onPauseOrDispose { }
    }

    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                foregroundPackage = withContext(Dispatchers.IO) { usageRepository.currentForegroundPackage() }
                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { PermissionsSection(permissions) }
        item {
            MonitoringSection(
                isMonitoring = isMonitoring,
                sessions = sessions,
                foregroundPackage = foregroundPackage,
                labelFor = usageRepository::labelFor,
                onStart = monitorController::start,
                onStop = monitorController::stop,
            )
        }
        item { MonitoredAppsSection(monitoredAppCount = monitoredApps.size, onOpen = onOpenMonitoredApps) }
        item {
            DetectionSection(
                connected = detectorConnected,
                activeFeature = activeFeature,
                capture = viewIdCapture,
                onCapture = ::captureViewIds,
            )
        }
        item { OverlayTestSection(onTestNow = { testOverlay(0) }, onTestDelayed = { testOverlay(TEST_DELAY_MILLIS) }) }
        item { UsageHeader(onRefresh = ::refreshUsage) }
        items(todayUsage.take(TOP_APPS), key = { it.packageName }) { UsageRow(it) }
    }
}

private const val TOP_APPS = 10
private const val TEST_DELAY_MILLIS = 5_000L
private const val CAPTURE_DELAY_MILLIS = 5_000L
