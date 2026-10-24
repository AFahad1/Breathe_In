package com.breathein.core.common

import com.breathein.core.model.ScrollFeature
import kotlinx.coroutines.flow.StateFlow

/** Detects short-form feeds (Reels, Shorts) inside apps. Implemented by :feature:scrolldetect. */
interface ScrollFeatureDetector {
    /** False until the user enables the accessibility service. */
    val isConnected: StateFlow<Boolean>

    /** Feature on screen in the last inspected Instagram/YouTube window, or null. Check its app is still in front. */
    val activeFeature: StateFlow<ScrollFeature?>

    /** Debug aid: view IDs captured from the next Instagram/YouTube window, to keep detection up to date. */
    val lastViewIdCapture: StateFlow<ViewIdCapture?>

    fun requestViewIdCapture()
}

data class ViewIdCapture(val packageName: String, val viewIds: List<String>)
