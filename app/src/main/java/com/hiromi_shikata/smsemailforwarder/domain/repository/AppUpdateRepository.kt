package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate

interface AppUpdateRepository {
    fun fetchLatest(currentVersion: String): AppUpdate
}
