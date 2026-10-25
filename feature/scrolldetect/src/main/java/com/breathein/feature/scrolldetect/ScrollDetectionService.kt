package com.breathein.feature.scrolldetect

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.breathein.core.common.FeedContentSink
import com.breathein.core.common.ViewIdCapture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Watches Instagram and YouTube windows (only those, see res/xml/scroll_detection_service.xml) and reports
 * whether Reels or Shorts is on screen, by known view IDs. While a feed is on screen it also passes the
 * visible text (caption, creator, audio) to [FeedContentSink] so the user can see what their time went to.
 * Nothing is read outside Reels / Shorts.
 */
class ScrollDetectionService : AccessibilityService() {

    private var detector: AccessibilityScrollFeatureDetector? = null
    private var feedContentSink: FeedContentSink? = null

    // Window queries run on their own thread: this service shares a process (and main thread) with our UI,
    // and querying our own window from the main thread would block it while it waits for itself.
    private val workerThread = HandlerThread("scroll-detection")
    private lateinit var worker: Handler
    private val inspectionScheduled = AtomicBoolean(false)

    @Volatile
    private var lastPackage: String? = null

    // Scrolling fires many events a second; inspect the window at most once per INSPECTION_DELAY_MILLIS.
    private val inspectWindow = Runnable {
        inspectionScheduled.set(false)
        inspect()
    }

    override fun onServiceConnected() {
        workerThread.start()
        worker = Handler(workerThread.looper)
        val dependencies = application as? ScrollDetectionDependenciesProvider
        detector = dependencies?.scrollFeatureDetector
        feedContentSink = dependencies?.feedContentSink
        if (detector == null) Log.e(TAG, "Application does not implement ScrollDetectionDependenciesProvider")
        Log.i(TAG, "Connected")
        detector?.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        lastPackage = event.packageName?.toString() ?: return
        if (::worker.isInitialized && inspectionScheduled.compareAndSet(false, true)) {
            worker.postDelayed(inspectWindow, INSPECTION_DELAY_MILLIS)
        }
    }

    private fun inspect() {
        val detector = detector ?: return
        val packageName = lastPackage ?: return
        // When another app (or our own overlay) is on top, keep the last result.
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return

        val visibleIds = FeatureMarkers.idsFor(packageName).values.flatten()
            .filterTo(HashSet()) { id -> root.findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser } }
        val feature = FeatureMarkers.match(packageName, visibleIds)
        detector.onWindowInspected(feature)
        if (feature != null) {
            feedContentSink?.onFeedContent(packageName, FeedTextFilter.keep(collectTexts(root)))
        }

        if (detector.captureRequested) {
            detector.onViewIdsCaptured(ViewIdCapture(packageName, collectViewIds(root)))
        }
    }

    private fun collectViewIds(root: AccessibilityNodeInfo): List<String> {
        val ids = sortedSetOf<String>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty() && ids.size < MAX_CAPTURED_IDS) {
            val node = queue.removeFirst()
            node.viewIdResourceName?.let { if (node.isVisibleToUser) ids += it }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return ids.toList()
    }

    /** Text and content descriptions of visible views, in screen order. */
    private fun collectTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque(listOf(root))
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (node.isVisibleToUser) {
                node.text?.toString()?.let(texts::add)
                node.contentDescription?.toString()?.let(texts::add)
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return texts
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        Log.i(TAG, "Disconnected")
        workerThread.quitSafely()
        detector?.onServiceDisconnected()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "ScrollDetection"
        const val INSPECTION_DELAY_MILLIS = 300L
        const val MAX_CAPTURED_IDS = 200
        const val MAX_NODES = 400
    }
}
