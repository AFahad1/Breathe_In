package com.breathein.feature.home

/** One monitored app as the home screen shows it. */
internal data class AppToday(
    val packageName: String,
    val label: String,
    /** "Reels", "Shorts"… for short-form apps, null for whole-app monitoring. */
    val feedName: String?,
    val minutesToday: Int,
    val limitSeconds: Int,
)
