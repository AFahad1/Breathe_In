package com.breathein.core.common

import com.breathein.core.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/** Starts and stops session monitoring. Implemented by :feature:monitor. */
interface MonitorController {
    val isRunning: StateFlow<Boolean>

    /** Sessions currently in progress, one per monitored app. */
    val sessions: StateFlow<List<SessionState>>

    fun start()
    fun stop()
}
