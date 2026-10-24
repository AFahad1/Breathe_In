package com.breathein.feature.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTrackerTest {

    private val tracker = SessionTracker(packageName = "com.instagram.android", gapMillis = 30_000)

    /** Polls once a second from [fromMillis] (inclusive) to [toMillis] (inclusive); true if any poll fired. */
    private fun poll(fromMillis: Long, toMillis: Long, inTarget: Boolean, thresholdMillis: Long = THRESHOLD): Boolean {
        var fired = false
        for (now in fromMillis..toMillis step 1_000) {
            if (tracker.onTick(now, inTarget, thresholdMillis)) fired = true
        }
        return fired
    }

    @Test
    fun `session starts when target is in view`() {
        assertNull(tracker.state)

        poll(0, 3_000, inTarget = true)

        assertEquals(3_000L, tracker.state?.elapsedMillis)
    }

    @Test
    fun `nothing starts while target is out of view`() {
        poll(0, 5_000, inTarget = false)

        assertNull(tracker.state)
    }

    @Test
    fun `time away from the target does not count`() {
        // Reels 5 s, home feed 5 s, back to Reels: only Reels time adds up.
        poll(0, 5_000, inTarget = true)
        poll(6_000, 10_000, inTarget = false)
        poll(11_000, 13_000, inTarget = true)

        assertEquals(7_000L, tracker.state?.elapsedMillis)
    }

    @Test
    fun `interrupts after 10 seconds of total feed time split across visits`() {
        val limit = 10_000L
        assertFalse(poll(0, 5_000, inTarget = true, thresholdMillis = limit))
        assertFalse(poll(6_000, 10_000, inTarget = false, thresholdMillis = limit))
        assertFalse(poll(11_000, 15_000, inTarget = true, thresholdMillis = limit))
        assertTrue(poll(16_000, 16_000, inTarget = true, thresholdMillis = limit))
    }

    @Test
    fun `short break keeps the session`() {
        poll(0, 5_000, inTarget = true)
        poll(6_000, 30_000, inTarget = false)

        assertNotNull(tracker.state)
    }

    @Test
    fun `session ends after more than 30 seconds away`() {
        poll(0, 5_000, inTarget = true)
        poll(6_000, 36_000, inTarget = false)

        assertNull(tracker.state)

        // Coming back starts a fresh session from zero.
        poll(40_000, 40_000, inTarget = true)
        assertEquals(0L, tracker.state?.elapsedMillis)
    }

    @Test
    fun `fires once when threshold is reached`() {
        assertFalse(poll(0, 59_000, inTarget = true))
        assertTrue(poll(60_000, 60_000, inTarget = true))
        assertFalse(poll(61_000, 600_000, inTarget = true))
    }

    @Test
    fun `fires after 10 seconds with a 10 second limit`() {
        assertFalse(poll(0, 9_000, inTarget = true, thresholdMillis = 10_000))
        assertTrue(poll(10_000, 10_000, inTarget = true, thresholdMillis = 10_000))
    }

    @Test
    fun `keep scrolling resets the timer for another full threshold`() {
        assertTrue(poll(0, 60_000, inTarget = true))

        tracker.onKeepScrolling(nowMillis = 90_000)

        assertNotNull(tracker.state)
        assertFalse(poll(91_000, 149_000, inTarget = true))
        assertTrue(poll(150_000, 150_000, inTarget = true))
    }

    @Test
    fun `ending the session allows a new interruption in the next session`() {
        assertTrue(poll(0, 60_000, inTarget = true))

        tracker.endSession()

        assertNull(tracker.state)
        assertTrue(poll(100_000, 160_000, inTarget = true))
    }

    private companion object {
        const val THRESHOLD = 60_000L
    }
}
