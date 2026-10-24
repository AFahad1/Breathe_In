package com.breathein.feature.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.breathein.core.permissions.PermissionChecker
import com.breathein.core.permissions.PermissionIntents

/** Lists the permissions Breathe In needs. "Start monitoring" unlocks once all are granted. */
@Composable
fun OnboardingScreen(
    permissionChecker: PermissionChecker,
    onStartMonitoring: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PermissionStatus.from(permissionChecker)) }
    var notificationPromptShown by rememberSaveable { mutableStateOf(false) }

    // The user grants two of these in system Settings, so re-check every time we come back.
    LifecycleResumeEffect(permissionChecker) {
        status = PermissionStatus.from(permissionChecker)
        onPauseOrDispose { }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        status = PermissionStatus.from(permissionChecker)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyLarge)

        PermissionRow(
            title = stringResource(R.string.onboarding_usage_title),
            description = stringResource(R.string.onboarding_usage_description),
            granted = status.usageAccess,
            onGrant = { context.openSettings(PermissionIntents.usageAccess()) },
        )
        PermissionRow(
            title = stringResource(R.string.onboarding_overlay_title),
            description = stringResource(R.string.onboarding_overlay_description),
            granted = status.overlay,
            onGrant = { context.openSettings(PermissionIntents.overlay(context)) },
        )
        PermissionRow(
            title = stringResource(R.string.onboarding_accessibility_title),
            description = stringResource(R.string.onboarding_accessibility_description),
            granted = status.accessibility,
            onGrant = { context.openSettings(PermissionIntents.accessibility()) },
        )
        PermissionRow(
            title = stringResource(R.string.onboarding_notifications_title),
            description = stringResource(R.string.onboarding_notifications_description),
            granted = status.notifications,
            onGrant = {
                // Ask once with the system prompt; after that it may be blocked, so go to Settings.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPromptShown) {
                    notificationPromptShown = true
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    context.openSettings(PermissionIntents.notifications(context))
                }
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartMonitoring,
            enabled = status.allGranted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_start))
        }
    }
}

private fun Context.openSettings(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.w("Onboarding", "No settings screen for ${intent.action}", e)
    }
}
