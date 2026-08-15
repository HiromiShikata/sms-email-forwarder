package com.hiromi_shikata.smsemailforwarder.domain.usecase

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
