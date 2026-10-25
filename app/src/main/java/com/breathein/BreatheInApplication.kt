package com.breathein

import android.app.Application
import com.breathein.core.common.FeedContentSink
import com.breathein.feature.monitor.MonitorDependenciesProvider
import com.breathein.feature.monitor.SessionMonitor
import com.breathein.feature.scrolldetect.AccessibilityScrollFeatureDetector
import com.breathein.feature.scrolldetect.ScrollDetectionDependenciesProvider

class BreatheInApplication : Application(), MonitorDependenciesProvider, ScrollDetectionDependenciesProvider {

    lateinit var container: AppContainer
        private set

    override val sessionMonitor: SessionMonitor get() = container.sessionMonitor
    override val scrollFeatureDetector: AccessibilityScrollFeatureDetector get() = container.scrollFeatureDetector
    override val feedContentSink: FeedContentSink get() = container.contentInsights

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
