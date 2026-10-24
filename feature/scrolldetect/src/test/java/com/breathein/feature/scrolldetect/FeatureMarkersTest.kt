package com.breathein.feature.scrolldetect

import com.breathein.core.model.ScrollFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeatureMarkersTest {

    @Test
    fun `youtube shorts player is detected`() {
        val ids = setOf("com.google.android.youtube:id/reel_recycler")

        assertEquals(ScrollFeature.YouTubeShorts, FeatureMarkers.match("com.google.android.youtube", ids))
    }

    @Test
    fun `instagram reels viewer is detected`() {
        val ids = setOf("com.instagram.android:id/clips_viewer_view_pager")

        assertEquals(ScrollFeature.InstagramReels, FeatureMarkers.match("com.instagram.android", ids))
    }

    @Test
    fun `leftover shorts time bar on the home tab is not detected`() {
        val ids = setOf("com.google.android.youtube:id/reel_time_bar")

        assertNull(FeatureMarkers.match("com.google.android.youtube", ids))
    }

    @Test
    fun `other screens are not detected`() {
        assertNull(FeatureMarkers.match("com.google.android.youtube", setOf("com.google.android.youtube:id/results")))
    }

    @Test
    fun `markers only count in their own app`() {
        val ids = setOf("com.google.android.youtube:id/reel_recycler")

        assertNull(FeatureMarkers.match("com.instagram.android", ids))
    }
}
