package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.EmailSendTestUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val configGetUseCase: ForwardingConfigGetUseCase = mock()
    private val configUpdateUseCase: ForwardingConfigUpdateUseCase = mock()
    private val emailSendTestUseCase: EmailSendTestUseCase = mock()
    private val viewModel = SettingsViewModel(
        configGetUseCase,
        configUpdateUseCase,
        emailSendTestUseCase,
        testDispatcher,
    )

    private val storedSmtpConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.SMTP,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "password",
    )

    @Test
    fun `loadConfig sets config live data from use case`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)

        viewModel.loadConfig()

        assertEquals(storedSmtpConfig, viewModel.config.value)
    }

    @Test
    fun `saveSmtpConfig delegates to use case with smtp auth mode`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveSmtpConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.gmail.com",
            smtpPort = "587",
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
        )

        verify(configUpdateUseCase).execute(
            org.mockito.kotlin.argThat { authMode == EmailAuthMode.SMTP },
        )
    }

    @Test
    fun `saveSmtpConfig sets saved to true after saving`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveSmtpConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.gmail.com",
            smtpPort = "587",
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
        )

        assertTrue(viewModel.saved.value == true)
    }

    @Test
    fun `saveSmtpConfig defaults to port 587 when port is not a number`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveSmtpConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.gmail.com",
            smtpPort = "invalid",
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
        )

        verify(configUpdateUseCase).execute(
            org.mockito.kotlin.argThat { smtpPort == 587 },
        )
    }

    @Test
    fun `saveGoogleAccountConfig delegates to use case with google account auth mode using smtp gmail server`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            gmailAddress = "user@gmail.com",
            appPassword = "xxxx-yyyy-zzzz",
        )

        verify(configUpdateUseCase).execute(
            org.mockito.kotlin.argThat {
                authMode == EmailAuthMode.GOOGLE_ACCOUNT &&
                    smtpHost == "smtp.gmail.com" &&
                    smtpPort == 587 &&
                    smtpUsername == "user@gmail.com" &&
                    smtpPassword == "xxxx-yyyy-zzzz"
            },
        )
    }

    @Test
    fun `saveGoogleAccountConfig sets saved to true after saving`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            gmailAddress = "user@gmail.com",
            appPassword = "xxxx-yyyy-zzzz",
        )

        assertTrue(viewModel.saved.value == true)
    }

    @Test
    fun `testGoogleAccountConfig posts success result when use case succeeds`() = runTest {
        whenever(emailSendTestUseCase.execute(any())).thenReturn(Result.success(Unit))

        viewModel.testGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            gmailAddress = "user@gmail.com",
            appPassword = "xxxx xxxx xxxx xxxx",
        )

        assertTrue(viewModel.testResult.value?.isSuccess == true)
    }

    @Test
    fun `testGoogleAccountConfig posts failure result when use case fails`() = runTest {
        val error = RuntimeException("535 Authentication credentials invalid")
        whenever(emailSendTestUseCase.execute(any())).thenReturn(Result.failure(error))

        viewModel.testGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            gmailAddress = "user@gmail.com",
            appPassword = "wrong-password",
        )

        assertTrue(viewModel.testResult.value?.isFailure == true)
    }

    @Test
    fun `testGoogleAccountConfig delegates to use case with gmail smtp config`() = runTest {
        whenever(emailSendTestUseCase.execute(any())).thenReturn(Result.success(Unit))

        viewModel.testGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            gmailAddress = "user@gmail.com",
            appPassword = "xxxx xxxx xxxx xxxx",
        )

        verify(emailSendTestUseCase).execute(
            org.mockito.kotlin.argThat {
                authMode == EmailAuthMode.GOOGLE_ACCOUNT &&
                    smtpHost == "smtp.gmail.com" &&
                    smtpPort == 587 &&
                    smtpUsername == "user@gmail.com" &&
                    smtpPassword == "xxxx xxxx xxxx xxxx"
            },
        )
    }

    @Test
    fun `testSmtpConfig posts success result when use case succeeds`() = runTest {
        whenever(emailSendTestUseCase.execute(any())).thenReturn(Result.success(Unit))

        viewModel.testSmtpConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.gmail.com",
            smtpPort = "587",
            smtpUsername = "user@gmail.com",
            smtpPassword = "app-password",
        )

        assertTrue(viewModel.testResult.value?.isSuccess == true)
    }

    @Test
    fun `testSmtpConfig delegates to use case with smtp config`() = runTest {
        whenever(emailSendTestUseCase.execute(any())).thenReturn(Result.success(Unit))

        viewModel.testSmtpConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.example.com",
            smtpPort = "465",
            smtpUsername = "user@example.com",
            smtpPassword = "password",
        )

        verify(emailSendTestUseCase).execute(
            org.mockito.kotlin.argThat {
                authMode == EmailAuthMode.SMTP &&
                    smtpHost == "smtp.example.com" &&
                    smtpPort == 465 &&
                    smtpUsername == "user@example.com" &&
                    smtpPassword == "password"
            },
        )
    }
}
