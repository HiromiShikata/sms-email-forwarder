package com.hiromi_shikata.smsemailforwarder.presentation

import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ForwardingLogEntryDisplayTest {

    @Test
    fun `forwardingLogStatusResId maps each status to its label`() {
        val cases = listOf(
            ForwardingLogEntryStatus.FORWARDED to R.string.status_forwarded,
            ForwardingLogEntryStatus.FAILED to R.string.status_failed,
            ForwardingLogEntryStatus.SETUP_INCOMPLETE to R.string.status_setup_incomplete,
            ForwardingLogEntryStatus.RETRYING to R.string.status_retrying,
        )

        cases.forEach { (status, expectedResId) ->
            assertEquals("status $status", expectedResId, forwardingLogStatusResId(status))
        }
    }

    @Test
    fun `showsForwardingLogErrorMessage shows the error only for failed or retrying entries that carry a message`() {
        val cases = listOf(
            Triple(ForwardingLogEntryStatus.RETRYING, "connect failed", true),
            Triple(ForwardingLogEntryStatus.FAILED, "SMTP error", true),
            Triple(ForwardingLogEntryStatus.RETRYING, null, false),
            Triple(ForwardingLogEntryStatus.FAILED, null, false),
            Triple(ForwardingLogEntryStatus.FORWARDED, null, false),
            Triple(ForwardingLogEntryStatus.SETUP_INCOMPLETE, null, false),
        )

        cases.forEach { (status, errorMessage, expected) ->
            val entry = ForwardingLogEntry(1000L, "+1234567890", status, errorMessage)
            assertEquals("status $status with message $errorMessage", expected, showsForwardingLogErrorMessage(entry))
        }
    }
}
