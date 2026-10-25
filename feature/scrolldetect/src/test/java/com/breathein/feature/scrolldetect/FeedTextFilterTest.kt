package com.breathein.feature.scrolldetect

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedTextFilterTest {

    @Test
    fun keepsCaptionAndCreator_dropsButtonsAndCounters() {
        val visible = listOf("Like", "12.4K", "3 comments", "Share", "gymbro_mike", "Leg day   finisher #legday", "Follow")
        assertEquals(listOf("gymbro_mike", "Leg day finisher #legday"), FeedTextFilter.keep(visible))
    }

    @Test
    fun dropsDuplicatesAndTooShortText() {
        assertEquals(listOf("pasta night"), FeedTextFilter.keep(listOf("ok", "pasta night", "pasta night", "")))
    }
}
