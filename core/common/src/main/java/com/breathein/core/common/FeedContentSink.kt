package com.breathein.core.common

/** Receives the text visible in a short-form feed. Implemented by :feature:insights, fed by :feature:scrolldetect. */
interface FeedContentSink {
    /** Called from a background thread, several times a second while a feed is on screen. */
    fun onFeedContent(packageName: String, texts: List<String>)
}

object NoOpFeedContentSink : FeedContentSink {
    override fun onFeedContent(packageName: String, texts: List<String>) = Unit
}
