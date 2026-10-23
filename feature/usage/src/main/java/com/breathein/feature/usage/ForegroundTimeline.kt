package com.breathein.feature.usage

/** Result of replaying foreground events over a time window. */
internal data class ForegroundTimeline(
    /** Foreground milliseconds per package within the window. */
    val millisByPackage: Map<String, Long>,
    /** App still in the foreground at the end of the window, if any. */
    val foregroundPackage: String?,
)

/**
 * Replays events in timestamp order. Only one app is in the foreground at a time:
 * a resume of another package closes the current one, and a pause only closes it if it is
 * the activity that was last resumed (so activity-to-activity hops inside one app don't count as leaving).
 */
internal fun buildForegroundTimeline(
    events: List<ForegroundEvent>,
    windowStartMillis: Long,
    windowEndMillis: Long,
): ForegroundTimeline {
    val totals = HashMap<String, Long>()
    var currentPackage: String? = null
    var currentClass: String? = null
    var currentSince = 0L

    fun closeCurrent(atMillis: Long) {
        val pkg = currentPackage ?: return
        totals[pkg] = (totals[pkg] ?: 0L) + (atMillis - currentSince).coerceAtLeast(0L)
        currentPackage = null
        currentClass = null
    }

    events.forEachIndexed { index, event ->
        when (event.type) {
            ForegroundEvent.Type.Resumed -> {
                if (event.packageName != currentPackage) {
                    closeCurrent(event.timestampMillis)
                    currentPackage = event.packageName
                    currentSince = event.timestampMillis
                }
                currentClass = event.className
            }
            ForegroundEvent.Type.Paused -> when {
                event.packageName == currentPackage && event.className == currentClass ->
                    closeCurrent(event.timestampMillis)
                // A pause before any resume means the app was already open when the window started.
                index == 0 && currentPackage == null ->
                    totals[event.packageName] = event.timestampMillis - windowStartMillis
            }
        }
    }

    val stillForeground = currentPackage
    closeCurrent(windowEndMillis)
    return ForegroundTimeline(totals, stillForeground)
}
