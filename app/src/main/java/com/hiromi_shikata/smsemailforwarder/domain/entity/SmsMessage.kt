package com.hiromi_shikata.smsemailforwarder.domain.entity

data class SmsMessage(
    val sender: String,
    val body: String,
    val timestamp: Long,
)
