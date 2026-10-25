package com.breathein.feature.insights

import com.breathein.core.model.CategoryTime
import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedItem
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCalculatorTest {

    @Test
    fun itemLastsUntilTheNextOne() {
        val items = listOf(item(first = 0, last = 2_000), item(first = 10_000, last = 12_000))
        assertEquals(listOf(10, 2), InsightsCalculator.durations(items))
    }

    @Test
    fun longPauseBeforeNextItem_endsAtLastSeen() {
        val items = listOf(item(first = 0, last = 8_000), item(first = 30 * 60_000L, last = 30 * 60_000L + 5_000))
        assertEquals(listOf(8, 5), InsightsCalculator.durations(items))
    }

    @Test
    fun build_addsUpTimePerCategory_largestFirst() {
        val items = listOf(
            item(first = 0, last = 0, category = ContentCategory.Memes),
            item(first = 20_000, last = 20_000, category = ContentCategory.Fitness),
            item(first = 50_000, last = 50_000, category = ContentCategory.Fitness),
            item(first = 70_000, last = 75_000, category = null),
        )

        val insights = InsightsCalculator.build(items, summary = null, isSorting = false, usesAi = true)

        assertEquals(75, insights.totalSeconds)
        assertEquals(
            listOf(CategoryTime(ContentCategory.Fitness, 50), CategoryTime(ContentCategory.Memes, 20)),
            insights.categories,
        )
        assertEquals(5, insights.unsortedSeconds)
    }

    @Test
    fun topCategory_onlyCountsThisAppSinceSessionStart() {
        val items = listOf(
            item(first = 0, last = 60_000, category = ContentCategory.News),
            item(first = 100_000, last = 110_000, category = ContentCategory.Cooking),
            item(first = 110_000, last = 112_000, category = ContentCategory.Gaming, packageName = "other"),
        )
        assertEquals(ContentCategory.Cooking, InsightsCalculator.topCategory(items, IG, sinceMillis = 90_000))
    }

    private fun item(first: Long, last: Long, category: ContentCategory? = null, packageName: String = IG) =
        FeedItem(packageName, "text $first", first, last, category)

    private companion object {
        const val IG = "com.instagram.android"
    }
}
