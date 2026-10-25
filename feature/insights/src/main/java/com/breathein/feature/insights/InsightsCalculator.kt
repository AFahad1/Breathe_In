package com.breathein.feature.insights

import com.breathein.core.model.CategoryTime
import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedInsights
import com.breathein.core.model.FeedItem

/** Pure maths over the watched items: how long each was on screen and how that adds up per category. */
internal object InsightsCalculator {

    /**
     * Seconds each item was on screen. An item lasts until the next one appears, unless there was a long
     * pause in between (the user left the feed), in which case it ends when it was last seen.
     */
    fun durations(items: List<FeedItem>): List<Int> = items.mapIndexed { index, item ->
        val next = items.getOrNull(index + 1)
        val end = next?.firstSeenMillis?.takeIf { it - item.lastSeenMillis <= IDLE_GAP_MILLIS } ?: item.lastSeenMillis
        ((end - item.firstSeenMillis) / 1_000).toInt().coerceIn(MIN_ITEM_SECONDS, MAX_ITEM_SECONDS)
    }

    fun build(items: List<FeedItem>, summary: String?, isSorting: Boolean, usesAi: Boolean): FeedInsights {
        val timed = items.zip(durations(items))
        return FeedInsights(
            totalSeconds = timed.sumOf { it.second },
            categories = categoryTimes(timed),
            unsortedSeconds = timed.filter { it.first.category == null }.sumOf { it.second },
            summary = summary,
            recentItems = items.takeLast(RECENT_ITEMS).reversed(),
            isSorting = isSorting,
            usesAi = usesAi,
        )
    }

    /** The category (other than Other) with the most time among [packageName]'s items seen since [sinceMillis]. */
    fun topCategory(items: List<FeedItem>, packageName: String, sinceMillis: Long): ContentCategory? {
        val timed = items.zip(durations(items))
            .filter { (item, _) -> item.packageName == packageName && item.lastSeenMillis >= sinceMillis }
        return categoryTimes(timed).firstOrNull { it.category != ContentCategory.Other }?.category
    }

    /** Texts of the items in [category], longest-watched first: examples for the summary. */
    fun examples(items: List<FeedItem>, category: ContentCategory, count: Int): List<String> =
        items.zip(durations(items))
            .filter { it.first.category == category }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first.text }

    private fun categoryTimes(timed: List<Pair<FeedItem, Int>>): List<CategoryTime> =
        timed.mapNotNull { (item, seconds) -> item.category?.let { it to seconds } }
            .groupBy({ it.first }, { it.second })
            .map { (category, seconds) -> CategoryTime(category, seconds.sum()) }
            .sortedByDescending { it.seconds }

    private const val IDLE_GAP_MILLIS = 60_000L
    private const val MIN_ITEM_SECONDS = 1
    private const val MAX_ITEM_SECONDS = 180
    private const val RECENT_ITEMS = 30
}
