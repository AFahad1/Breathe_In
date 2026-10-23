package com.breathein.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Intents that open the system settings page for each permission. */
object PermissionIntents {

    fun usageAccess(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun overlay(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** Fallback when the runtime notification prompt has been denied permanently. */
    fun notifications(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
