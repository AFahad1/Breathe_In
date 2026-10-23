package com.breathein.core.common

import com.breathein.core.model.AppUsage

/** Read-only access to app usage data. Implemented by :feature:usage. Calls block, so use a background thread. */
interface UsageRepository {
    /** Package of the app currently in the foreground, or null if unknown. */
    fun currentForegroundPackage(): String?

    /** Per-app foreground minutes since local midnight, most used first. */
    fun todayUsage(): List<AppUsage>

    fun todayMinutesFor(packageName: String): Int
}
