package com.breathein

import android.app.Application
import com.breathein.feature.monitor.MonitorDependenciesProvider
import com.breathein.feature.monitor.SessionMonitor

class BreatheInApplication : Application(), MonitorDependenciesProvider {

    lateinit var container: AppContainer
        private set

    override val sessionMonitor: SessionMonitor get() = container.sessionMonitor

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
