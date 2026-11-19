package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease

interface AppUpdateRepository {
    fun fetchReleasesNewerThan(installedVersion: String): List<AppRelease>
}
