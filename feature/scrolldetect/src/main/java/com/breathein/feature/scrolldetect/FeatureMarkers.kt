package com.breathein.feature.scrolldetect

import com.breathein.core.common.INSTAGRAM_PACKAGE
import com.breathein.core.common.YOUTUBE_PACKAGE
import com.breathein.core.model.ScrollFeature

/**
 * View IDs that only appear on screen while a short-form feed is open. These are internal to
 * Instagram/YouTube and can change with app updates: if detection stops working, open the feed,
 * use "Capture view IDs" on the debug screen and add the new IDs here.
 */
internal object FeatureMarkers {

    private val markers: Map<ScrollFeature, Marker> = mapOf(
        ScrollFeature.InstagramReels to Marker(
            INSTAGRAM_PACKAGE,
            listOf("clips_viewer_view_pager", "clips_viewer_container", "clips_video_container", "root_clips_layout"),
        ),
        ScrollFeature.YouTubeShorts to Marker(
            YOUTUBE_PACKAGE,
            // Checked against YouTube 21.18. Not "reel_time_bar": it stays visible on the Home tab after leaving Shorts.
            listOf("reel_recycler", "reel_player_page_container", "reel_watch_player"),
        ),
    )

    /** Fully qualified view IDs ("package:id/name") to look for in [packageName], per feature. */
    fun idsFor(packageName: String): Map<ScrollFeature, List<String>> =
        markers.filterValues { it.packageName == packageName }
            .mapValues { (_, marker) -> marker.viewIds.map { "${marker.packageName}:id/$it" } }

    /** Which feature (if any) a window from [packageName] showing [visibleViewIds] belongs to. */
    fun match(packageName: String, visibleViewIds: Set<String>): ScrollFeature? =
        idsFor(packageName).entries.firstOrNull { (_, ids) -> ids.any(visibleViewIds::contains) }?.key

    private class Marker(val packageName: String, val viewIds: List<String>)
}
