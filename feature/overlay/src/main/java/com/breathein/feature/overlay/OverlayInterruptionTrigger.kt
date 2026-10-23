package com.breathein.feature.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.breathein.core.common.InterruptionTrigger
import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest

/** Draws the breathing interruption full-screen over whatever app is open. */
class OverlayInterruptionTrigger(context: Context) : InterruptionTrigger {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Only touched on the main thread.
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    override fun show(request: InterruptionRequest, onOutcome: (InterruptionOutcome) -> Unit) = onMainThread {
        if (overlayView != null) return@onMainThread
        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "Overlay permission missing; skipping interruption")
            return@onMainThread
        }

        val owner = OverlayLifecycleOwner().apply { create() }
        var outcomeDelivered = false
        val view = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                BreathingOverlay(request = request) { outcome ->
                    if (!outcomeDelivered) {
                        outcomeDelivered = true
                        // Posted so the window isn't torn down while it is still dispatching the click.
                        mainHandler.post {
                            removeOverlay()
                            onOutcome(outcome)
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(view, overlayLayoutParams())
        } catch (e: RuntimeException) {
            // BadTokenException / SecurityException if the permission was revoked a moment ago.
            Log.e(TAG, "Could not add overlay", e)
            owner.destroy()
            return@onMainThread
        }
        owner.resume()
        overlayView = view
        lifecycleOwner = owner
    }

    override fun dismiss() = onMainThread { removeOverlay() }

    private fun removeOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Overlay was already removed", e)
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    private fun overlayLayoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // No FLAG_NOT_FOCUSABLE: the overlay takes focus so its buttons receive input.
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        // Draw behind the status and navigation bars; the Compose content pads itself for them.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun onMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private companion object {
        const val TAG = "OverlayTrigger"
    }
}
