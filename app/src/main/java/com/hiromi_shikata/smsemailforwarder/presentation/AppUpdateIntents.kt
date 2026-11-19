package com.hiromi_shikata.smsemailforwarder.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal fun unknownSourcesSettingsIntent(packageName: String): Intent =
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))

internal fun installApkIntent(contentUri: Uri): Intent =
    Intent(Intent.ACTION_VIEW)
        .setDataAndType(contentUri, APK_MIME_TYPE)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

internal const val APK_MIME_TYPE = "application/vnd.android.package-archive"
