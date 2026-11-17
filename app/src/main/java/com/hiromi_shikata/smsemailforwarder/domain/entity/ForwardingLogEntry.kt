package com.hiromi_shikata.smsemailforwarder.domain.entity

enum class ForwardingLogEntryStatus {
    FORWARDED,
    FAILED,
    SETUP_INCOMPLETE,
}

data class ForwardingLogEntry(
    val timestamp: Long,
    val sender: String,
    val status: ForwardingLogEntryStatus,
    val errorMessage: String?,
)
