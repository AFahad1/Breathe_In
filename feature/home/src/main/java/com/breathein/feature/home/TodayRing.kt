package com.breathein.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Today's total minutes inside a ring that slowly "breathes", echoing the break screen. */
@Composable
internal fun TodayRing(totalMinutes: Int?, modifier: Modifier = Modifier) {
    val breathing = rememberInfiniteTransition(label = "ring")
    val scale by breathing.animateFloat(
        initialValue = 0.96f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ringScale",
    )

    Box(modifier = modifier.size(RING_SIZE.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            drawCircle(color = Color.White.copy(alpha = 0.04f))
            drawCircle(color = Color.White.copy(alpha = 0.18f), style = Stroke(width = 1.5.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = totalMinutes?.toString() ?: "–",
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = stringResource(R.string.home_minutes_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val RING_SIZE = 240
private const val BREATH_MILLIS = 5_000
