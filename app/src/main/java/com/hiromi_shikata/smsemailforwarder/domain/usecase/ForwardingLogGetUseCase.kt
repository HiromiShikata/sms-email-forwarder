package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository

class ForwardingLogGetUseCase(
    private val repository: ForwardingLogRepository,
) {
    fun execute(): List<ForwardingLogEntry> = repository.getAll()
}
