package com.hiromi_shikata.smsemailforwarder.domain.entity

data class AppRelease(
    val version: String,
    val changeLines: List<String>,
)
