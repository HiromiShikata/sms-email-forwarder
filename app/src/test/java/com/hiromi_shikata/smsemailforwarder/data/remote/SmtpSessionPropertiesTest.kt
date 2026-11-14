package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SmtpSessionPropertiesTest {

    @Test
    fun `buildSmtpSessionProperties requires STARTTLS and server identity verification for every config`() {
        val cases = listOf(
            ForwardingConfig(
                destinationEmail = "dest@example.com",
                authMode = EmailAuthMode.SMTP,
                smtpHost = "smtp.example.com",
                smtpPort = 587,
                smtpUsername = "user@example.com",
                smtpPassword = "test-password",
            ),
            ForwardingConfig(
                destinationEmail = "dest@example.com",
                authMode = EmailAuthMode.SMTP,
                smtpHost = "mail.another-example.org",
                smtpPort = 465,
                smtpUsername = "user@another-example.org",
                smtpPassword = "test-password",
            ),
        )

        cases.forEach { config ->
            val props = buildSmtpSessionProperties(config)

            assertEquals(
                "mail.smtp.host for host ${config.smtpHost}",
                config.smtpHost,
                props.getProperty("mail.smtp.host"),
            )
            assertEquals(
                "mail.smtp.port for host ${config.smtpHost}",
                config.smtpPort.toString(),
                props.getProperty("mail.smtp.port"),
            )
            assertEquals(
                "mail.smtp.auth for host ${config.smtpHost}",
                "true",
                props.getProperty("mail.smtp.auth"),
            )
            assertEquals(
                "mail.smtp.starttls.enable for host ${config.smtpHost}",
                "true",
                props.getProperty("mail.smtp.starttls.enable"),
            )
            assertEquals(
                "mail.smtp.starttls.required for host ${config.smtpHost}",
                "true",
                props.getProperty("mail.smtp.starttls.required"),
            )
            assertEquals(
                "mail.smtp.ssl.checkserveridentity for host ${config.smtpHost}",
                "true",
                props.getProperty("mail.smtp.ssl.checkserveridentity"),
            )
            assertFalse(
                "mail.smtp.ssl.trust must be absent for host ${config.smtpHost}",
                props.containsKey("mail.smtp.ssl.trust"),
            )
        }
    }
}
