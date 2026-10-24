package com.breathein.core.common

import com.breathein.core.model.ScrollFeature
import com.breathein.core.model.ShortFormApp

/** The short-form feeds Breathe In targets first. */
object ShortFormApps {

    val all = listOf(
        ShortFormApp(INSTAGRAM_PACKAGE, "Instagram", "Reels", ScrollFeature.InstagramReels),
        ShortFormApp(YOUTUBE_PACKAGE, "YouTube", "Shorts", ScrollFeature.YouTubeShorts),
        ShortFormApp(TIKTOK_PACKAGE, "TikTok", "For You feed", feature = null),
    )

    fun find(packageName: String): ShortFormApp? = all.firstOrNull { it.packageName == packageName }

    /** The in-app feature that has to be on screen for time in [packageName] to count, or null for the whole app. */
    fun requiredFeature(packageName: String): ScrollFeature? = find(packageName)?.feature
}
