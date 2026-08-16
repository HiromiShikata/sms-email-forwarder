package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.OAuthTokenProvider
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.HttpURLConnection

class GmailApiEmailSendRepositoryTest {
    private val tokenProvider: OAuthTokenProvider = mock()
    private val apiCaller: GmailApiCaller = mock()

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
        whenever(tokenProvider.getToken(any())).thenReturn("valid-token")
        whenever(apiCaller.call(any(), any())).thenReturn(Pair(HttpURLConnection.HTTP_OK, ""))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)

        verify(tokenProvider).getToken("sender@gmail.com")
    }

    @Test(expected = IllegalStateException::class)
    fun `send throws when token provider throws`() {
        whenever(tokenProvider.getToken(any()))
            .thenThrow(IllegalStateException("Google account not found on device: sender@gmail.com"))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)
    }

    @Test
    fun `send does not invalidate token on successful response`() {
        whenever(tokenProvider.getToken(any())).thenReturn("valid-token")
        whenever(apiCaller.call(any(), any())).thenReturn(Pair(HttpURLConnection.HTTP_OK, ""))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)

        verify(tokenProvider, never()).invalidateToken(any(), any())
    }

    @Test
    fun `send invalidates stale token and retries with fresh token on 401`() {
        whenever(tokenProvider.getToken(any()))
            .thenReturn("stale-token")
            .thenReturn("fresh-token")
        whenever(apiCaller.call(eq("stale-token"), any())).thenReturn(Pair(HttpURLConnection.HTTP_UNAUTHORIZED, ""))
        whenever(apiCaller.call(eq("fresh-token"), any())).thenReturn(Pair(HttpURLConnection.HTTP_OK, ""))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)

        verify(tokenProvider).invalidateToken("sender@gmail.com", "stale-token")
    }

    @Test
    fun `send invalidates stale token and retries with fresh token on 403`() {
        whenever(tokenProvider.getToken(any()))
            .thenReturn("stale-token")
            .thenReturn("fresh-token")
        whenever(apiCaller.call(eq("stale-token"), any())).thenReturn(Pair(HttpURLConnection.HTTP_FORBIDDEN, ""))
        whenever(apiCaller.call(eq("fresh-token"), any())).thenReturn(Pair(HttpURLConnection.HTTP_OK, ""))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)

        verify(tokenProvider).invalidateToken("sender@gmail.com", "stale-token")
    }

    @Test(expected = java.io.IOException::class)
    fun `send throws after retry still fails on 401`() {
        whenever(tokenProvider.getToken(any()))
            .thenReturn("stale-token")
            .thenReturn("fresh-token")
        whenever(apiCaller.call(any(), any())).thenReturn(Pair(HttpURLConnection.HTTP_UNAUTHORIZED, "Auth error"))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)
    }

    @Test(expected = java.io.IOException::class)
    fun `send throws without retry on non-auth error`() {
        whenever(tokenProvider.getToken(any())).thenReturn("valid-token")
        whenever(apiCaller.call(any(), any())).thenReturn(Pair(HttpURLConnection.HTTP_INTERNAL_ERROR, "Server error"))

        GmailApiEmailSendRepository(tokenProvider, apiCaller).send(smsMessage, config)
    }
}
