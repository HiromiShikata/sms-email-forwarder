package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository

class ForwardingConfigGetUseCase(
    private val repository: ForwardingConfigRepository,
) {
    fun execute(): ForwardingConfig = repository.get()
}
