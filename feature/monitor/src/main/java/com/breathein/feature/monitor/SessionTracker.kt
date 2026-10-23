package com.breathein.feature.monitor

import com.breathein.core.common.SESSION_GAP_MILLIS
import com.breathein.core.model.SessionState

/**
 * Times continuous sessions in [targetPackage]. Pure Kotlin: the caller passes the time in.
 *
 * A session starts when the target app is seen in the foreground and ends once it has been out of
 * the foreground for more than [gapMillis]. Shorter trips away don't break the session (and count
 * towards it). The interruption fires once per session, and again only after [onKeepScrolling].
 */
class SessionTracker(
    private val targetPackage: String,
    private val gapMillis: Long = SESSION_GAP_MILLIS,
) {
    private var sessionStartMillis: Long? = null
    private var lastSeenMillis = 0L
    private var interruptionFired = false

    var state: SessionState = SessionState.Idle
        private set

    /** Records one poll. Returns true if the interruption should be shown now. */
    fun onTick(nowMillis: Long, foregroundPackage: String?, thresholdMillis: Long): Boolean {
        val inTarget = foregroundPackage == targetPackage
        if (inTarget) {
            if (sessionStartMillis == null) sessionStartMillis = nowMillis
            lastSeenMillis = nowMillis
        } else if (sessionStartMillis != null && nowMillis - lastSeenMillis > gapMillis) {
            endSession()
        }

        val start = sessionStartMillis ?: return false
        val elapsed = nowMillis - start
        val shouldInterrupt = inTarget && !interruptionFired && elapsed >= thresholdMillis
        if (shouldInterrupt) interruptionFired = true
        state = SessionState(isActive = true, elapsedMillis = elapsed, interruptionShown = interruptionFired)
        return shouldInterrupt
    }

    /** The user chose to keep going: restart the clock so the next interruption is a full threshold away. */
    fun onKeepScrolling(nowMillis: Long) {
        if (sessionStartMillis != null) {
            sessionStartMillis = nowMillis
            lastSeenMillis = nowMillis
            state = SessionState(isActive = true)
        }
        interruptionFired = false
    }

    fun endSession() {
        sessionStartMillis = null
        interruptionFired = false
        state = SessionState.Idle
    }
}
