package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun permissionSettingsIntent(permission: String, packageName: String): Intent =
    if (permission == Manifest.permission.POST_NOTIFICATIONS) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    }
