package com.breathein.feature.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTrackerTest {

    private val tracker = SessionTracker(targetPackage = TARGET, gapMillis = 30_000)

    @Test
    fun `session starts when target app is in foreground`() {
        assertFalse(tracker.state.isActive)

        tracker.onTick(nowMillis = 1_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        tracker.onTick(nowMillis = 4_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)

        assertTrue(tracker.state.isActive)
        assertEquals(3_000, tracker.state.elapsedMillis)
    }

    @Test
    fun `other apps do not start a session`() {
        tracker.onTick(nowMillis = 1_000, foregroundPackage = OTHER, thresholdMillis = THRESHOLD)

        assertFalse(tracker.state.isActive)
    }

    @Test
    fun `short trip away keeps the session`() {
        tracker.onTick(nowMillis = 0, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        tracker.onTick(nowMillis = 30_000, foregroundPackage = OTHER, thresholdMillis = THRESHOLD)

        assertTrue(tracker.state.isActive)
    }

    @Test
    fun `session ends after more than 30 seconds away`() {
        tracker.onTick(nowMillis = 0, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        tracker.onTick(nowMillis = 30_001, foregroundPackage = OTHER, thresholdMillis = THRESHOLD)

        assertFalse(tracker.state.isActive)

        // Coming back starts a fresh session.
        tracker.onTick(nowMillis = 40_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        assertEquals(0, tracker.state.elapsedMillis)
    }

    @Test
    fun `fires once when threshold is reached`() {
        assertFalse(tracker.onTick(nowMillis = 0, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
        assertFalse(tracker.onTick(nowMillis = 57_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
        assertTrue(tracker.onTick(nowMillis = 60_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
        assertFalse(tracker.onTick(nowMillis = 63_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
        assertFalse(tracker.onTick(nowMillis = 600_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
    }

    @Test
    fun `does not fire while another app is in front`() {
        tracker.onTick(nowMillis = 0, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        tracker.onTick(nowMillis = 50_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)

        assertFalse(tracker.onTick(nowMillis = 61_000, foregroundPackage = OTHER, thresholdMillis = THRESHOLD))
        assertTrue(tracker.onTick(nowMillis = 64_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
    }

    @Test
    fun `keep scrolling resets the timer for another full threshold`() {
        tracker.onTick(nowMillis = 0, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        assertTrue(tracker.onTick(nowMillis = 60_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))

        tracker.onKeepScrolling(nowMillis = 90_000)

        assertTrue(tracker.state.isActive)
        assertFalse(tracker.onTick(nowMillis = 149_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
        assertTrue(tracker.onTick(nowMillis = 150_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
    }

    @Test
    fun `ending the session allows a new interruption in the next session`() {
        tracker.onTick(nowMillis = 0, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        assertTrue(tracker.onTick(nowMillis = 60_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))

        tracker.endSession()

        assertFalse(tracker.state.isActive)
        tracker.onTick(nowMillis = 100_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD)
        assertTrue(tracker.onTick(nowMillis = 160_000, foregroundPackage = TARGET, thresholdMillis = THRESHOLD))
    }

    private companion object {
        const val TARGET = "com.instagram.android"
        const val OTHER = "com.android.launcher"
        const val THRESHOLD = 60_000L
    }
}
