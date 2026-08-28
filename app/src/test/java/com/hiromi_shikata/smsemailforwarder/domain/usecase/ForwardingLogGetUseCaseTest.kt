package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ForwardingLogGetUseCaseTest {
    private val repository: ForwardingLogRepository = mock()
    private val useCase = ForwardingLogGetUseCase(repository)

    @Test
    fun `execute returns all log entries from repository`() {
        val expected = listOf(
            ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FORWARDED, null),
            ForwardingLogEntry(2000L, "+9876543210", ForwardingLogEntryStatus.FAILED, "SMTP error"),
        )
        whenever(repository.getAll()).thenReturn(expected)

        val result = useCase.execute()

        assertEquals(expected, result)
    }

    @Test
    fun `execute returns empty list when repository has no entries`() {
        whenever(repository.getAll()).thenReturn(emptyList())

        val result = useCase.execute()

        assertEquals(emptyList<ForwardingLogEntry>(), result)
    }
}
