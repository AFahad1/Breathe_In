package com.breathein.core.model

/** Live state of one monitored app's session, published by the monitor for the debug screen. */
data class SessionState(
    val packageName: String,
    val elapsedMillis: Long = 0L,
    val interruptionShown: Boolean = false,
) {
    val elapsedSeconds: Long get() = elapsedMillis / 1_000
}
