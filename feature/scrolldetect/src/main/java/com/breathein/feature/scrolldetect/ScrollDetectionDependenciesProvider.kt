package com.breathein.feature.scrolldetect

/** Implemented by the Application so [ScrollDetectionService], which the system creates, can find the detector. */
interface ScrollDetectionDependenciesProvider {
    val scrollFeatureDetector: AccessibilityScrollFeatureDetector
}
