package com.hiromi_shikata.smsemailforwarder.domain.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ForwardingLogEntryTest {

    @Test
    fun `ForwardingLogEntry holds all fields`() {
        val entry = ForwardingLogEntry(
            timestamp = 1000L,
            sender = "+1234567890",
            status = ForwardingLogEntryStatus.FORWARDED,
            errorMessage = null,
        )

        assertEquals(1000L, entry.timestamp)
        assertEquals("+1234567890", entry.sender)
        assertEquals(ForwardingLogEntryStatus.FORWARDED, entry.status)
        assertNull(entry.errorMessage)
    }

    @Test
    fun `ForwardingLogEntry stores error message for FAILED status`() {
        val entry = ForwardingLogEntry(
            timestamp = 2000L,
            sender = "+9876543210",
            status = ForwardingLogEntryStatus.FAILED,
            errorMessage = "SMTP authentication failed",
        )

        assertEquals(ForwardingLogEntryStatus.FAILED, entry.status)
        assertEquals("SMTP authentication failed", entry.errorMessage)
    }

    @Test
    fun `ForwardingLogEntryStatus has FORWARDED value`() {
        assertEquals("FORWARDED", ForwardingLogEntryStatus.FORWARDED.name)
    }

    @Test
    fun `ForwardingLogEntryStatus has FAILED value`() {
        assertEquals("FAILED", ForwardingLogEntryStatus.FAILED.name)
    }

    @Test
    fun `ForwardingLogEntryStatus has SETUP_INCOMPLETE value`() {
        assertEquals("SETUP_INCOMPLETE", ForwardingLogEntryStatus.SETUP_INCOMPLETE.name)
    }

    @Test
    fun `ForwardingLogEntry data class equality works by value`() {
        val entry1 = ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FORWARDED, null)
        val entry2 = ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FORWARDED, null)

        assertEquals(entry1, entry2)
    }

    @Test
    fun `ForwardingLogEntry copy produces distinct entry with changed field`() {
        val original = ForwardingLogEntry(1000L, "+1234567890", ForwardingLogEntryStatus.FORWARDED, null)
        val copied = original.copy(status = ForwardingLogEntryStatus.FAILED, errorMessage = "error")

        assertEquals(ForwardingLogEntryStatus.FAILED, copied.status)
        assertEquals("error", copied.errorMessage)
        assertEquals(original.timestamp, copied.timestamp)
        assertEquals(original.sender, copied.sender)
    }
}
