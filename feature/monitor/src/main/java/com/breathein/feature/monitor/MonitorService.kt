package com.breathein.feature.monitor

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope

/** Foreground service that keeps [SessionMonitor] running while the user is in other apps. */
class MonitorService : LifecycleService() {

    private var monitor: SessionMonitor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startInForeground()

        val provider = application as? MonitorDependenciesProvider
        if (provider == null) {
            Log.e(TAG, "Application does not implement MonitorDependenciesProvider")
            stopSelf()
            return START_NOT_STICKY
        }
        monitor = provider.sessionMonitor.also { sessionMonitor ->
            sessionMonitor.start(lifecycleScope) {
                Log.w(TAG, "Permission revoked; stopping monitoring")
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        monitor?.stop()
        monitor = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, MonitorNotification.ID, MonitorNotification.build(this), type)
    }

    private companion object {
        const val TAG = "MonitorService"
    }
}
