package com.hiromi_shikata.smsemailforwarder

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingSetupNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class SmsReceiverDispatchSmsTest {

    private val setupNotifier: SmsForwardingSetupNotifier = mock()
    private val enqueued = mutableListOf<Triple<String, String, Long>>()
    private val enqueue: (String, String, Long) -> Unit = { sender, body, timestamp ->
        enqueued.add(Triple(sender, body, timestamp))
    }

    private val completeConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.GOOGLE_ACCOUNT,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "sender@gmail.com",
        smtpPassword = "app-password",
    )

    @Test
    fun `dispatchSms notifies setup and does not enqueue when config is incomplete`() {
        dispatchSms(
            sender = "+1234567890",
            body = "Test SMS",
            timestamp = 1000L,
            config = ForwardingConfig.EMPTY,
            setupNotifier = setupNotifier,
            enqueue = enqueue,
        )

        verify(setupNotifier).notify("+1234567890")
        assertTrue(enqueued.isEmpty())
    }

    @Test
    fun `dispatchSms enqueues work and does not notify setup when config is complete`() {
        dispatchSms(
            sender = "+1234567890",
            body = "Test SMS",
            timestamp = 1000L,
            config = completeConfig,
            setupNotifier = setupNotifier,
            enqueue = enqueue,
        )

        verify(setupNotifier, never()).notify(org.mockito.kotlin.any())
        assertEquals(1, enqueued.size)
    }

    @Test
    fun `dispatchSms passes sender to setup notifier when config is incomplete`() {
        dispatchSms(
            sender = "ShortCode",
            body = "OTP: 123456",
            timestamp = 2000L,
            config = ForwardingConfig.EMPTY,
            setupNotifier = setupNotifier,
            enqueue = enqueue,
        )

        verify(setupNotifier).notify("ShortCode")
    }

    @Test
    fun `dispatchSms enqueues correct sender body and timestamp when config is complete`() {
        dispatchSms(
            sender = "+9876543210",
            body = "Hello",
            timestamp = 5000L,
            config = completeConfig,
            setupNotifier = setupNotifier,
            enqueue = enqueue,
        )

        assertEquals(1, enqueued.size)
        val (sender, body, timestamp) = enqueued[0]
        assertEquals("+9876543210", sender)
        assertEquals("Hello", body)
        assertEquals(5000L, timestamp)
    }

    @Test
    fun `dispatchSms does not enqueue when config has destination but no credentials`() {
        val incompleteConfig = ForwardingConfig(
            destinationEmail = "dest@example.com",
            authMode = EmailAuthMode.GOOGLE_ACCOUNT,
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            smtpUsername = "",
            smtpPassword = "",
        )

        dispatchSms(
            sender = "+1234567890",
            body = "Test",
            timestamp = 1000L,
            config = incompleteConfig,
            setupNotifier = setupNotifier,
            enqueue = enqueue,
        )

        verify(setupNotifier).notify("+1234567890")
        assertTrue(enqueued.isEmpty())
    }

    @Test
    fun `dispatchSms enqueues when SMTP config is complete`() {
        val smtpConfig = ForwardingConfig(
            destinationEmail = "dest@example.com",
            authMode = EmailAuthMode.SMTP,
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            smtpUsername = "sender@gmail.com",
            smtpPassword = "password",
        )

        dispatchSms(
            sender = "+1234567890",
            body = "Test",
            timestamp = 3000L,
            config = smtpConfig,
            setupNotifier = setupNotifier,
            enqueue = enqueue,
        )

        verify(setupNotifier, never()).notify(org.mockito.kotlin.any())
        assertEquals(1, enqueued.size)
    }
}
