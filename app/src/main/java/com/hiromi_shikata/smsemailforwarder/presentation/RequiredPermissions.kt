package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest

internal const val ANDROID_13_API_LEVEL = 33

internal fun requiredPermissions(sdkInt: Int): List<String> {
    val required = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
    if (sdkInt >= ANDROID_13_API_LEVEL) {
        required.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return required
}

internal fun missingRequiredPermissions(sdkInt: Int, isGranted: (String) -> Boolean): List<String> =
    requiredPermissions(sdkInt).filterNot(isGranted)

internal sealed interface PermissionStatusText {
    data object Granted : PermissionStatusText
    data class Missing(val sms: Boolean, val notifications: Boolean) : PermissionStatusText
}

internal fun resolvePermissionStatusText(missing: List<String>): PermissionStatusText {
    if (missing.isEmpty()) return PermissionStatusText.Granted
    return PermissionStatusText.Missing(
        sms = missing.any { it == Manifest.permission.RECEIVE_SMS || it == Manifest.permission.READ_SMS },
        notifications = Manifest.permission.POST_NOTIFICATIONS in missing,
    )
}

internal sealed interface PermissionGrantAction {
    data class Request(val permissions: List<String>) : PermissionGrantAction
    data class OpenSettings(val permission: String) : PermissionGrantAction
    data object None : PermissionGrantAction
}

internal fun resolvePermissionGrantAction(
    missing: List<String>,
    requestedBefore: Set<String>,
    shouldShowRationale: (String) -> Boolean,
): PermissionGrantAction {
    val permanentlyDenied = missing.filter { it in requestedBefore && !shouldShowRationale(it) }
    val requestable = missing - permanentlyDenied.toSet()
    return when {
        requestable.isNotEmpty() -> PermissionGrantAction.Request(requestable)
        permanentlyDenied.isNotEmpty() -> PermissionGrantAction.OpenSettings(permanentlyDenied.first())
        else -> PermissionGrantAction.None
    }
}
