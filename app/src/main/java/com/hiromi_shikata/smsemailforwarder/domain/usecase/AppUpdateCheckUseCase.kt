package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository

class AppUpdateCheckUseCase(
    private val repository: AppUpdateRepository,
) {
    fun execute(currentVersion: String): AppUpdate = repository.fetchLatest(currentVersion)
}
