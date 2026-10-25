package com.breathein.core.model

/**
 * One reel or short the user watched, recognised by the text on screen (caption, creator, audio name).
 * Times are wall-clock millis.
 */
data class FeedItem(
    val packageName: String,
    val text: String,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
    /** Null until the classifier has looked at it. */
    val category: ContentCategory? = null,
)
