package com.breathein.core.model

/** An app the user wants interrupted after [limitSeconds] of continuous use. */
data class MonitoredApp(
    val packageName: String,
    val limitSeconds: Int,
)
