package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository

class AppUpdateStatusGetUseCase(
    private val repository: AppUpdateStatusRepository,
) {
    fun execute(): AppUpdateStatus? = repository.load()
}
