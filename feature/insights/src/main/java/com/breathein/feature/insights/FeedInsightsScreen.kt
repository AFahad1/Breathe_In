package com.breathein.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.breathein.core.common.FeedInsightsRepository
import com.breathein.core.common.ShortFormApps
import com.breathein.core.common.formatLimit
import com.breathein.core.model.FeedItem

/** Everything watched in Reels / Shorts today: summary, every category and the recent videos. */
@Composable
fun FeedInsightsScreen(repository: FeedInsightsRepository, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val insights by repository.insights.collectAsStateWithLifecycle()
    LaunchedEffect(repository) { repository.refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.insights_back)) }
        Column {
            Text(stringResource(R.string.insights_title), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.insights_total, formatLimit(insights.totalSeconds)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        insights.summary?.let { Text(it, style = MaterialTheme.typography.titleMedium) }

        if (insights.categories.isNotEmpty()) {
            val sortedSeconds = insights.categories.sumOf { it.seconds }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                insights.categories.forEach { CategoryBar(it, sortedSeconds) }
            }
        }

        if (insights.unsortedSeconds > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.insights_unsorted, formatLimit(insights.unsortedSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = repository::refresh, enabled = !insights.isSorting) {
                    Text(stringResource(if (insights.isSorting) R.string.insights_card_sorting else R.string.insights_sort_now))
                }
            }
        }

        if (insights.recentItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.insights_recent), style = MaterialTheme.typography.titleMedium)
                RecentItems(insights.recentItems)
            }
        }

        Text(
            text = stringResource(if (insights.usesAi) R.string.insights_sorted_by_ai else R.string.insights_sorted_by_keywords),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RecentItems(items: List<FeedItem>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
                    val app = ShortFormApps.find(item.packageName)?.let { "${it.label} ${it.feedName}" }.orEmpty()
                    val category = item.category?.label ?: stringResource(R.string.insights_unsorted_item)
                    Text(
                        text = listOf(category, app).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
