package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingLogGetUseCase

class ForwardingLogViewModelFactory(
    private val forwardingLogGetUseCase: ForwardingLogGetUseCase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ForwardingLogViewModel(forwardingLogGetUseCase) as T
}
