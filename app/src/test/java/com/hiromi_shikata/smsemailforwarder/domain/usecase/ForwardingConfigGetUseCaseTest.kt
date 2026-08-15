package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ForwardingConfigGetUseCaseTest {
    private val repository: ForwardingConfigRepository = mock()
    private val useCase = ForwardingConfigGetUseCase(repository)

    @Test
    fun `execute returns config from repository`() {
        val expected = ForwardingConfig(
            destinationEmail = "test@example.com",
            authMode = com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode.SMTP,
            smtpHost = "smtp.gmail.com",
            smtpPort = 587,
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
            googleAccountName = "",
        )
        whenever(repository.get()).thenReturn(expected)

        val result = useCase.execute()

        assertEquals(expected, result)
    }
}
