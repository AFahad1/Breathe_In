package com.breathein.feature.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal enum class BreathingPhase { BreatheIn, Hold, BreatheOut, Finished }

@Stable
internal class BreathingState {
    val scale = Animatable(MIN_SCALE)
    var phase by mutableStateOf(BreathingPhase.BreatheIn)
        internal set

    companion object {
        const val MIN_SCALE = 0.4f
        const val MAX_SCALE = 1f
    }
}

/** Runs [cycles] of breathe in / hold / breathe out, then moves to [BreathingPhase.Finished]. */
@Composable
internal fun rememberBreathingState(
    cycles: Int = 3,
    breatheInMillis: Int = 5_000,
    holdMillis: Long = 2_000,
    breatheOutMillis: Int = 5_000,
): BreathingState {
    val state = remember { BreathingState() }
    LaunchedEffect(state) {
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
