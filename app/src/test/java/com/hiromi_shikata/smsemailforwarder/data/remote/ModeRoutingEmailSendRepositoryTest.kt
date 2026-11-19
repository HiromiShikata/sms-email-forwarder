package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ModeRoutingEmailSendRepositoryTest {
    @Test
    fun `send delegates to the sender that matches the configured auth mode`() {
        val cases = listOf(
            RoutingCase(EmailAuthMode.SMTP, expectedSender = "smtp"),
            RoutingCase(EmailAuthMode.GOOGLE_ACCOUNT, expectedSender = "smtp"),
            RoutingCase(EmailAuthMode.HTTPS_API, expectedSender = "https"),
        )
        for (case in cases) {
            val smtp = RecordingSender("smtp")
            val httpsApi = RecordingSender("https")
            val router = ModeRoutingEmailSendRepository(smtp, httpsApi)

            router.send(SmsMessage(sender = "+81", body = "hi", timestamp = 0L), configFor(case.mode))

            assertEquals(case.mode.name, case.expectedSender, listOf(smtp, httpsApi).single { it.calls > 0 }.name)
        }
    }

    private fun configFor(mode: EmailAuthMode) = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = mode,
        smtpHost = "smtp.example.net",
        smtpPort = 587,
        smtpUsername = "sender@example.net",
        smtpPassword = "secret",
    )
}

private class RecordingSender(val name: String) : EmailSendRepository {
    var calls = 0

    override fun send(message: SmsMessage, config: ForwardingConfig) {
        calls += 1
    }
}

private data class RoutingCase(val mode: EmailAuthMode, val expectedSender: String)
