package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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

    private val storedConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "password",
    )

    @Test
    fun `loadConfig sets config live data from use case`() {
        whenever(configGetUseCase.execute()).thenReturn(storedConfig)

        viewModel.loadConfig()

        assertEquals(storedConfig, viewModel.config.value)
    }

    @Test
    fun `saveConfig delegates to use case with correct ForwardingConfig`() {
        viewModel.saveConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.gmail.com",
            smtpPort = "587",
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
        )

        verify(configUpdateUseCase).execute(any())
    }

    @Test
    fun `saveConfig sets saved to true after saving`() {
        viewModel.saveConfig(
            destinationEmail = "dest@example.com",
            smtpHost = "smtp.gmail.com",
            smtpPort = "587",
            smtpUsername = "user@gmail.com",
            smtpPassword = "password",
        )

        assertTrue(viewModel.saved.value == true)
    }

    @Test
    fun `saveConfig defaults to port 587 when port is not a number`() {
        viewModel.saveConfig(
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
}
