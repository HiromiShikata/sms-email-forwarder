package com.hiromi_shikata.smsemailforwarder.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingConfig
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ForwardingConfigGetUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val configGetUseCase: ForwardingConfigGetUseCase,
    private val updateCheckUseCase: AppUpdateCheckUseCase,
    private val currentVersion: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _config = MutableLiveData<ForwardingConfig>()
    val config: LiveData<ForwardingConfig> = _config

    private val _update = MutableLiveData<AppUpdate?>()
    val update: LiveData<AppUpdate?> = _update

    fun loadConfig() {
        _config.value = configGetUseCase.execute()
    }

    fun checkForUpdate() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val update = updateCheckUseCase.execute(currentVersion)
                _update.postValue(if (update.isUpdateAvailable) update else null)
            } catch (e: Exception) {
                _update.postValue(null)
            }
        }
    }
}
