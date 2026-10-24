package com.breathein.feature.monitor

import com.breathein.core.common.SESSION_GAP_MILLIS
import com.breathein.core.model.SessionState

/**
 * Times sessions for one monitored app. Pure Kotlin: the caller passes the time in, and whether the
 * user is "in" the target right now (the app, or its short-form feed).
 *
 * Only time in the target counts: Reels 5 s, home feed 5 s, Reels again = 5 s so far, not 10.
 * A session ends once the target has been out of view for more than [gapMillis], which resets the
 * total. The interruption fires once per session, and again only after [onKeepScrolling].
 */
class SessionTracker(
    private val packageName: String,
    private val gapMillis: Long = SESSION_GAP_MILLIS,
) {
    private var inSession = false
    private var activeMillis = 0L
    private var lastTickMillis: Long? = null
    private var lastTickInTarget = false
    private var lastSeenMillis = 0L
    private var interruptionFired = false

    /** The session in progress, or null when there is none. */
    var state: SessionState? = null
        private set

    /** Records one poll. Returns true if the interruption should be shown now. */
    fun onTick(nowMillis: Long, inTarget: Boolean, thresholdMillis: Long): Boolean {
        // Time between two polls counts only if the user was in the target at both ends of it.
        val previousTick = lastTickMillis
        if (inSession && inTarget && lastTickInTarget && previousTick != null) {
            activeMillis += nowMillis - previousTick
        }
        lastTickMillis = nowMillis
        lastTickInTarget = inTarget

        if (inTarget) {
            inSession = true
            lastSeenMillis = nowMillis
        } else if (inSession && nowMillis - lastSeenMillis > gapMillis) {
            endSession()
        }

        if (!inSession) return false
        val shouldInterrupt = inTarget && !interruptionFired && activeMillis >= thresholdMillis
        if (shouldInterrupt) interruptionFired = true
        state = SessionState(packageName, activeMillis, interruptionFired)
        return shouldInterrupt
    }

    /** The user chose to keep going: start counting again so the next interruption is a full threshold away. */
    fun onKeepScrolling(nowMillis: Long) {
        activeMillis = 0L
        lastTickMillis = nowMillis
        if (inSession) {
            lastSeenMillis = nowMillis
            state = SessionState(packageName)
        }
        interruptionFired = false
    }

    fun endSession() {
        inSession = false
        activeMillis = 0L
        interruptionFired = false
        state = null
    }
}
