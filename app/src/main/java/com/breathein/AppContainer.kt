package com.breathein

import android.content.Context
import com.breathein.core.common.CheckInLauncher
import com.breathein.core.common.EventLog
import com.breathein.core.common.InterruptionTrigger
import com.breathein.core.common.MonitorController
import com.breathein.core.common.NoOpCheckInLauncher
import com.breathein.core.common.NoOpEventLog
import com.breathein.core.common.UsageRepository
import com.breathein.core.permissions.PermissionChecker
import com.breathein.core.settings.SettingsRepository
import com.breathein.feature.monitor.ServiceMonitorController
import com.breathein.feature.monitor.SessionMonitor
import com.breathein.feature.overlay.OverlayInterruptionTrigger
import com.breathein.feature.usage.UsageStatsUsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency injection: creates every app-wide object once and wires features to each other. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val permissionChecker = PermissionChecker(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val usageRepository: UsageRepository = UsageStatsUsageRepository(appContext)
    val interruptionTrigger: InterruptionTrigger = OverlayInterruptionTrigger(appContext)

    // TODO(check-in): replace with the real check-in once it exists.
    private val checkInLauncher: CheckInLauncher = NoOpCheckInLauncher

    // TODO(event-log): replace with a database-backed log.
    private val eventLog: EventLog = NoOpEventLog

    val sessionMonitor = SessionMonitor(
        context = appContext,
        usageRepository = usageRepository,
        interruptionTrigger = interruptionTrigger,
        settingsRepository = settingsRepository,
        permissionChecker = permissionChecker,
        checkInLauncher = checkInLauncher,
        eventLog = eventLog,
    )

    val monitorController: MonitorController =
        ServiceMonitorController(appContext, sessionMonitor, settingsRepository, applicationScope)
}
