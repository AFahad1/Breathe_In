package com.breathein.core.model

/** Everything the interruption UI needs to show itself. */
data class InterruptionRequest(
    val packageName: String,
    val appLabel: String,
    val todayMinutes: Int,
)
