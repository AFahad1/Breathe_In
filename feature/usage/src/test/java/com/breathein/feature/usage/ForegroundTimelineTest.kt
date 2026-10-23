package com.breathein.feature.usage

import com.breathein.feature.usage.ForegroundEvent.Type.Paused
import com.breathein.feature.usage.ForegroundEvent.Type.Resumed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForegroundTimelineTest {

    @Test
    fun `sums time between resume and pause`() {
        val timeline = buildForegroundTimeline(
            listOf(event(A, "Main", 1_000, Resumed), event(A, "Main", 5_000, Paused)),
            windowStartMillis = 0,
            windowEndMillis = 10_000,
        )

        assertEquals(4_000L, timeline.millisByPackage[A])
        assertNull(timeline.foregroundPackage)
    }

    @Test
    fun `app still open counts until window end and is the foreground app`() {
        val timeline = buildForegroundTimeline(listOf(event(A, "Main", 2_000, Resumed)), 0, 10_000)

        assertEquals(8_000L, timeline.millisByPackage[A])
        assertEquals(A, timeline.foregroundPackage)
    }

    @Test
    fun `hopping between activities of one app keeps it in the foreground`() {
        val timeline = buildForegroundTimeline(
            listOf(
                event(A, "Feed", 0, Resumed),
                event(A, "Reels", 3_000, Resumed),
                event(A, "Feed", 3_100, Paused),
            ),
            windowStartMillis = 0,
            windowEndMillis = 10_000,
        )

        assertEquals(A, timeline.foregroundPackage)
        assertEquals(10_000L, timeline.millisByPackage[A])
    }

    @Test
    fun `switching apps closes the previous one`() {
        val timeline = buildForegroundTimeline(
            listOf(event(A, "Main", 0, Resumed), event(B, "Main", 4_000, Resumed)),
            windowStartMillis = 0,
            windowEndMillis = 10_000,
        )

        assertEquals(4_000L, timeline.millisByPackage[A])
        assertEquals(6_000L, timeline.millisByPackage[B])
        assertEquals(B, timeline.foregroundPackage)
    }

    @Test
    fun `app open before the window started counts from window start`() {
        val timeline = buildForegroundTimeline(listOf(event(A, "Main", 3_000, Paused)), 0, 10_000)

        assertEquals(3_000L, timeline.millisByPackage[A])
    }

    private fun event(pkg: String, cls: String, at: Long, type: ForegroundEvent.Type) =
        ForegroundEvent(pkg, cls, at, type)

    private companion object {
        const val A = "com.instagram.android"
        const val B = "com.android.launcher"
    }
}
