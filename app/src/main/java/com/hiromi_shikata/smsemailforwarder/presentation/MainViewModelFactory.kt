package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase

class MainViewModelFactory(
    private val configGetUseCase: ForwardingConfigGetUseCase,
    private val updateCheckUseCase: AppUpdateCheckUseCase,
    private val currentVersion: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(configGetUseCase, updateCheckUseCase, currentVersion) as T
}
