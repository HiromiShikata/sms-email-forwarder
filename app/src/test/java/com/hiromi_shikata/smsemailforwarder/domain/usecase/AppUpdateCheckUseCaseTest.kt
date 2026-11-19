package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AppUpdateCheckUseCaseTest {
    private val repository: AppUpdateRepository = mock()
    private val useCase = AppUpdateCheckUseCase(repository)

    @Test
    fun `execute returns update info from repository`() {
        val currentVersion = "1.0.0"
        val expected = AppUpdate(
            latestVersion = "1.1.0",
            downloadUrl = "https://github.com/example/releases/download/v1.1.0/app_universal.apk",
            isUpdateAvailable = true,
        )
        whenever(repository.fetchLatest(currentVersion)).thenReturn(expected)

        val result = useCase.execute(currentVersion)

        assertEquals(expected, result)
    }

    @Test
    fun `execute keeps only the releases newer than the current version, newest first`() {
        val currentVersion = "0.1.345"
        val release0350 = AppRelease(version = "0.1.350", changeLines = listOf("New change"))
        val release0348 = AppRelease(version = "0.1.348", changeLines = listOf("Middle change"))
        whenever(repository.fetchLatest(currentVersion)).thenReturn(
            AppUpdate(
                latestVersion = "0.1.350",
                downloadUrl = "https://github.com/example/releases/download/v0.1.350/app_universal.apk",
                isUpdateAvailable = true,
                releases = listOf(
                    AppRelease(version = "0.1.340", changeLines = listOf("Old change")),
                    release0350,
                    release0348,
                ),
            ),
        )

        val result = useCase.execute(currentVersion)

        assertEquals(listOf(release0350, release0348), result.releases)
    }

    @Test
    fun `execute returns no-update info when already on latest`() {
        val currentVersion = "1.1.0"
        val expected = AppUpdate(
            latestVersion = "1.1.0",
            downloadUrl = "https://github.com/example/releases/download/v1.1.0/app_universal.apk",
            isUpdateAvailable = false,
        )
        whenever(repository.fetchLatest(currentVersion)).thenReturn(expected)

        val result = useCase.execute(currentVersion)

        assertEquals(expected, result)
    }
}
