package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository

class ForwardingConfigUpdateUseCase(
    private val repository: ForwardingConfigRepository,
) {
    fun execute(config: ForwardingConfig) = repository.save(config)
}
