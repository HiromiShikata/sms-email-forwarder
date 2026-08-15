package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SettingsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val configGetUseCase: ForwardingConfigGetUseCase = mock()
    private val configUpdateUseCase: ForwardingConfigUpdateUseCase = mock()
    private val viewModel = SettingsViewModel(configGetUseCase, configUpdateUseCase)

    private val storedSmtpConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        authMode = EmailAuthMode.SMTP,
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "password",
        googleAccountName = "",
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
    fun `saveGoogleAccountConfig delegates to use case with google account auth mode`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            googleAccountName = "user@gmail.com",
        )

        verify(configUpdateUseCase).execute(
            org.mockito.kotlin.argThat {
                authMode == EmailAuthMode.GOOGLE_ACCOUNT && googleAccountName == "user@gmail.com"
            },
        )
    }

    @Test
    fun `saveGoogleAccountConfig sets saved to true after saving`() {
        whenever(configGetUseCase.execute()).thenReturn(storedSmtpConfig)
        viewModel.loadConfig()

        viewModel.saveGoogleAccountConfig(
            destinationEmail = "dest@example.com",
            googleAccountName = "user@gmail.com",
        )

        assertTrue(viewModel.saved.value == true)
    }
}
