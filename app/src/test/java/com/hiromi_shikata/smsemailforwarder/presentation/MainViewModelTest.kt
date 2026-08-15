package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val configGetUseCase: ForwardingConfigGetUseCase = mock()
    private val updateCheckUseCase: AppUpdateCheckUseCase = mock()

    private val completeConfig = ForwardingConfig(
        destinationEmail = "dest@example.com",
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        smtpUsername = "user@gmail.com",
        smtpPassword = "password",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(currentVersion: String = "1.0.0") =
        MainViewModel(configGetUseCase, updateCheckUseCase, currentVersion, testDispatcher)

    @Test
    fun `loadConfig sets config live data from use case`() {
        whenever(configGetUseCase.execute()).thenReturn(completeConfig)
        val viewModel = createViewModel()

        viewModel.loadConfig()

        assertEquals(completeConfig, viewModel.config.value)
    }

    @Test
    fun `checkForUpdate sets update live data when newer version is available`() = runTest {
        val update = AppUpdate("1.1.0", "https://example.com/app.apk", true)
        whenever(updateCheckUseCase.execute("1.0.0")).thenReturn(update)
        val viewModel = createViewModel("1.0.0")

        viewModel.checkForUpdate()

        assertEquals(update, viewModel.update.value)
    }

    @Test
    fun `checkForUpdate sets null update when no newer version`() = runTest {
        val update = AppUpdate("1.0.0", "https://example.com/app.apk", false)
        whenever(updateCheckUseCase.execute("1.0.0")).thenReturn(update)
        val viewModel = createViewModel("1.0.0")

        viewModel.checkForUpdate()

        assertNull(viewModel.update.value)
    }

    @Test
    fun `checkForUpdate sets null update when network call fails`() = runTest {
        whenever(updateCheckUseCase.execute("1.0.0")).thenThrow(RuntimeException("Network error"))
        val viewModel = createViewModel("1.0.0")

        viewModel.checkForUpdate()

        assertNull(viewModel.update.value)
    }
}
