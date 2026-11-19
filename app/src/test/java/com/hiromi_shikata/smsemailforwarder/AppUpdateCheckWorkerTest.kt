package com.hiromi_shikata.smsemailforwarder

import androidx.work.ListenableWorker.Result
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ApkDownloadUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class AppUpdateCheckWorkerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val useCase: AppUpdateCheckUseCase = mock()
    private val downloadUseCase: ApkDownloadUseCase = mock()
    private val notifier: AppUpdateNotifier = mock()
    private val currentVersion = "0.1.345"
    private val downloadUrl = "https://example.com/app.apk"
    private val newerReleases = listOf(
        AppRelease(version = "0.1.350", changeLines = listOf("Add X")),
        AppRelease(version = "0.1.348", changeLines = listOf("Fix Y")),
    )

    @Test
    fun `checkUpdateAndNotify downloads the apk before notifying with the newer releases and the downloaded file`() {
        val update = AppUpdate(
            latestVersion = "0.1.350",
            downloadUrl = downloadUrl,
            isUpdateAvailable = true,
            releases = newerReleases,
        )
        val apkFile = tempFolder.newFile("update.apk")
        whenever(useCase.execute(currentVersion)).thenReturn(update)
        whenever(downloadUseCase.execute(downloadUrl)).thenReturn(apkFile)

        val result = checkUpdateAndNotify(useCase, downloadUseCase, notifier, currentVersion)

        val order = inOrder(downloadUseCase, notifier)
        order.verify(downloadUseCase).execute(downloadUrl)
        order.verify(notifier).notifyDownloaded("0.1.350", newerReleases, apkFile)
        assertEquals(Result.success(), result)
    }

    @Test
    fun `checkUpdateAndNotify does not notify and returns retry when the download throws`() {
        val update = AppUpdate(
            latestVersion = "0.1.350",
            downloadUrl = downloadUrl,
            isUpdateAvailable = true,
            releases = newerReleases,
        )
        whenever(useCase.execute(currentVersion)).thenReturn(update)
        whenever(downloadUseCase.execute(downloadUrl)).thenThrow(RuntimeException("Download failed"))

        val result = checkUpdateAndNotify(useCase, downloadUseCase, notifier, currentVersion)

        verify(notifier, never()).notifyDownloaded(any(), any(), any())
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `checkUpdateAndNotify does not notify and returns retry when the download leaves no apk file`() {
        val update = AppUpdate(
            latestVersion = "0.1.350",
            downloadUrl = downloadUrl,
            isUpdateAvailable = true,
            releases = newerReleases,
        )
        val missingApkFile = File(tempFolder.root, "update.apk")
        whenever(useCase.execute(currentVersion)).thenReturn(update)
        whenever(downloadUseCase.execute(downloadUrl)).thenReturn(missingApkFile)

        val result = checkUpdateAndNotify(useCase, downloadUseCase, notifier, currentVersion)

        verify(notifier, never()).notifyDownloaded(any(), any(), any())
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `checkUpdateAndNotify neither downloads nor notifies a download when no update is available`() {
        val update = AppUpdate(
            latestVersion = currentVersion,
            downloadUrl = downloadUrl,
            isUpdateAvailable = false,
        )
        whenever(useCase.execute(currentVersion)).thenReturn(update)

        val result = checkUpdateAndNotify(useCase, downloadUseCase, notifier, currentVersion)

        verify(downloadUseCase, never()).execute(any())
        verify(notifier, never()).notifyDownloaded(any(), any(), any())
        verify(notifier).cancel()
        assertEquals(Result.success(), result)
    }

    @Test
    fun `checkUpdateAndNotify returns retry and downloads nothing when the update check throws`() {
        whenever(useCase.execute(currentVersion)).thenThrow(RuntimeException("Network error"))

        val result = checkUpdateAndNotify(useCase, downloadUseCase, notifier, currentVersion)

        verify(downloadUseCase, never()).execute(any())
        verify(notifier, never()).notifyDownloaded(any(), any(), any())
        assertEquals(Result.retry(), result)
    }
}
