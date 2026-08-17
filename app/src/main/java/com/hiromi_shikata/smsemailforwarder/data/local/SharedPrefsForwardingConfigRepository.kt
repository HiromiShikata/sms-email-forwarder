package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingConfigRepository

class SharedPrefsForwardingConfigRepository(context: Context) : ForwardingConfigRepository {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun get(): ForwardingConfig = ForwardingConfig(
        destinationEmail = prefs.getString(KEY_DEST_EMAIL, "") ?: "",
        authMode = prefs.getString(KEY_AUTH_MODE, EmailAuthMode.GOOGLE_ACCOUNT.name)
            ?.let { runCatching { EmailAuthMode.valueOf(it) }.getOrDefault(EmailAuthMode.GOOGLE_ACCOUNT) }
            ?: EmailAuthMode.GOOGLE_ACCOUNT,
        smtpHost = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com",
        smtpPort = prefs.getInt(KEY_SMTP_PORT, 587),
        smtpUsername = prefs.getString(KEY_SMTP_USERNAME, "") ?: "",
        smtpPassword = prefs.getString(KEY_SMTP_PASSWORD, "") ?: "",
    )

    override fun save(config: ForwardingConfig) {
        prefs.edit()
            .putString(KEY_DEST_EMAIL, config.destinationEmail)
            .putString(KEY_AUTH_MODE, config.authMode.name)
            .putString(KEY_SMTP_HOST, config.smtpHost)
            .putInt(KEY_SMTP_PORT, config.smtpPort)
            .putString(KEY_SMTP_USERNAME, config.smtpUsername)
            .putString(KEY_SMTP_PASSWORD, config.smtpPassword)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "forwarding_config"
        private const val KEY_DEST_EMAIL = "destination_email"
        private const val KEY_AUTH_MODE = "auth_mode"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USERNAME = "smtp_username"
        private const val KEY_SMTP_PASSWORD = "smtp_password"
    }
}
