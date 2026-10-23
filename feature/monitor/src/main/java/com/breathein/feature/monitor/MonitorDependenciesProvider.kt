package com.breathein.feature.monitor

/**
 * Implemented by the Application so [MonitorService], which the system creates, can find its
 * dependencies without a DI framework.
 */
interface MonitorDependenciesProvider {
    val sessionMonitor: SessionMonitor
}
