package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigUpdateUseCase

class SettingsViewModelFactory(
    private val configGetUseCase: ForwardingConfigGetUseCase,
    private val configUpdateUseCase: ForwardingConfigUpdateUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(configGetUseCase, configUpdateUseCase) as T
}
