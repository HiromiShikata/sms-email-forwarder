package com.hiromi_shikata.smsemailforwarder.domain.repository

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus

interface AppUpdateStatusRepository {
    fun load(): AppUpdateStatus?
    fun save(status: AppUpdateStatus)
}
