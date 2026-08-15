package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import org.json.JSONObject
import java.net.URL

class GithubAppUpdateRepository(
    private val githubRepo: String,
    private val versionCompare: AppVersionCompare = AppVersionCompare(),
) : AppUpdateRepository {
    override fun fetchLatest(currentVersion: String): AppUpdate {
        val url = "https://api.github.com/repos/$githubRepo/releases/latest"
        val connection = URL(url).openConnection()
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        val response = connection.getInputStream().bufferedReader().readText()
        val json = JSONObject(response)
        val latestTag = json.getString("tag_name").removePrefix("v")
        val assets = json.getJSONArray("assets")
        val downloadUrl = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith("_universal.apk") }
            ?.getString("browser_download_url") ?: ""
        return AppUpdate(
            latestVersion = latestTag,
            downloadUrl = downloadUrl,
            isUpdateAvailable = versionCompare.isNewer(latestTag, currentVersion),
        )
    }
}
