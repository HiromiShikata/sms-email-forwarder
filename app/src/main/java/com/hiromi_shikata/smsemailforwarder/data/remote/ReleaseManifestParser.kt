package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ReleaseManifestParser(
    private val apkDownloadBaseUrl: String,
) {
    fun parse(manifestJson: String): List<AppRelease> {
        val entries = try {
            JSONArray(manifestJson)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Release manifest is not a JSON array: ${e.message}", e)
        }
        return (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val version = requireText(entry, "version", index)
            AppRelease(
                version = version,
                notes = requireText(entry, "notes", index, allowBlank = true),
                apkDownloadUrl = "$apkDownloadBaseUrl/download/v$version/sms-email-forwarder_universal.apk",
            )
        }
    }

    private fun requireText(entry: JSONObject, key: String, index: Int, allowBlank: Boolean = false): String {
        if (!entry.has(key) || entry.isNull(key)) {
            throw IllegalArgumentException("Release manifest entry $index has no \"$key\"")
        }
        val value = entry.get(key)
        if (value !is String) {
            throw IllegalArgumentException("Release manifest entry $index \"$key\" is not a string")
        }
        if (!allowBlank && value.isBlank()) {
            throw IllegalArgumentException("Release manifest entry $index has a blank \"$key\"")
        }
        return value
    }
}
