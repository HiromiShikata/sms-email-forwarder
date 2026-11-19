package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import java.io.File

interface AppUpdateNotifier {
    fun notifyDownloaded(latestVersion: String, releases: List<AppRelease>, apkFile: File)
    fun cancel()
}
