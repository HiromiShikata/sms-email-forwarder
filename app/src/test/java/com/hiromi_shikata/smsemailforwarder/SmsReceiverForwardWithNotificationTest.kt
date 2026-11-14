package com.hiromi_shikata.smsemailforwarder

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SMS_FORWARD_MAX_ATTEMPTS
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsForwardAttemptOutcome
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.entity.TransientEmailSendException
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.SmsForwardingErrorNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.SmsForwardUseCase
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException
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

    @Test
    fun `forwardWithNotification schedules retry without notifying on transient failure at first attempt`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        whenever(emailSendRepository.send(any(), any())).thenAnswer {
            throw TransientEmailSendException("connect failed", SocketTimeoutException())
        }
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        val outcome = forwardWithNotification(useCase, message, notifier, logRepository, attemptNumber = 1)

        assertEquals(SmsForwardAttemptOutcome.RETRY_SCHEDULED, outcome)
        verify(notifier, never()).notify(any(), any())
        verify(logRepository).save(argThat {
            sender == "+1234567890" &&
                timestamp == 1000L &&
                status == ForwardingLogEntryStatus.RETRYING &&
                errorMessage == "connect failed"
        })
    }

    @Test
    fun `forwardWithNotification schedules retry for transient failure at every attempt before the last`() {
        val attemptNumbers = listOf(1, 5, SMS_FORWARD_MAX_ATTEMPTS - 1)

        attemptNumbers.forEach { attemptNumber ->
            val configs: ForwardingConfigRepository = mock()
            val sends: EmailSendRepository = mock()
            val alerts: SmsForwardingErrorNotifier = mock()
            val logs: ForwardingLogRepository = mock()
            whenever(configs.get()).thenReturn(completeConfig)
            whenever(sends.send(any(), any())).thenAnswer {
                throw TransientEmailSendException("connect failed", SocketTimeoutException())
            }
            val useCase = SmsForwardUseCase(configs, sends)

            val outcome = forwardWithNotification(useCase, message, alerts, logs, attemptNumber = attemptNumber)

            assertEquals("attempt $attemptNumber", SmsForwardAttemptOutcome.RETRY_SCHEDULED, outcome)
            verify(alerts, never()).notify(any(), any())
            verify(logs).save(argThat {
                status == ForwardingLogEntryStatus.RETRYING && errorMessage == "connect failed"
            })
        }
    }

    @Test
    fun `forwardWithNotification fails and notifies on transient failure at the last attempt`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        whenever(emailSendRepository.send(any(), any())).thenAnswer {
            throw TransientEmailSendException("connect failed", SocketTimeoutException())
        }
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        val outcome = forwardWithNotification(
            useCase,
            message,
            notifier,
            logRepository,
            attemptNumber = SMS_FORWARD_MAX_ATTEMPTS,
        )

        assertEquals(SmsForwardAttemptOutcome.FAILED, outcome)
        verify(notifier).notify("+1234567890", "connect failed")
        verify(logRepository).save(argThat {
            sender == "+1234567890" &&
                status == ForwardingLogEntryStatus.FAILED &&
                errorMessage == "connect failed"
        })
    }

    @Test
    fun `forwardWithNotification fails without retry on non transient failure at first attempt`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        whenever(emailSendRepository.send(any(), any())).thenThrow(RuntimeException("SMTP authentication failed"))
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        val outcome = forwardWithNotification(useCase, message, notifier, logRepository, attemptNumber = 1)

        assertEquals(SmsForwardAttemptOutcome.FAILED, outcome)
        verify(notifier).notify("+1234567890", "SMTP authentication failed")
        verify(logRepository).save(argThat {
            sender == "+1234567890" &&
                status == ForwardingLogEntryStatus.FAILED &&
                errorMessage == "SMTP authentication failed"
        })
    }

    @Test
    fun `forwardWithNotification saves FORWARDED for the same message when a later attempt succeeds`() {
        whenever(configRepository.get()).thenReturn(completeConfig)
        val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

        val outcome = forwardWithNotification(useCase, message, notifier, logRepository, attemptNumber = 3)

        assertEquals(SmsForwardAttemptOutcome.FORWARDED, outcome)
        verify(logRepository).save(argThat {
            sender == "+1234567890" &&
                timestamp == 1000L &&
                status == ForwardingLogEntryStatus.FORWARDED
        })
    }
}
