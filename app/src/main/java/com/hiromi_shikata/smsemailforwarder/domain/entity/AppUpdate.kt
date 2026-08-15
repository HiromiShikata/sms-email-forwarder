package com.hiromi_shikata.smsemailforwarder.domain.entity

data class AppUpdate(
    val latestVersion: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean,
)
