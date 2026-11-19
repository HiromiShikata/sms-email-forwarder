package com.hiromi_shikata.smsemailforwarder.data.remote

import org.json.JSONArray

data class GithubReleaseDto(
    val tagName: String,
    val body: String,
    val universalApkUrl: String?,
    val draft: Boolean,
    val prerelease: Boolean,
)

object GithubReleasesJsonParser {
    fun parse(json: String): List<GithubReleaseDto> {
        val releases = JSONArray(json)
        return (0 until releases.length()).map { index ->
            val release = releases.getJSONObject(index)
            val assets = release.getJSONArray("assets")
            GithubReleaseDto(
                tagName = release.getString("tag_name"),
                body = if (release.isNull("body")) "" else release.getString("body"),
                universalApkUrl = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith("_universal.apk") }
                    ?.getString("browser_download_url"),
                draft = release.getBoolean("draft"),
                prerelease = release.getBoolean("prerelease"),
            )
        }
    }

    fun changeLines(body: String): List<String> =
        body.lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").trim() }
            .filter { it.isNotEmpty() }
}
