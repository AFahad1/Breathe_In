package com.breathein.feature.scrolldetect

import com.breathein.core.common.FeedContentSink

/** Implemented by the Application so [ScrollDetectionService], which the system creates, can find the detector. */
interface ScrollDetectionDependenciesProvider {
    val scrollFeatureDetector: AccessibilityScrollFeatureDetector

    /** Where the text visible in Reels / Shorts goes, for "what you watched" insights. */
    val feedContentSink: FeedContentSink
}
