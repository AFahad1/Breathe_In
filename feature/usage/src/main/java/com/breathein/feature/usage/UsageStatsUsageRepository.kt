package com.breathein.feature.usage

import android.content.Context
import com.breathein.core.common.UsageRepository
import com.breathein.core.model.AppUsage
import java.util.Calendar

/** [UsageRepository] backed by [android.app.usage.UsageStatsManager] events. */
class UsageStatsUsageRepository(context: Context) : UsageRepository {

    private val appContext = context.applicationContext
    private val reader = UsageEventReader(appContext)
    private val labels = AppLabelResolver(appContext)

    @Volatile
    private var lastKnownForeground: String? = null

    override fun currentForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = reader.read(now - FOREGROUND_LOOKBACK_MILLIS, now)
        // No events in the window means nothing changed: whatever was in front is still in front.
        if (events.isNotEmpty()) {
            lastKnownForeground = buildForegroundTimeline(events, now - FOREGROUND_LOOKBACK_MILLIS, now).foregroundPackage
        }
        return lastKnownForeground
    }

    override fun todayUsage(): List<AppUsage> =
        todayMillisByPackage()
            .filterKeys { it != appContext.packageName }
            .map { (pkg, millis) -> AppUsage(pkg, labels.labelFor(pkg), (millis / MILLIS_PER_MINUTE).toInt()) }
            .filter { it.minutes > 0 }
            .sortedByDescending { it.minutes }

    override fun todayMinutesFor(packageName: String): Int =
        ((todayMillisByPackage()[packageName] ?: 0L) / MILLIS_PER_MINUTE).toInt()

    private fun todayMillisByPackage(): Map<String, Long> {
        val now = System.currentTimeMillis()
        val midnight = startOfToday()
        return buildForegroundTimeline(reader.read(midnight, now), midnight, now).millisByPackage
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val FOREGROUND_LOOKBACK_MILLIS = 60 * 60 * 1_000L
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
