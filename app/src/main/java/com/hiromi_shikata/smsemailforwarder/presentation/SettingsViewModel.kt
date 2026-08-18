package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiromi_shikata.smsemailforwarder.domain.entity.EmailAuthMode
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.EmailSendTestUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val configGetUseCase: ForwardingConfigGetUseCase,
    private val configUpdateUseCase: ForwardingConfigUpdateUseCase,
    private val emailSendTestUseCase: EmailSendTestUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _config = MutableLiveData<ForwardingConfig>()
    val config: LiveData<ForwardingConfig> = _config

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _testResult = MutableLiveData<Result<Unit>>()
    val testResult: LiveData<Result<Unit>> = _testResult

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

    fun saveGoogleAccountConfig(
        destinationEmail: String,
        gmailAddress: String,
        appPassword: String,
    ) {
        val existing = _config.value ?: ForwardingConfig.EMPTY
        configUpdateUseCase.execute(
            existing.copy(
                destinationEmail = destinationEmail,
                authMode = EmailAuthMode.GOOGLE_ACCOUNT,
                smtpHost = "smtp.gmail.com",
                smtpPort = 587,
                smtpUsername = gmailAddress,
                smtpPassword = appPassword,
            ),
        )
        _saved.value = true
    }

    fun testGoogleAccountConfig(
        destinationEmail: String,
        gmailAddress: String,
        appPassword: String,
    ) {
        viewModelScope.launch(ioDispatcher) {
            _testResult.postValue(
                emailSendTestUseCase.execute(
                    ForwardingConfig(
                        destinationEmail = destinationEmail,
                        authMode = EmailAuthMode.GOOGLE_ACCOUNT,
                        smtpHost = "smtp.gmail.com",
                        smtpPort = 587,
                        smtpUsername = gmailAddress,
                        smtpPassword = appPassword,
                    ),
                ),
            )
        }
    }

    fun testSmtpConfig(
        destinationEmail: String,
        smtpHost: String,
        smtpPort: String,
        smtpUsername: String,
        smtpPassword: String,
    ) {
        viewModelScope.launch(ioDispatcher) {
            _testResult.postValue(
                emailSendTestUseCase.execute(
                    ForwardingConfig(
                        destinationEmail = destinationEmail,
                        authMode = EmailAuthMode.SMTP,
                        smtpHost = smtpHost,
                        smtpPort = smtpPort.toIntOrNull() ?: 587,
                        smtpUsername = smtpUsername,
                        smtpPassword = smtpPassword,
                    ),
                ),
            )
        }
    }
}
