package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingLogGetUseCase

class ForwardingLogViewModel(
    private val forwardingLogGetUseCase: ForwardingLogGetUseCase,
) : ViewModel() {
    private val _log = MutableLiveData<List<ForwardingLogEntry>>()
    val log: LiveData<List<ForwardingLogEntry>> = _log

    fun loadLog() {
        _log.value = forwardingLogGetUseCase.execute()
    }
}
