package com.breathein.feature.scrolldetect

/**
 * Picks the text that describes a reel or short (caption, creator, audio) out of everything visible in the
 * feed, dropping buttons ("Like", "Share"…) and counters ("1.2K"), which say nothing about the content.
 */
internal object FeedTextFilter {

    fun keep(texts: List<String>): List<String> =
        texts.asSequence()
            .map { it.replace(WHITESPACE, " ").trim() }
            .filter { it.length in MIN_LENGTH..MAX_LENGTH }
            .filter { text -> text.any(Char::isLetter) }
            .filterNot { COUNTER.matches(it) }
            .filterNot { it.lowercase() in CHROME }
            .distinct()
            .take(MAX_TEXTS)
            .toList()

    private const val MIN_LENGTH = 3
    private const val MAX_LENGTH = 400
    private const val MAX_TEXTS = 12
    private val WHITESPACE = Regex("""\s+""")

    // "1,234", "1.2K", "12K likes", "3 comments"
    private val COUNTER = Regex("""^[\d.,]+\s*[kmb]?(\s+\p{L}+)?$""", RegexOption.IGNORE_CASE)

    private val CHROME = setOf(
        "like", "likes", "comment", "comments", "share", "send", "save", "follow", "following", "subscribe",
        "subscribed", "remix", "more", "less", "reels", "shorts", "home", "search", "explore", "profile",
        "your story", "create", "camera", "back", "close", "menu", "options", "more options", "dislike",
        "mute", "unmute", "play", "pause", "audio", "original audio", "sponsored", "see translation",
        "add comment", "add a comment…", "subscriptions", "you", "notifications",
    )
}
