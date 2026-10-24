package com.breathein.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.breathein.core.common.ViewIdCapture
import com.breathein.core.model.AppUsage
import com.breathein.core.model.ScrollFeature
import com.breathein.core.model.SessionState

@Composable
internal fun PermissionsSection(permissions: PermissionSnapshot) {
    DebugCard(title = stringResource(R.string.debug_permissions)) {
        StatusLine(stringResource(R.string.debug_permission_usage), permissions.usageAccess)
        StatusLine(stringResource(R.string.debug_permission_overlay), permissions.overlay)
        StatusLine(stringResource(R.string.debug_permission_notifications), permissions.notifications)
        StatusLine(stringResource(R.string.debug_permission_accessibility), permissions.accessibility)
    }
}

@Composable
internal fun MonitoringSection(
    isMonitoring: Boolean,
    sessions: List<SessionState>,
    foregroundPackage: String?,
    labelFor: (String) -> String,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    DebugCard(title = stringResource(R.string.debug_monitoring)) {
        StatusLine(
            label = stringResource(R.string.debug_monitoring_service),
            ok = isMonitoring,
            okText = stringResource(R.string.debug_running),
            notOkText = stringResource(R.string.debug_stopped),
        )
        Text(stringResource(R.string.debug_foreground_app, foregroundPackage ?: stringResource(R.string.debug_unknown)))
        if (sessions.isEmpty()) Text(stringResource(R.string.debug_session_idle))
        sessions.forEach { session ->
            val line = stringResource(R.string.debug_session_active, labelFor(session.packageName), session.elapsedSeconds)
            Text(if (session.interruptionShown) "$line · ${stringResource(R.string.debug_interruption_shown)}" else line)
        }
        if (isMonitoring) {
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.debug_stop_monitoring))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.debug_start_monitoring))
            }
        }
    }
}

@Composable
internal fun MonitoredAppsSection(monitoredAppCount: Int, onOpen: () -> Unit) {
    DebugCard(title = stringResource(R.string.debug_monitored_apps)) {
        Text(stringResource(R.string.debug_monitored_apps_count, monitoredAppCount))
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debug_edit_monitored_apps))
        }
    }
}

@Composable
internal fun DetectionSection(
    connected: Boolean,
    activeFeature: ScrollFeature?,
    capture: ViewIdCapture?,
    onCapture: () -> Unit,
) {
    DebugCard(title = stringResource(R.string.debug_detection)) {
        StatusLine(
            label = stringResource(R.string.debug_detection_service),
            ok = connected,
            okText = stringResource(R.string.debug_connected),
            notOkText = stringResource(R.string.debug_not_connected),
        )
        Text(stringResource(R.string.debug_detected_feature, activeFeature?.name ?: stringResource(R.string.debug_none)))
        OutlinedButton(onClick = onCapture, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debug_capture_view_ids))
        }
        if (capture != null) {
            Text(
                text = stringResource(R.string.debug_captured_from, capture.packageName, capture.viewIds.size),
                style = MaterialTheme.typography.labelLarge,
            )
            SelectionContainer {
                Text(capture.viewIds.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun OverlayTestSection(onTestNow: () -> Unit, onTestDelayed: () -> Unit) {
    DebugCard(title = stringResource(R.string.debug_overlay)) {
        Button(onClick = onTestNow, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debug_test_overlay))
        }
        OutlinedButton(onClick = onTestDelayed, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.debug_test_overlay_delayed))
        }
    }
}

@Composable
internal fun UsageHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.debug_today_usage),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRefresh) { Text(stringResource(R.string.debug_refresh)) }
    }
}

@Composable
internal fun UsageRow(usage: AppUsage) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(usage.label, style = MaterialTheme.typography.bodyLarge)
            Text(usage.packageName, style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.debug_minutes, usage.minutes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DebugCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    ok: Boolean,
    okText: String = stringResource(R.string.debug_yes),
    notOkText: String = stringResource(R.string.debug_no),
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            text = if (ok) okText else notOkText,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
