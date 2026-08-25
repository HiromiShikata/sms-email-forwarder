package com.hiromi_shikata.smsemailforwarder

import androidx.work.ListenableWorker.Result
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AppUpdateCheckWorkerTest {
    private val useCase: AppUpdateCheckUseCase = mock()
    private val notifier: AppUpdateNotifier = mock()
    private val currentVersion = "1.0.0"

    @Test
    fun `checkUpdateAndNotify calls notify when update is available`() {
        val update = AppUpdate(
            latestVersion = "1.1.0",
            downloadUrl = "https://example.com/app.apk",
            isUpdateAvailable = true,
        )
        whenever(useCase.execute(currentVersion)).thenReturn(update)

        val result = checkUpdateAndNotify(useCase, notifier, currentVersion)

        verify(notifier).notify(update)
        verify(notifier, never()).cancel()
        assertEquals(Result.success(), result)
    }

    @Test
    fun `checkUpdateAndNotify calls cancel when no update is available`() {
        val update = AppUpdate(
            latestVersion = "1.0.0",
            downloadUrl = "https://example.com/app.apk",
            isUpdateAvailable = false,
        )
        whenever(useCase.execute(currentVersion)).thenReturn(update)

        val result = checkUpdateAndNotify(useCase, notifier, currentVersion)

        verify(notifier).cancel()
        verify(notifier, never()).notify(update)
        assertEquals(Result.success(), result)
    }

    @Test
    fun `checkUpdateAndNotify returns retry when repository throws`() {
        whenever(useCase.execute(currentVersion)).thenThrow(RuntimeException("Network error"))

        val result = checkUpdateAndNotify(useCase, notifier, currentVersion)

        assertEquals(Result.retry(), result)
    }
}
