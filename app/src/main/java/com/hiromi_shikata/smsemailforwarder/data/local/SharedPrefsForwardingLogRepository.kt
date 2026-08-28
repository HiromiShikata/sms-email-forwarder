package com.hiromi_shikata.smsemailforwarder.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntry
import com.hiromi_shikata.smsemailforwarder.domain.entity.ForwardingLogEntryStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ForwardingLogRepository
import java.util.Base64

internal fun encodeField(value: String): String =
    Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

internal fun decodeField(encoded: String): String =
    String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)

internal fun serializeEntry(entry: ForwardingLogEntry): String {
    val errorEncoded = if (entry.errorMessage != null) encodeField(entry.errorMessage) else ""
    return "${entry.timestamp}|${encodeField(entry.sender)}|${entry.status.name}|$errorEncoded"
}

internal fun deserializeEntry(line: String): ForwardingLogEntry? = runCatching {
    val parts = line.split("|", limit = 4)
    if (parts.size < 4) return null
    ForwardingLogEntry(
        timestamp = parts[0].toLong(),
        sender = decodeField(parts[1]),
        status = ForwardingLogEntryStatus.valueOf(parts[2]),
        errorMessage = if (parts[3].isNotEmpty()) decodeField(parts[3]) else null,
    )
}.getOrNull()

internal fun serializeEntries(entries: List<ForwardingLogEntry>): String =
    entries.joinToString("\n") { serializeEntry(it) }

internal fun deserializeEntries(data: String): List<ForwardingLogEntry> =
    if (data.isBlank()) emptyList()
    else data.lines().mapNotNull { deserializeEntry(it) }

internal fun enforceMaxEntries(
    entries: List<ForwardingLogEntry>,
    maxSize: Int = MAX_ENTRIES,
): List<ForwardingLogEntry> =
    if (entries.size > maxSize) entries.drop(entries.size - maxSize) else entries

private const val MAX_ENTRIES = 50

class SharedPrefsForwardingLogRepository(
    private val prefs: SharedPreferences,
) : ForwardingLogRepository {

    override fun save(entry: ForwardingLogEntry) {
        val updated = enforceMaxEntries(getAll() + entry)
        prefs.edit().putString(KEY_LOG, serializeEntries(updated)).apply()
    }

    override fun getAll(): List<ForwardingLogEntry> {
        val data = prefs.getString(KEY_LOG, null) ?: return emptyList()
        return deserializeEntries(data)
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
