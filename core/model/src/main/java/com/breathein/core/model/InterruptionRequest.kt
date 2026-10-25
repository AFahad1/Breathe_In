package com.breathein.core.model

/** Everything the interruption UI needs to show itself. */
data class InterruptionRequest(
    val packageName: String,
    val appLabel: String,
    val todayMinutes: Int,
    /** "Reels", "Shorts"… when only a feed inside the app is monitored, otherwise null. */
    val feedName: String? = null,
    /** How long the user has been scrolling when the break was triggered. */
    val scrolledSeconds: Int = 0,
)
