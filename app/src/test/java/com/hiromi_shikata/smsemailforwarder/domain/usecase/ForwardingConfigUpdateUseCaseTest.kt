package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ForwardingConfigUpdateUseCaseTest {
    private val repository: ForwardingConfigRepository = mock()
    private val useCase = ForwardingConfigUpdateUseCase(repository)

    @Test
    fun `execute saves config to repository`() {
        val config = ForwardingConfig(
            destinationEmail = "dest@example.com",
            authMode = com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode.SMTP,
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
            googleAccountName = "",
        )

        useCase.execute(config)

        verify(repository).save(config)
    }
}
