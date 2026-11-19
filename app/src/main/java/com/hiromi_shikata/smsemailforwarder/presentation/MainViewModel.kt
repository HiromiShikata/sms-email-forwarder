package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateStatusGetUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase

class MainViewModel(
    private val configGetUseCase: ForwardingConfigGetUseCase,
    private val updateStatusGetUseCase: AppUpdateStatusGetUseCase,
    private val currentVersion: String,
) : ViewModel() {
    private val _config = MutableLiveData<ForwardingConfig>()
    val config: LiveData<ForwardingConfig> = _config

    private val _updateStatus = MutableLiveData<AppUpdateStatusView>()
    val updateStatus: LiveData<AppUpdateStatusView> = _updateStatus

    fun loadConfig() {
        _config.value = configGetUseCase.execute()
    }

    fun refreshUpdateStatus() {
        _updateStatus.value = try {
            resolveAppUpdateStatusView(currentVersion, updateStatusGetUseCase.execute())
        } catch (e: Exception) {
            AppUpdateStatusView.ReadFailed(currentVersion, e.message ?: e.javaClass.name)
        }
    }
}
