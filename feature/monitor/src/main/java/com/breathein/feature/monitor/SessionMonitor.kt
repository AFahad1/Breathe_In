package com.breathein.feature.monitor

import android.content.Context
import android.os.SystemClock
import com.breathein.core.common.CheckInLauncher
import com.breathein.core.common.EventLog
import com.breathein.core.common.INSTAGRAM_LABEL
import com.breathein.core.common.INSTAGRAM_PACKAGE
import com.breathein.core.common.InterruptionTrigger
import com.breathein.core.common.POLL_INTERVAL_MILLIS
import com.breathein.core.common.UsageRepository
import com.breathein.core.common.goToHomeScreen
import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest
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
 * The polling loop: every few seconds, checks the foreground app, feeds [SessionTracker] and fires
 * the interruption. Runs inside [MonitorService]. Tracker state is only touched on the main thread.
 */
class SessionMonitor(
    context: Context,
    private val usageRepository: UsageRepository,
    private val interruptionTrigger: InterruptionTrigger,
    private val settingsRepository: SettingsRepository,
    private val permissionChecker: PermissionChecker,
    private val checkInLauncher: CheckInLauncher,
    private val eventLog: EventLog,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val appContext = context.applicationContext
    private val tracker = SessionTracker(INSTAGRAM_PACKAGE)
    private var loop: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

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
        tracker.endSession()
        _sessionState.value = tracker.state
        _isRunning.value = false
    }

    private suspend fun tick() {
        val foreground = withContext(Dispatchers.IO) { usageRepository.currentForegroundPackage() }
        val thresholdMillis = settingsRepository.thresholdMinutes.first() * MILLIS_PER_MINUTE
        val shouldInterrupt = tracker.onTick(clock(), foreground, thresholdMillis)
        _sessionState.value = tracker.state
        if (shouldInterrupt) showInterruption()
    }

    private suspend fun showInterruption() {
        val todayMinutes = withContext(Dispatchers.IO) { usageRepository.todayMinutesFor(INSTAGRAM_PACKAGE) }
        val request = InterruptionRequest(INSTAGRAM_PACKAGE, INSTAGRAM_LABEL, todayMinutes)
        eventLog.interruptionShown(request)
        interruptionTrigger.show(request, ::onOutcome)
    }

    private fun onOutcome(outcome: InterruptionOutcome) {
        eventLog.interruptionFinished(outcome)
        when (outcome) {
            InterruptionOutcome.KeepScrolling -> tracker.onKeepScrolling(clock())
            InterruptionOutcome.ImDone -> {
                tracker.endSession()
                appContext.goToHomeScreen()
                // TODO(check-in): currently a no-op; plug the check-in in via AppContainer.
                checkInLauncher.launch()
            }
        }
        _sessionState.value = tracker.state
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
