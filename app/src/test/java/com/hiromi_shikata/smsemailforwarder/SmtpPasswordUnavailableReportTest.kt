package com.hiromi_shikata.smsemailforwarder

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SmtpPasswordUnavailableReportTest {

    private data class ReportCase(
        val name: String,
        val senders: List<String>,
        val expectedSenders: List<String>,
    )

    private val errorMessage = "SMTP password is unreadable"
    private val timestamp = 1700000000000L

    private val reportCases = listOf(
        ReportCase(
            name = "two distinct senders are reported in first-seen order",
            senders = listOf("+81111111111", "+81222222222"),
            expectedSenders = listOf("+81111111111", "+81222222222"),
        ),
        ReportCase(
            name = "duplicate sender is reported once at its first position",
            senders = listOf("+81111111111", "+81222222222", "+81111111111"),
            expectedSenders = listOf("+81111111111", "+81222222222"),
        ),
        ReportCase(
            name = "empty sender list reports nothing",
            senders = emptyList(),
            expectedSenders = emptyList(),
        ),
        ReportCase(
            name = "sender literally named Unknown is treated as any other sender",
            senders = listOf("Unknown", "+81333333333"),
            expectedSenders = listOf("Unknown", "+81333333333"),
        ),
    )

    @Test
    fun `reportSmtpPasswordUnavailable notifies and logs each distinct sender once in first-seen order`() {
        reportCases.forEach { testCase ->
            val events = mutableListOf<String>()
            val notifications = mutableListOf<Pair<String, String>>()
            val savedLogs = mutableListOf<ForwardingLogEntry>()

            reportSmtpPasswordUnavailable(
                senders = testCase.senders,
                errorMessage = errorMessage,
                timestamp = timestamp,
                notifyError = { sender, message ->
                    events.add("notify:$sender")
                    notifications.add(sender to message)
                },
                saveLog = { entry ->
                    events.add("log:${entry.sender}")
                    savedLogs.add(entry)
                },
            )

            val expectedNotifications = testCase.expectedSenders.map { sender -> sender to errorMessage }
            val expectedLogs = testCase.expectedSenders.map { sender ->
                ForwardingLogEntry(
                    timestamp = timestamp,
                    sender = sender,
                    status = ForwardingLogEntryStatus.FAILED,
                    errorMessage = errorMessage,
                )
            }
            val expectedEvents = testCase.expectedSenders.flatMap { sender ->
                listOf("notify:$sender", "log:$sender")
            }

            assertEquals(testCase.name, expectedNotifications, notifications)
            assertEquals(testCase.name, expectedLogs, savedLogs)
            assertEquals(testCase.name, expectedEvents, events)
        }
    }
}
