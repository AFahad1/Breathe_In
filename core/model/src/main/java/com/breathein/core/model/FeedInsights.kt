package com.breathein.core.model

/** What the user watched in Reels / Shorts today, by category. */
data class FeedInsights(
    val totalSeconds: Int = 0,
    /** Classified time per category, largest first. */
    val categories: List<CategoryTime> = emptyList(),
    /** Time on items the classifier hasn't sorted yet. */
    val unsortedSeconds: Int = 0,
    /** A short AI-written reflection on today's mix, or null if there isn't one yet. */
    val summary: String? = null,
    /** Most recent first. */
    val recentItems: List<FeedItem> = emptyList(),
    val isSorting: Boolean = false,
    /** False when no API key is configured and categories come from simple keyword matching. */
    val usesAi: Boolean = false,
)

data class CategoryTime(val category: ContentCategory, val seconds: Int)
