package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.OAuthTokenProvider
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GmailApiEmailSendRepositoryTest {
    private val tokenProvider: OAuthTokenProvider = mock()
    private val repository = GmailApiEmailSendRepository(tokenProvider)

    private val config = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.GOOGLE_ACCOUNT,
        smtpHost = "",
        smtpPort = 0,
        smtpUsername = "",
        smtpPassword = "",
        googleAccountName = "sender@gmail.com",
    )

    private val smsMessage = SmsMessage(
        sender = "+1234567890",
        body = "Test SMS body",
        timestamp = 1000L,
    )

    @Test
    fun `send requests token for the configured google account`() {
        whenever(tokenProvider.getToken("sender@gmail.com")).thenReturn("valid-token")

        try {
            repository.send(smsMessage, config)
        } catch (_: Exception) {
        }

        verify(tokenProvider).getToken("sender@gmail.com")
    }

    @Test(expected = IllegalStateException::class)
    fun `send throws when token provider throws`() {
        whenever(tokenProvider.getToken("sender@gmail.com"))
            .thenThrow(IllegalStateException("Google account not found on device: sender@gmail.com"))

        repository.send(smsMessage, config)
    }
}
