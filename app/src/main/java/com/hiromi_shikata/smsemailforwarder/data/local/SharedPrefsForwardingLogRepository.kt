package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository
import org.json.JSONArray
import org.json.JSONObject

internal fun serializeEntry(entry: ForwardingLogEntry): JSONObject = JSONObject().apply {
    put(KEY_TIMESTAMP, entry.timestamp)
    put(KEY_SENDER, entry.sender)
    put(KEY_STATUS, entry.status.name)
    if (entry.errorMessage != null) put(KEY_ERROR_MESSAGE, entry.errorMessage)
}

internal fun deserializeEntry(obj: JSONObject): ForwardingLogEntry? = runCatching {
    ForwardingLogEntry(
        timestamp = obj.getLong(KEY_TIMESTAMP),
        sender = obj.getString(KEY_SENDER),
        status = ForwardingLogEntryStatus.valueOf(obj.getString(KEY_STATUS)),
        errorMessage = if (obj.has(KEY_ERROR_MESSAGE)) obj.getString(KEY_ERROR_MESSAGE) else null,
    )
}.getOrNull()

internal fun serializeEntries(entries: List<ForwardingLogEntry>): String =
    JSONArray().also { array ->
        entries.forEach { array.put(serializeEntry(it)) }
    }.toString()

internal fun deserializeEntries(json: String): List<ForwardingLogEntry> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).mapNotNull { deserializeEntry(array.getJSONObject(it)) }
}.getOrDefault(emptyList())

internal fun enforceMaxEntries(
    entries: List<ForwardingLogEntry>,
    maxSize: Int = MAX_ENTRIES,
): List<ForwardingLogEntry> =
    if (entries.size > maxSize) entries.drop(entries.size - maxSize) else entries

private const val KEY_TIMESTAMP = "timestamp"
private const val KEY_SENDER = "sender"
private const val KEY_STATUS = "status"
private const val KEY_ERROR_MESSAGE = "errorMessage"
private const val MAX_ENTRIES = 50

class SharedPrefsForwardingLogRepository(
    private val prefs: SharedPreferences,
) : ForwardingLogRepository {

    override fun save(entry: ForwardingLogEntry) {
        val updated = enforceMaxEntries(getAll() + entry)
        prefs.edit().putString(KEY_LOG, serializeEntries(updated)).apply()
    }

    override fun getAll(): List<ForwardingLogEntry> {
        val json = prefs.getString(KEY_LOG, null) ?: return emptyList()
        return deserializeEntries(json)
    }

    companion object {
        private const val PREFS_NAME = "forwarding_log"
        private const val KEY_LOG = "log_entries"

        fun create(context: Context): SharedPrefsForwardingLogRepository =
            SharedPrefsForwardingLogRepository(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
            )
    }
}
