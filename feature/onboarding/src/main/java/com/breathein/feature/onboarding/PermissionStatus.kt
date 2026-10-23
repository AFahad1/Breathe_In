package com.breathein.feature.onboarding

import com.breathein.core.permissions.PermissionChecker

internal data class PermissionStatus(
    val usageAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
) {
    val allGranted: Boolean get() = usageAccess && overlay && notifications

    companion object {
        fun from(checker: PermissionChecker) = PermissionStatus(
            usageAccess = checker.hasUsageAccess(),
            overlay = checker.canDrawOverlays(),
            notifications = checker.hasNotificationPermission(),
        )
    }
}
