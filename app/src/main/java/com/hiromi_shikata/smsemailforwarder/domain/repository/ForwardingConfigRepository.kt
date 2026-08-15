package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig

interface ForwardingConfigRepository {
    fun get(): ForwardingConfig
    fun save(config: ForwardingConfig)
}
