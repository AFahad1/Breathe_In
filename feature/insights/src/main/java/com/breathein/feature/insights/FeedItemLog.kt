package com.breathein.feature.insights

import com.breathein.core.model.ContentCategory
import com.breathein.core.model.FeedItem

/**
 * Turns the stream of "text on screen in the feed" snapshots into a list of watched items. A new item
 * starts when the main text (usually the caption) changes. Not thread-safe; the repository locks around it.
 */
internal class FeedItemLog {

    private val items = mutableListOf<FeedItem>()
    private var currentKey: String? = null

    val all: List<FeedItem> get() = items.toList()

    /** Puts items saved earlier today in front of anything recorded since the app started. */
    fun restore(saved: List<FeedItem>) {
        items.addAll(0, saved)
    }

    fun clear() {
        items.clear()
        currentKey = null
    }

    /** Records what's on screen at [now]. Returns true when it is a new item. */
    fun record(packageName: String, texts: List<String>, now: Long): Boolean {
        if (texts.isEmpty()) return false
        val key = keyOf(texts)
        val last = items.lastOrNull()
        if (last != null && key == currentKey && last.packageName == packageName &&
            now - last.lastSeenMillis < SAME_ITEM_GAP_MILLIS
        ) {
            items[items.lastIndex] = last.copy(lastSeenMillis = now)
            return false
        }
        currentKey = key
        items += FeedItem(packageName, describe(texts), firstSeenMillis = now, lastSeenMillis = now)
        return true
    }

    /** Gives each of [sorted] (matched by package and start time) the category at the same index. */
    fun applyCategories(sorted: List<FeedItem>, categories: List<ContentCategory?>) {
        val byItem = sorted.zip(categories)
            .mapNotNull { (item, category) -> category?.let { (item.packageName to item.firstSeenMillis) to it } }
            .toMap()
        items.replaceAll { item ->
            byItem[item.packageName to item.firstSeenMillis]?.let { item.copy(category = it) } ?: item
        }
    }

    private companion object {
        /** Coming back to the same text after this long counts as a new item. */
        const val SAME_ITEM_GAP_MILLIS = 5 * 60_000L
        const val KEY_LENGTH = 40
        const val MAX_DESCRIPTION = 300

        /** The longest text is the caption in practice; it identifies the item. */
        fun keyOf(texts: List<String>): String =
            texts.maxBy { it.length }.lowercase().filter { it.isLetter() || it == ' ' }.take(KEY_LENGTH)

        fun describe(texts: List<String>): String = texts.joinToString(" · ").take(MAX_DESCRIPTION)
    }
}
