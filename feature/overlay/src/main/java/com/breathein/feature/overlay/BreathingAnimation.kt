package com.breathein.feature.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal enum class BreathingPhase { Countdown, BreatheIn, Hold, BreatheOut, Finished }

@Stable
internal class BreathingState(countdownSeconds: Int) {
    val scale = Animatable(MIN_SCALE)
    var phase by mutableStateOf(BreathingPhase.Countdown)
        internal set

    /** Seconds left before the breathing starts, while in [BreathingPhase.Countdown]. */
    var countdown by mutableIntStateOf(countdownSeconds)
        internal set

    companion object {
        const val MIN_SCALE = 0.4f
        const val PUMP_SCALE = 0.5f
        const val MAX_SCALE = 1f
    }
}

private const val PUMP_UP_MILLIS = 300

/** How long the prompt counts down before breathing starts; the background blur fades in over the same time. */
internal const val COUNTDOWN_SECONDS = 5

/**
 * Counts down from [countdownSeconds] (the user can still tap away), then runs [cycles] of
 * breathe in / hold / breathe out, then moves to [BreathingPhase.Finished].
 */
@Composable
internal fun rememberBreathingState(
    countdownSeconds: Int = COUNTDOWN_SECONDS,
    cycles: Int = 3,
    breatheInMillis: Int = 5_000,
    holdMillis: Long = 2_000,
    breatheOutMillis: Int = 5_000,
): BreathingState {
    val state = remember { BreathingState(countdownSeconds) }
    LaunchedEffect(state) {
        // One "pump" per number: a quick swell, then settle back, one second in total.
        for (secondsLeft in countdownSeconds downTo 1) {
            state.countdown = secondsLeft
            state.scale.animateTo(BreathingState.PUMP_SCALE, tween(PUMP_UP_MILLIS, easing = FastOutSlowInEasing))
            state.scale.animateTo(BreathingState.MIN_SCALE, tween(1_000 - PUMP_UP_MILLIS, easing = FastOutSlowInEasing))
        }
        repeat(cycles) {
            state.phase = BreathingPhase.BreatheIn
            state.scale.animateTo(BreathingState.MAX_SCALE, tween(breatheInMillis, easing = FastOutSlowInEasing))
            state.phase = BreathingPhase.Hold
            delay(holdMillis)
            state.phase = BreathingPhase.BreatheOut
            state.scale.animateTo(BreathingState.MIN_SCALE, tween(breatheOutMillis, easing = FastOutSlowInEasing))
        }
        state.phase = BreathingPhase.Finished
    }
    return state
}
