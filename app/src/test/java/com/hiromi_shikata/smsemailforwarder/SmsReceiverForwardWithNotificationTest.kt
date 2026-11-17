package com.hiromi_shikata.smsemailforwarder

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SmsReceiverForwardWithNotificationTest {

    private val configRepository: ForwardingConfigRepository = mock()
    private val emailSendRepository: EmailSendRepository = mock()
    private val notifier: SmsForwardingErrorNotifier = mock()
    private val logRepository: ForwardingLogRepository = mock()

    private val completeConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.SMTP,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "sender@gmail.com",
        smtpPassword = "password",
    )

    private val message = SmsMessage(
        sender = "+1234567890",
        body = "Test SMS",
        timestamp = 1000L,
    )

    @Test
    fun `forwardWithNotification notifies on send failure`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        whenever(emailSendRepository.send(any(), any())).thenThrow(RuntimeException("SMTP send error"))
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        forwardWithNotification(useCase, message, notifier, logRepository)

        verify(notifier).notify("+1234567890", "SMTP send error")
    }

    @Test
    fun `forwardWithNotification does not notify on successful send`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        forwardWithNotification(useCase, message, notifier, logRepository)

        verify(notifier, never()).notify(any(), any())
    }

    @Test
    fun `forwardWithNotification does not notify when config is incomplete`() {
        whenever(configRepository.get()).thenReturn(ForwardingConfig.EMPTY)
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        forwardWithNotification(useCase, message, notifier, logRepository)

        verify(notifier, never()).notify(any(), any())
    }

    @Test
    fun `forwardWithNotification uses exception message in notification`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        whenever(emailSendRepository.send(any(), any()))
            .thenThrow(IllegalStateException("Authentication credentials invalid for sender@gmail.com."))

        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        forwardWithNotification(useCase, message, notifier, logRepository)

        verify(notifier).notify(
            "+1234567890",
            "Authentication credentials invalid for sender@gmail.com.",
        )
    }

    @Test
    fun `forwardWithNotification saves FORWARDED log entry on successful send`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        forwardWithNotification(useCase, message, notifier, logRepository)

        verify(logRepository).save(argThat {
            sender == "+1234567890" &&
                status == ForwardingLogEntryStatus.FORWARDED &&
                errorMessage == null
        })
    }

    @Test
    fun `forwardWithNotification saves FAILED log entry with error message on send failure`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        whenever(emailSendRepository.send(any(), any())).thenThrow(RuntimeException("SMTP timeout"))
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        forwardWithNotification(useCase, message, notifier, logRepository)

        verify(logRepository).save(argThat {
            sender == "+1234567890" &&
                status == ForwardingLogEntryStatus.FAILED &&
                errorMessage == "SMTP timeout"
        })
    }
}
