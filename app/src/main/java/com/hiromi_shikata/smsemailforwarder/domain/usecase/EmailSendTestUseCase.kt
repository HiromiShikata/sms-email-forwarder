package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository

class EmailSendTestUseCase(
    private val emailSendRepository: EmailSendRepository,
) {
    fun execute(config: ForwardingConfig): Result<Unit> = runCatching {
        emailSendRepository.send(
            SmsMessage(
                sender = "SMS Forwarder Test",
                body = "This is a test message to verify your email configuration is working correctly.",
                timestamp = System.currentTimeMillis(),
            ),
            config,
        )
    }
}
