package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsMessage
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SmsForwardUseCaseTest {
    private val configRepository: ForwardingConfigRepository = mock()
    private val emailSendRepository: EmailSendRepository = mock()
    private val useCase = SmsForwardUseCase(configRepository, emailSendRepository)

    private val completeConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode.SMTP,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "sender@gmail.com",
        smtpPassword = "app-password",
    )

    private val smsMessage = SmsMessage(
        sender = "+1234567890",
        body = "Test SMS body",
        timestamp = 1000L,
    )

    @Test
    fun `execute sends email when config is complete`() {
        whenever(configRepository.get()).thenReturn(completeConfig)

        useCase.execute(smsMessage)

        verify(emailSendRepository).send(smsMessage, completeConfig)
    }

    @Test
    fun `execute does not send email when config is incomplete`() {
        whenever(configRepository.get()).thenReturn(ForwardingConfig.EMPTY)

        useCase.execute(smsMessage)

        verify(emailSendRepository, never()).send(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }
}
