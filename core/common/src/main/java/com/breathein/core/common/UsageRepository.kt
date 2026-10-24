package com.breathein.core.common

import com.breathein.core.model.AppInfo
import com.breathein.core.model.AppUsage

/** Read-only access to app usage data. Implemented by :feature:usage. Calls block, so use a background thread. */
interface UsageRepository {
    /** Package of the app currently in the foreground, or null if unknown. */
    fun currentForegroundPackage(): String?

    /** Per-app foreground minutes since local midnight, most used first. */
    fun todayUsage(): List<AppUsage>

    fun todayMinutesFor(packageName: String): Int

    /** Apps with a launcher icon, excluding Breathe In, sorted by name. */
    fun launchableApps(): List<AppInfo>

    /** Human-readable name, or the package name if the app isn't visible to us. */
    fun labelFor(packageName: String): String
}
