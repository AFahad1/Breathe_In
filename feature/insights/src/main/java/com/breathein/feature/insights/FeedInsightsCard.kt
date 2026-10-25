package com.breathein.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.breathein.core.common.FeedInsightsRepository

/** Home screen card: the top categories today and the AI summary. Tapping "See everything" opens [FeedInsightsScreen]. */
@Composable
fun FeedInsightsCard(repository: FeedInsightsRepository, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val insights by repository.insights.collectAsStateWithLifecycle()
    LaunchedEffect(repository) { repository.refresh() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.insights_card_title), style = MaterialTheme.typography.titleMedium)
            val sortedSeconds = insights.categories.sumOf { it.seconds }
            when {
                insights.totalSeconds == 0 -> Hint(stringResource(R.string.insights_card_empty))
                insights.categories.isEmpty() -> Hint(stringResource(R.string.insights_card_sorting))
                else -> {
                    insights.summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    insights.categories.take(TOP_CATEGORIES).forEach { CategoryBar(it, sortedSeconds) }
                }
            }
            if (insights.totalSeconds > 0) {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.insights_see_all))
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private const val TOP_CATEGORIES = 3
