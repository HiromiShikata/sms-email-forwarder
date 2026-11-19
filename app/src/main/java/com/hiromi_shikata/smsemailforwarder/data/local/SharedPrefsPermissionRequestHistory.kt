package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.Context

class SharedPrefsPermissionRequestHistory(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY_REQUESTED, emptySet())!!.toSet()

    fun add(permissions: List<String>) {
        prefs.edit().putStringSet(KEY_REQUESTED, load() + permissions).apply()
    }

    companion object {
        const val PREFS_NAME = "permission_requests"
        const val KEY_REQUESTED = "requested"
    }
}
