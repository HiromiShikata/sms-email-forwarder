package com.hiromi_shikata.smsemailforwarder.data.remote

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import org.junit.Assert.assertEquals
import org.junit.Test

class GithubAppUpdateMapperTest {

    private data class Case(
        val name: String,
        val dtos: List<GithubReleaseDto>,
        val currentVersion: String,
        val expected: AppUpdate,
    )

    private fun dto(
        tag: String,
        body: String = "",
        apkUrl: String? = null,
        draft: Boolean = false,
        prerelease: Boolean = false,
    ): GithubReleaseDto = GithubReleaseDto(
        tagName = tag,
        body = body,
        universalApkUrl = apkUrl,
        draft = draft,
        prerelease = prerelease,
    )

    private val cases = listOf(
        Case(
            name = "a newer release without an APK keeps its change lines and the download is the next newest APK",
            dtos = listOf(
                dto("v0.1.11", body = "- Eleven\n- Also eleven"),
                dto("v0.1.10", body = "- Ten", apkUrl = "https://example.test/0.1.10.apk"),
                dto("v0.1.9", body = "- Nine", apkUrl = "https://example.test/0.1.9.apk"),
            ),
            currentVersion = "0.1.8",
            expected = AppUpdate(
                latestVersion = "0.1.10",
                downloadUrl = "https://example.test/0.1.10.apk",
                isUpdateAvailable = true,
                releases = listOf(
                    AppRelease(version = "0.1.11", changeLines = listOf("Eleven", "Also eleven")),
                    AppRelease(version = "0.1.10", changeLines = listOf("Ten")),
                    AppRelease(version = "0.1.9", changeLines = listOf("Nine")),
                ),
            ),
        ),
        Case(
            name = "no valid release has an APK so nothing is downloadable and the current version is kept",
            dtos = listOf(
                dto("v0.1.10", body = "- Ten"),
            ),
            currentVersion = "0.1.9",
            expected = AppUpdate(
                latestVersion = "0.1.9",
                downloadUrl = "",
                isUpdateAvailable = false,
                releases = listOf(AppRelease(version = "0.1.10", changeLines = listOf("Ten"))),
            ),
        ),
        Case(
            name = "empty input gives an update with no releases",
            dtos = emptyList(),
            currentVersion = "0.1.9",
            expected = AppUpdate(
                latestVersion = "0.1.9",
                downloadUrl = "",
                isUpdateAvailable = false,
                releases = emptyList(),
            ),
        ),
        Case(
            name = "draft, prerelease and non-version tags are ignored entirely",
            dtos = listOf(
                dto("v0.1.12", body = "- Draft", apkUrl = "https://example.test/0.1.12.apk", draft = true),
                dto("v0.1.11", body = "- Pre", apkUrl = "https://example.test/0.1.11.apk", prerelease = true),
                dto("nightly", body = "- Nightly", apkUrl = "https://example.test/nightly.apk"),
                dto("v0.1.10", body = "- Ten", apkUrl = "https://example.test/0.1.10.apk"),
            ),
            currentVersion = "0.1.9",
            expected = AppUpdate(
                latestVersion = "0.1.10",
                downloadUrl = "https://example.test/0.1.10.apk",
                isUpdateAvailable = true,
                releases = listOf(AppRelease(version = "0.1.10", changeLines = listOf("Ten"))),
            ),
        ),
        Case(
            name = "releases are ordered by numeric version, newest first",
            dtos = listOf(
                dto("v0.1.9", body = "- Nine", apkUrl = "https://example.test/0.1.9.apk"),
                dto("0.1.10", body = "- Ten", apkUrl = "https://example.test/0.1.10.apk"),
            ),
            currentVersion = "0.1.9",
            expected = AppUpdate(
                latestVersion = "0.1.10",
                downloadUrl = "https://example.test/0.1.10.apk",
                isUpdateAvailable = true,
                releases = listOf(
                    AppRelease(version = "0.1.10", changeLines = listOf("Ten")),
                    AppRelease(version = "0.1.9", changeLines = listOf("Nine")),
                ),
            ),
        ),
        Case(
            name = "isUpdateAvailable is false when the newest APK is the installed version",
            dtos = listOf(
                dto("v0.1.9", body = "- Nine", apkUrl = "https://example.test/0.1.9.apk"),
            ),
            currentVersion = "0.1.9",
            expected = AppUpdate(
                latestVersion = "0.1.9",
                downloadUrl = "https://example.test/0.1.9.apk",
                isUpdateAvailable = false,
                releases = listOf(AppRelease(version = "0.1.9", changeLines = listOf("Nine"))),
            ),
        ),
    )

    @Test
    fun `toAppUpdate maps every valid release and picks the newest release that has an APK`() {
        val versionCompare = AppVersionCompare()
        val actual = cases.map { case ->
            case.name to GithubAppUpdateMapper.toAppUpdate(
                dtos = case.dtos,
                currentVersion = case.currentVersion,
                versionCompare = versionCompare,
            )
        }
        assertEquals(cases.map { it.name to it.expected }, actual)
    }
}
