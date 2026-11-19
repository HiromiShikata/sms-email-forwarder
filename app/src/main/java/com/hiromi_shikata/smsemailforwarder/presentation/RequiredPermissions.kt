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
