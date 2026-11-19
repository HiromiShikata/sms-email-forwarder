package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease

interface AppUpdateNotifier {
    fun notifyReadyToInstall(releases: List<AppRelease>)
    fun cancel()
}
