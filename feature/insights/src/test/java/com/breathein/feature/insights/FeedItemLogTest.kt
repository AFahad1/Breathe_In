package com.breathein.feature.insights

import com.breathein.core.model.ContentCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedItemLogTest {

    private val log = FeedItemLog()

    @Test
    fun sameCaption_extendsTheItem() {
        assertTrue(log.record(IG, listOf("mike", "Leg day finisher #legday"), now = 0))
        assertFalse(log.record(IG, listOf("mike", "Leg day finisher #legday"), now = 4_000))

        val item = log.all.single()
        assertEquals("mike · Leg day finisher #legday", item.text)
        assertEquals(4_000L, item.lastSeenMillis)
    }

    @Test
    fun newCaption_startsANewItem() {
        log.record(IG, listOf("Leg day finisher"), now = 0)
        assertTrue(log.record(IG, listOf("15 minute pasta"), now = 6_000))
        assertEquals(2, log.all.size)
    }

    @Test
    fun emptyScreen_isIgnored() {
        assertFalse(log.record(IG, emptyList(), now = 0))
        assertTrue(log.all.isEmpty())
    }

    @Test
    fun applyCategories_matchesItemsByStartTime() {
        log.record(IG, listOf("Leg day finisher"), now = 0)
        log.record(IG, listOf("15 minute pasta"), now = 6_000)
        val items = log.all

        log.applyCategories(items, listOf(ContentCategory.Fitness, null))

        assertEquals(listOf(ContentCategory.Fitness, null), log.all.map { it.category })
    }

    private companion object {
        const val IG = "com.instagram.android"
    }
}
