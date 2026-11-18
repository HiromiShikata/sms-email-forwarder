package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import java.util.Properties

internal fun buildSmtpSessionProperties(config: ForwardingConfig): Properties =
    Properties().apply {
        put("mail.smtp.host", config.smtpHost)
        put("mail.smtp.port", config.smtpPort.toString())
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.starttls.required", "true")
        put("mail.smtp.ssl.checkserveridentity", "true")
        put("mail.smtp.connectiontimeout", SMTP_TIMEOUT_MILLIS.toString())
        put("mail.smtp.timeout", SMTP_TIMEOUT_MILLIS.toString())
        put("mail.smtp.writetimeout", SMTP_TIMEOUT_MILLIS.toString())
    }

private const val SMTP_TIMEOUT_MILLIS = 30_000
