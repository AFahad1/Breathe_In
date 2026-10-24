package com.breathein.feature.scrolldetect

import com.breathein.core.common.ScrollFeatureDetector
import com.breathein.core.common.ViewIdCapture
import com.breathein.core.model.ScrollFeature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [ScrollFeatureDetector] fed by [ScrollDetectionService]. One instance per process, owned by AppContainer. */
class AccessibilityScrollFeatureDetector : ScrollFeatureDetector {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _activeFeature = MutableStateFlow<ScrollFeature?>(null)
    override val activeFeature: StateFlow<ScrollFeature?> = _activeFeature.asStateFlow()

    private val _lastViewIdCapture = MutableStateFlow<ViewIdCapture?>(null)
    override val lastViewIdCapture: StateFlow<ViewIdCapture?> = _lastViewIdCapture.asStateFlow()

    @Volatile
    internal var captureRequested = false
        private set

    override fun requestViewIdCapture() {
        captureRequested = true
    }

    internal fun onServiceConnected() {
        _isConnected.value = true
    }

    internal fun onServiceDisconnected() {
        _isConnected.value = false
        _activeFeature.value = null
    }

    internal fun onWindowInspected(feature: ScrollFeature?) {
        _activeFeature.value = feature
    }

    internal fun onViewIdsCaptured(capture: ViewIdCapture) {
        captureRequested = false
        _lastViewIdCapture.value = capture
    }
}
