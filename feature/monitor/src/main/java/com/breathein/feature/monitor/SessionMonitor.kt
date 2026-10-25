package com.breathein.feature.monitor

import android.content.Context
import android.os.SystemClock
import com.breathein.core.common.CheckInLauncher
import com.breathein.core.common.EventLog
import com.breathein.core.common.InterruptionTrigger
import com.breathein.core.common.POLL_INTERVAL_MILLIS
import com.breathein.core.common.ScrollFeatureDetector
import com.breathein.core.common.ShortFormApps
import com.breathein.core.common.UsageRepository
import com.breathein.core.common.goToHomeScreen
import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest
import com.breathein.core.model.MonitoredApp
import com.breathein.core.model.SessionState
import com.breathein.core.permissions.PermissionChecker
import com.breathein.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The polling loop: every few seconds, works out which monitored app (or feed) is in view, feeds one
 * [SessionTracker] per app and fires the interruption. Runs inside [MonitorService].
 * Tracker state is only touched on the main thread.
 */
class SessionMonitor(
    context: Context,
    private val usageRepository: UsageRepository,
    private val scrollFeatureDetector: ScrollFeatureDetector,
    private val interruptionTrigger: InterruptionTrigger,
    private val settingsRepository: SettingsRepository,
    private val permissionChecker: PermissionChecker,
    private val checkInLauncher: CheckInLauncher,
    private val eventLog: EventLog,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val appContext = context.applicationContext
    private val trackers = mutableMapOf<String, SessionTracker>()
    private var loop: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _sessions = MutableStateFlow<List<SessionState>>(emptyList())
    val sessions: StateFlow<List<SessionState>> = _sessions.asStateFlow()

    /** Starts polling in [scope] (which must use the main dispatcher). [onPermissionLost] is called if access is revoked. */
    fun start(scope: CoroutineScope, onPermissionLost: () -> Unit) {
        if (loop?.isActive == true) return
        _isRunning.value = true
        loop = scope.launch {
            while (isActive) {
                if (!permissionChecker.hasUsageAccess() || !permissionChecker.canDrawOverlays()) {
                    onPermissionLost()
                    break
                }
                tick()
                delay(POLL_INTERVAL_MILLIS)
            }
        }
    }

    fun stop() {
        loop?.cancel()
        loop = null
        trackers.clear()
        publishSessions()
        _isRunning.value = false
    }

    private suspend fun tick() {
        val foreground = withContext(Dispatchers.IO) { usageRepository.currentForegroundPackage() }
        val monitoredApps = settingsRepository.monitoredApps.first()
        val now = clock()

        trackers.keys.retainAll(monitoredApps.mapTo(HashSet()) { it.packageName })
        var toInterrupt: MonitoredApp? = null
        for (app in monitoredApps) {
            val tracker = trackers.getOrPut(app.packageName) { SessionTracker(app.packageName) }
            val fired = tracker.onTick(now, isInTarget(app.packageName, foreground), app.limitSeconds * 1_000L)
            if (fired && toInterrupt == null) toInterrupt = app
        }
        publishSessions()
        toInterrupt?.let { showInterruption(it.packageName) }
    }

    /** In the app, and (for Instagram/YouTube) in its short-form feed rather than elsewhere in the app. */
    private fun isInTarget(packageName: String, foreground: String?): Boolean {
        if (foreground != packageName) return false
        val requiredFeature = ShortFormApps.requiredFeature(packageName) ?: return true
        return scrollFeatureDetector.activeFeature.value == requiredFeature
    }

    private suspend fun showInterruption(packageName: String) {
        val (label, todayMinutes) = withContext(Dispatchers.IO) {
            usageRepository.labelFor(packageName) to usageRepository.todayMinutesFor(packageName)
        }
        val shortForm = ShortFormApps.find(packageName)
        val request = InterruptionRequest(
            packageName = packageName,
            appLabel = shortForm?.label ?: label,
            todayMinutes = todayMinutes,
            feedName = shortForm?.feature?.let { shortForm.feedName },
            scrolledSeconds = ((trackers[packageName]?.state?.elapsedMillis ?: 0L) / 1_000).toInt(),
        )
        eventLog.interruptionShown(request)
        interruptionTrigger.show(request) { outcome -> onOutcome(packageName, outcome) }
    }

    private fun onOutcome(packageName: String, outcome: InterruptionOutcome) {
        eventLog.interruptionFinished(outcome)
        val tracker = trackers[packageName]
        when (outcome) {
            InterruptionOutcome.KeepScrolling -> tracker?.onKeepScrolling(clock())
            InterruptionOutcome.ImDone -> {
                tracker?.endSession()
                appContext.goToHomeScreen()
                // TODO(check-in): currently a no-op; plug the check-in in via AppContainer.
                checkInLauncher.launch()
            }
        }
        publishSessions()
    }

    private fun publishSessions() {
        _sessions.value = trackers.values.mapNotNull { it.state }
    }
}
