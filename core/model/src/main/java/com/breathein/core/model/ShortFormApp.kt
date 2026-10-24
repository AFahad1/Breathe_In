package com.breathein.core.model

/**
 * An app we target for its short-form feed. When [feature] is set, only time spent in that feature
 * counts (e.g. Reels, not the Instagram home feed); when null, the whole app is the feed (TikTok).
 */
data class ShortFormApp(
    val packageName: String,
    val label: String,
    val feedName: String,
    val feature: ScrollFeature?,
)
