package com.hiromi_shikata.smsemailforwarder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class GithubReleasesJsonParserTest {

    private val releasesJson = """
        [
          {
            "tag_name": "v0.1.350",
            "body": "- Add X\n- Fix Y",
            "draft": false,
            "prerelease": false,
            "assets": [
              {"name": "sms-email-forwarder_arm64-v8a.apk", "browser_download_url": "https://example.com/arm64.apk"},
              {"name": "sms-email-forwarder_universal.apk", "browser_download_url": "https://example.com/universal.apk"}
            ]
          },
          {
            "tag_name": "v0.1.348",
            "body": null,
            "draft": true,
            "prerelease": true,
            "assets": [
              {"name": "sms-email-forwarder_arm64-v8a.apk", "browser_download_url": "https://example.com/arm64-old.apk"}
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun `parse maps tag, body, universal apk url and draft and prerelease flags of each release`() {
        val actual = GithubReleasesJsonParser.parse(releasesJson)

        assertEquals(
            listOf(
                GithubReleaseDto(
                    tagName = "v0.1.350",
                    body = "- Add X\n- Fix Y",
                    universalApkUrl = "https://example.com/universal.apk",
                    draft = false,
                    prerelease = false,
                ),
                GithubReleaseDto(
                    tagName = "v0.1.348",
                    body = "",
                    universalApkUrl = null,
                    draft = true,
                    prerelease = true,
                ),
            ),
            actual,
        )
    }

    @Test
    fun `parse returns an empty list for an empty releases array`() {
        assertEquals(emptyList<GithubReleaseDto>(), GithubReleasesJsonParser.parse("[]"))
    }

    @Test
    fun `changeLines returns the trimmed text after each dash bullet and ignores other lines`() {
        val cases = listOf(
            "" to emptyList(),
            "Only prose" to emptyList(),
            "- Add X\n- Fix Y" to listOf("Add X", "Fix Y"),
            "## Changes\n- Add X\nplain line\n  - Indented fix  \n-\n- " to listOf("Add X", "Indented fix"),
            "- Add X\r\n- Fix Y\r\n" to listOf("Add X", "Fix Y"),
            "-Not a bullet\n- Real bullet" to listOf("Real bullet"),
        )

        val actual = cases.map { (body, _) -> GithubReleasesJsonParser.changeLines(body) }

        assertEquals(cases.map { (_, expected) -> expected }, actual)
    }
}
