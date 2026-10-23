package com.breathein.feature.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

/** Reads activity resume/pause events from [UsageStatsManager]. Returns nothing if Usage Access is missing. */
internal class UsageEventReader(context: Context) {

    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)

    fun read(startMillis: Long, endMillis: Long): List<ForegroundEvent> {
        val manager = usageStatsManager ?: return emptyList()
        val events = try {
            manager.queryEvents(startMillis, endMillis)
        } catch (e: SecurityException) {
            Log.w(TAG, "Usage Access not granted", e)
            return emptyList()
        } ?: return emptyList()

        val result = mutableListOf<ForegroundEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                ACTIVITY_RESUMED -> ForegroundEvent.Type.Resumed
                ACTIVITY_PAUSED -> ForegroundEvent.Type.Paused
                else -> continue
            }
            result += ForegroundEvent(event.packageName, event.className.orEmpty(), event.timeStamp, type)
        }
        return result
    }

    private companion object {
        const val TAG = "UsageEventReader"

        // UsageEvents.Event.ACTIVITY_RESUMED / ACTIVITY_PAUSED (API 29). Same values as the older
        // MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND, so these work back to our min SDK.
        const val ACTIVITY_RESUMED = 1
        const val ACTIVITY_PAUSED = 2
    }
}
