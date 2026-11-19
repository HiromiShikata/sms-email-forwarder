package com.hiromi_shikata.smsemailforwarder.data.local

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.CheckFailure
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import org.json.JSONArray
import org.json.JSONObject

object AppUpdateStatusJson {
    fun encode(status: AppUpdateStatus): String {
        val json = JSONObject()
            .put("installedVersion", status.installedVersion)
            .put("checkedAtEpochMillis", status.checkedAtEpochMillis)
        when (status) {
            is AppUpdateStatus.UpToDate -> json.put("type", "UP_TO_DATE")
            is AppUpdateStatus.Available -> json.put("type", "AVAILABLE").put("releases", releasesToJson(status.releases))
            is AppUpdateStatus.Downloading -> json.put("type", "DOWNLOADING").put("releases", releasesToJson(status.releases))
            is AppUpdateStatus.ReadyToInstall -> json.put("type", "READY_TO_INSTALL")
                .put("releases", releasesToJson(status.releases))
                .put("apkFilePath", status.apkFilePath)
                .putOpt("lastCheckFailure", status.lastCheckFailure?.let(::failureToJson))
            is AppUpdateStatus.CheckFailed -> json.put("type", "CHECK_FAILED").put("message", status.message)
        }
        return json.toString()
    }

    fun decode(text: String): AppUpdateStatus {
        val json = JSONObject(text)
        val installed = json.getString("installedVersion")
        val checkedAt = json.getLong("checkedAtEpochMillis")
        return when (val type = json.getString("type")) {
            "UP_TO_DATE" -> AppUpdateStatus.UpToDate(installed, checkedAt)
            "AVAILABLE" -> AppUpdateStatus.Available(installed, releasesFromJson(json.getJSONArray("releases")), checkedAt)
            "DOWNLOADING" -> AppUpdateStatus.Downloading(installed, releasesFromJson(json.getJSONArray("releases")), checkedAt)
            "READY_TO_INSTALL" -> AppUpdateStatus.ReadyToInstall(
                installed,
                releasesFromJson(json.getJSONArray("releases")),
                json.getString("apkFilePath"),
                checkedAt,
                if (json.has("lastCheckFailure")) failureFromJson(json.getJSONObject("lastCheckFailure")) else null,
            )
            "CHECK_FAILED" -> AppUpdateStatus.CheckFailed(installed, json.getString("message"), checkedAt)
            else -> throw IllegalArgumentException("Unknown update status type: $type")
        }
    }

    private fun failureToJson(failure: CheckFailure): JSONObject = JSONObject()
        .put("message", failure.message)
        .put("failedAtEpochMillis", failure.failedAtEpochMillis)

    private fun failureFromJson(json: JSONObject): CheckFailure =
        CheckFailure(json.getString("message"), json.getLong("failedAtEpochMillis"))

    private fun releasesToJson(releases: List<AppRelease>): JSONArray {
        val array = JSONArray()
        releases.forEach { release ->
            array.put(
                JSONObject()
                    .put("version", release.version)
                    .put("notes", release.notes)
                    .put("apkDownloadUrl", release.apkDownloadUrl),
            )
        }
        return array
    }

    private fun releasesFromJson(array: JSONArray): List<AppRelease> = (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        AppRelease(
            version = item.getString("version"),
            notes = item.getString("notes"),
            apkDownloadUrl = item.getString("apkDownloadUrl"),
        )
    }
}
