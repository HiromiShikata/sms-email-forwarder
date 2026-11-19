package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository

class ModeRoutingEmailSendRepository(
    private val smtp: EmailSendRepository,
    private val httpsApi: EmailSendRepository,
) : EmailSendRepository {
    override fun send(message: SmsMessage, config: ForwardingConfig) {
        when (config.authMode) {
            EmailAuthMode.SMTP, EmailAuthMode.GOOGLE_ACCOUNT -> smtp.send(message, config)
            EmailAuthMode.HTTPS_API -> httpsApi.send(message, config)
        }
    }
}
