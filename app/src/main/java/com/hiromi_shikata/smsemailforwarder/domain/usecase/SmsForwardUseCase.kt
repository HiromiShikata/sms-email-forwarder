package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository

class SmsForwardUseCase(
    private val configRepository: ForwardingConfigRepository,
    private val emailSendRepository: EmailSendRepository,
) {
    fun execute(message: SmsMessage) {
        val config = configRepository.get()
        if (!config.isComplete) return
        emailSendRepository.send(message, config)
    }
}
