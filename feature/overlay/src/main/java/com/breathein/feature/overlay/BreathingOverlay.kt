package com.breathein.feature.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest

/** Full-screen breathing exercise. The two choices only appear once the exercise is over. */
@Composable
internal fun BreathingOverlay(
    request: InterruptionRequest,
    onOutcome: (InterruptionOutcome) -> Unit,
) {
    val breathing = rememberBreathingState()
    val finished = breathing.phase == BreathingPhase.Finished

    // TODO(design): placeholder visuals; default dark Material 3 scheme for now.
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.overlay_today_usage, request.appLabel, request.todayMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 32.dp),
                )

                Box(modifier = Modifier.size(CIRCLE_SIZE.dp), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = breathing.scale.value
                                scaleY = breathing.scale.value
                            }
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                    )
                    Text(
                        text = stringResource(breathing.phase.labelRes()),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), contentAlignment = Alignment.Center) {
                    OutcomeButtons(visible = finished, onOutcome = onOutcome)
                }
            }
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
    BreathingPhase.BreatheIn -> R.string.overlay_breathe_in
    BreathingPhase.BreatheOut -> R.string.overlay_breathe_out
    BreathingPhase.Finished -> R.string.overlay_finished
}

private const val CIRCLE_SIZE = 280

@Preview
@Composable
private fun BreathingOverlayPreview() {
    BreathingOverlay(InterruptionRequest("com.instagram.android", "Instagram", 42)) {}
}
