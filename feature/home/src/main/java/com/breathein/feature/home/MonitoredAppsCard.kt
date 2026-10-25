package com.breathein.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.breathein.core.common.formatLimit

/** The monitored apps, each with today's minutes and when the break kicks in. */
@Composable
internal fun MonitoredAppsCard(apps: List<AppToday>?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            when {
                apps == null -> EmptyLine(stringResource(R.string.home_loading))
                apps.isEmpty() -> EmptyLine(stringResource(R.string.home_no_apps))
                else -> apps.forEachIndexed { index, app ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    AppRow(app)
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppToday) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // "YouTube Shorts", "Instagram Reels", or just the app name.
            Text(listOfNotNull(app.label, app.feedName).joinToString(" "), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.home_app_limit, formatLimit(app.limitSeconds)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.home_app_minutes, app.minutesToday),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}
