package com.breathein.feature.debug

import com.breathein.core.permissions.PermissionChecker

internal data class PermissionSnapshot(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
) {
    companion object {
        fun from(checker: PermissionChecker) = PermissionSnapshot(
            usageAccess = checker.hasUsageAccess(),
            overlay = checker.canDrawOverlays(),
            notifications = checker.hasNotificationPermission(),
        )
    }
}
