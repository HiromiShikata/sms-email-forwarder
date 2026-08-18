package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.EmailSendRepository
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EmailSendTestUseCaseTest {
    private val emailSendRepository: EmailSendRepository = mock()
    private val useCase = EmailSendTestUseCase(emailSendRepository)

    private val googleAccountConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.GOOGLE_ACCOUNT,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "sender@gmail.com",
        smtpPassword = "xxxx xxxx xxxx xxxx",
    )

    @Test
    fun `execute returns success when repository send succeeds`() {
        val result = useCase.execute(googleAccountConfig)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `execute delegates to email send repository with given config`() {
        useCase.execute(googleAccountConfig)

        verify(emailSendRepository).send(any(), org.mockito.kotlin.eq(googleAccountConfig))
    }

    @Test
    fun `execute returns failure when repository throws`() {
        doThrow(RuntimeException("Authentication failed")).`when`(emailSendRepository).send(any(), any())

        val result = useCase.execute(googleAccountConfig)

        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }

    @Test
    fun `execute captures error message when repository throws`() {
        val errorMessage = "535 Authentication credentials invalid"
        doThrow(RuntimeException(errorMessage)).`when`(emailSendRepository).send(any(), any())

        val result = useCase.execute(googleAccountConfig)

        assertTrue(result.exceptionOrNull()?.message?.contains(errorMessage) == true)
    }
}
