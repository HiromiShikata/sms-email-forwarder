package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.AppReleaseSelector
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository

class AppUpdateCheckUseCase(
    private val repository: AppUpdateRepository,
) {
    fun execute(currentVersion: String): AppUpdate {
        val update = repository.fetchLatest(currentVersion)
        return update.copy(releases = AppReleaseSelector.newerThan(update.releases, currentVersion))
    }
}
