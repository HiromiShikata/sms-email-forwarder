package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase

class SettingsViewModel(
    private val configGetUseCase: ForwardingConfigGetUseCase,
    private val configUpdateUseCase: ForwardingConfigUpdateUseCase,
) : ViewModel() {
    private val _config = MutableLiveData<ForwardingConfig>()
    val config: LiveData<ForwardingConfig> = _config

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    fun loadConfig() {
        _config.value = configGetUseCase.execute()
    }

    fun saveSmtpConfig(
        destinationEmail: String,
        smtpHost: String,
        smtpPort: String,
        smtpUsername: String,
        smtpPassword: String,
    ) {
        val existing = _config.value ?: ForwardingConfig.EMPTY
        configUpdateUseCase.execute(
            existing.copy(
                destinationEmail = destinationEmail,
                authMode = EmailAuthMode.SMTP,
                smtpHost = smtpHost,
                smtpPort = smtpPort.toIntOrNull() ?: 587,
                smtpUsername = smtpUsername,
                smtpPassword = smtpPassword,
            ),
        )
        _saved.value = true
    }

    fun saveGoogleAccountConfig(destinationEmail: String, googleAccountName: String) {
        val existing = _config.value ?: ForwardingConfig.EMPTY
        configUpdateUseCase.execute(
            existing.copy(
                destinationEmail = destinationEmail,
                authMode = EmailAuthMode.GOOGLE_ACCOUNT,
                googleAccountName = googleAccountName,
            ),
        )
        _saved.value = true
    }
}
