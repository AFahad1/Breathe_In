package com.breathein.feature.overlay

import android.animation.ValueAnimator
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
    private var blurAnimator: ValueAnimator? = null

    override fun show(request: InterruptionRequest, onOutcome: (InterruptionOutcome) -> Unit) = onMainThread {
        if (overlayView != null) return@onMainThread
        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "Overlay permission missing; skipping interruption")
            return@onMainThread
        }

        val owner = OverlayLifecycleOwner().apply { create() }
        val blurBehind = canBlurBehind()
        var outcomeDelivered = false
        val view = ComposeView(appContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                BreathingOverlay(request = request, backgroundBlurred = blurBehind) { outcome ->
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

        val params = overlayLayoutParams(blurBehind)
        try {
            windowManager.addView(view, params)
        } catch (e: RuntimeException) {
            // BadTokenException / SecurityException if the permission was revoked a moment ago.
            Log.e(TAG, "Could not add overlay", e)
            owner.destroy()
            return@onMainThread
        }
        owner.resume()
        overlayView = view
        lifecycleOwner = owner
        if (blurBehind) fadeInBlur(view, params)
    }

    /** Ramps the blur from none to full over the countdown, so the app behind fades out gently. */
    private fun fadeInBlur(view: ComposeView, params: WindowManager.LayoutParams) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        blurAnimator = ValueAnimator.ofInt(0, BLUR_RADIUS_PX).apply {
            duration = COUNTDOWN_SECONDS * 1_000L
            addUpdateListener { animator ->
                val radius = animator.animatedValue as Int
                if (overlayView !== view || params.blurBehindRadius == radius) return@addUpdateListener
                params.blurBehindRadius = radius
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: IllegalArgumentException) {
                    cancel() // The overlay was removed mid-animation.
                }
            }
            start()
        }
    }

    override fun dismiss() = onMainThread { removeOverlay() }

    private fun removeOverlay() {
        val view = overlayView ?: return
        overlayView = null
        blurAnimator?.cancel()
        blurAnimator = null
        try {
            windowManager.removeView(view)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Overlay was already removed", e)
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    /** Blurring the app behind needs Android 12+ and can be switched off (some devices, battery saver). */
    private fun canBlurBehind(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled

    private fun overlayLayoutParams(blurBehind: Boolean) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // No FLAG_NOT_FOCUSABLE: the overlay takes focus so its buttons receive input.
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        if (blurBehind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            blurBehindRadius = 0 // Raised gradually by fadeInBlur().
        }
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
        const val BLUR_RADIUS_PX = 80
    }
}
