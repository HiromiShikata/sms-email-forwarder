package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.Context
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository

class SharedPrefsAppUpdateStatusRepository(context: Context) : AppUpdateStatusRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): AppUpdateStatus? = prefs.getString(KEY_STATUS, null)?.let { AppUpdateStatusJson.decode(it) }

    override fun save(status: AppUpdateStatus) {
        prefs.edit().putString(KEY_STATUS, AppUpdateStatusJson.encode(status)).apply()
    }

    companion object {
        const val PREFS_NAME = "app_update_status"
        const val KEY_STATUS = "status"
    }
}
