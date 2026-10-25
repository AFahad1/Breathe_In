package com.breathein.feature.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.breathein.core.common.formatLimit
import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest

/**
 * Full-screen breathing exercise over a see-through background. Tapping anywhere outside the circle
 * counts as "Keep scrolling"; the two buttons appear once the exercise is over.
 *
 * @param backgroundBlurred true when the window blurs the app behind it, so a light tint is enough.
 */
@Composable
internal fun BreathingOverlay(
    request: InterruptionRequest,
    backgroundBlurred: Boolean,
    onOutcome: (InterruptionOutcome) -> Unit,
) {
    val breathing = rememberBreathingState()
    val finished = breathing.phase == BreathingPhase.Finished
    val countingDown = breathing.phase == BreathingPhase.Countdown
    // Without blur, a darker tint keeps the text readable over a busy app.
    // It fades in over the countdown, together with the window blur (see OverlayInterruptionTrigger).
    val tintAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        tintAlpha.animateTo(if (backgroundBlurred) 0.35f else 0.7f, tween(COUNTDOWN_SECONDS * 1_000, easing = LinearEasing))
    }
    val tint = Color.Black.copy(alpha = tintAlpha.value)

    // TODO(design): placeholder visuals.
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    onOutcome(InterruptionOutcome.KeepScrolling)
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (countingDown) {
                    PromptMessage(request, modifier = Modifier.padding(top = 32.dp))
                } else {
                    Text(
                        text = stringResource(R.string.overlay_today_usage, request.appLabel, request.todayMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }

                BreathingCircle(breathing)

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                    if (countingDown) {
                        Text(
                            text = stringResource(R.string.overlay_skip_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    OutcomeButtons(visible = finished, onOutcome = onOutcome)
                }
            }
        }
    }
}

/** "You've been on Instagram Reels for 2 min." + "Mostly gym & fitness." + "Take a breathing break in…" */
@Composable
private fun PromptMessage(request: InterruptionRequest, modifier: Modifier = Modifier) {
    val scrolled = formatLimit(request.scrolledSeconds.coerceAtLeast(1))
    val where = request.feedName?.let { "${request.appLabel} $it" } ?: request.appLabel
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.overlay_prompt_scrolled, where, scrolled),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        request.topCategory?.let { category ->
            Text(
                text = stringResource(R.string.overlay_prompt_topic, category.label.lowercase()),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stringResource(R.string.overlay_prompt_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** The black circle. Taps on it are swallowed so they don't count as tapping outside. */
@Composable
private fun BreathingCircle(breathing: BreathingState) {
    Box(modifier = Modifier.size(CIRCLE_SIZE.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breathing.scale.value
                    scaleY = breathing.scale.value
                }
                .clip(CircleShape)
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        )
        if (breathing.phase == BreathingPhase.Countdown) {
            Text(
                text = breathing.countdown.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
        } else {
            Text(
                text = stringResource(breathing.phase.labelRes()),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun OutcomeButtons(visible: Boolean, onOutcome: (InterruptionOutcome) -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = { onOutcome(InterruptionOutcome.ImDone) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.overlay_im_done))
            }
            OutlinedButton(onClick = { onOutcome(InterruptionOutcome.KeepScrolling) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.overlay_keep_scrolling))
            }
        }
    }
}

private fun BreathingPhase.labelRes(): Int = when (this) {
    BreathingPhase.Countdown, BreathingPhase.BreatheIn -> R.string.overlay_breathe_in
    BreathingPhase.Hold -> R.string.overlay_hold
    BreathingPhase.BreatheOut -> R.string.overlay_breathe_out
    BreathingPhase.Finished -> R.string.overlay_finished
}

private const val CIRCLE_SIZE = 280

@Preview
@Composable
private fun BreathingOverlayPreview() {
    BreathingOverlay(
        InterruptionRequest("com.instagram.android", "Instagram", 42, feedName = "Reels", scrolledSeconds = 120),
        backgroundBlurred = false,
    ) {}
}
