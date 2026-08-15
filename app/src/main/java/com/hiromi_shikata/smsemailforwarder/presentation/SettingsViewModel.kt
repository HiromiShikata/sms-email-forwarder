package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    fun saveConfig(
        destinationEmail: String,
        smtpHost: String,
        smtpPort: String,
        smtpUsername: String,
        smtpPassword: String,
    ) {
        val config = ForwardingConfig(
            destinationEmail = destinationEmail,
            smtpHost = smtpHost,
            smtpPort = smtpPort.toIntOrNull() ?: 587,
            smtpUsername = smtpUsername,
            smtpPassword = smtpPassword,
        )
        configUpdateUseCase.execute(config)
        _saved.value = true
    }
}
