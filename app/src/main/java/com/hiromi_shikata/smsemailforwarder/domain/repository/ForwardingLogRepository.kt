package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry

interface ForwardingLogRepository {
    fun save(entry: ForwardingLogEntry)
    fun getAll(): List<ForwardingLogEntry>
}
