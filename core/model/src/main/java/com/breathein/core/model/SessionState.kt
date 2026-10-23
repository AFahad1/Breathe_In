package com.breathein.core.model

/** Live state of the monitored app session, published by the monitor for the debug screen. */
data class SessionState(
    val isActive: Boolean = false,
    val elapsedMillis: Long = 0L,
    val interruptionShown: Boolean = false,
) {
    val elapsedSeconds: Long get() = elapsedMillis / 1_000

    companion object {
        val Idle = SessionState()
    }
}
