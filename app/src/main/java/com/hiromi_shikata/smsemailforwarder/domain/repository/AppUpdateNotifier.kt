package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate

interface AppUpdateNotifier {
    fun notify(update: AppUpdate)
    fun cancel()
}
