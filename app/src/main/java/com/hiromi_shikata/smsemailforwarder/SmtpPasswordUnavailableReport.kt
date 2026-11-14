package com.hiromi_shikata.smsemailforwarder

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus

internal fun reportSmtpPasswordUnavailable(
    senders: List<String>,
    errorMessage: String,
    timestamp: Long,
    notifyError: (sender: String, message: String) -> Unit,
    saveLog: (ForwardingLogEntry) -> Unit,
) {
    senders.distinct().forEach { sender ->
        notifyError(sender, errorMessage)
        saveLog(ForwardingLogEntry(timestamp = timestamp, sender = sender, status = ForwardingLogEntryStatus.FAILED, errorMessage = errorMessage))
    }
}
