package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingLogGetUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ForwardingLogViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val forwardingLogGetUseCase: ForwardingLogGetUseCase = mock()
    private val viewModel = ForwardingLogViewModel(forwardingLogGetUseCase)

    @Test
    fun `loadLog sets log live data from use case`() {
        val expected = listOf(
            ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FORWARDED, null),
            ForwardingLogEntry(2000L, "+9876543210", ForwardingLogEntryStatus.FAILED, "SMTP error"),
        )
        whenever(forwardingLogGetUseCase.execute()).thenReturn(expected)

        viewModel.loadLog()

        assertEquals(expected, viewModel.log.value)
    }

    @Test
    fun `loadLog sets empty list when use case returns no entries`() {
        whenever(forwardingLogGetUseCase.execute()).thenReturn(emptyList())

        viewModel.loadLog()

        assertTrue(viewModel.log.value?.isEmpty() == true)
    }
}
