package com.breathein.feature.monitor

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.breathein.core.common.MonitorController
import com.breathein.core.model.SessionState
import com.breathein.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** [MonitorController] that starts/stops [MonitorService] and remembers the choice in settings. */
class ServiceMonitorController(
    context: Context,
    private val sessionMonitor: SessionMonitor,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) : MonitorController {

    private val appContext = context.applicationContext
    private val serviceIntent get() = Intent(appContext, MonitorService::class.java)

    override val isRunning: StateFlow<Boolean> get() = sessionMonitor.isRunning
    override val sessions: StateFlow<List<SessionState>> get() = sessionMonitor.sessions

    override fun start() {
        scope.launch { settingsRepository.setMonitoringEnabled(true) }
        try {
            ContextCompat.startForegroundService(appContext, serviceIntent)
        } catch (e: IllegalStateException) {
            // Android 12+ refuses to start foreground services from the background.
            Log.w(TAG, "Could not start monitor service", e)
        }
    }

    override fun stop() {
        scope.launch { settingsRepository.setMonitoringEnabled(false) }
        appContext.stopService(serviceIntent)
    }

    private companion object {
        const val TAG = "MonitorController"
    }
}
