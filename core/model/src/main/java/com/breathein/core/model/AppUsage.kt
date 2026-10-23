package com.breathein.core.model

/** Foreground time for one app since local midnight. */
data class AppUsage(
    val packageName: String,
    val label: String,
    val minutes: Int,
)
