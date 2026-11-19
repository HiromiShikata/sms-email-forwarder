package com.hiromi_shikata.smsemailforwarder

import android.app.Application
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.testing.TestListenableWorkerBuilder
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppUpdateCheckWorkerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class MemoryStatus : AppUpdateStatusRepository {
        var stored: AppUpdateStatus? = null
        override fun load(): AppUpdateStatus? = stored
        override fun save(status: AppUpdateStatus) {
            stored = status
        }
    }

    private object NoopNotifier : AppUpdateNotifier {
        override fun notifyReadyToInstall(releases: List<AppRelease>) = Unit
        override fun cancel() = Unit
    }

    private fun useCase(releases: () -> List<AppRelease>, store: MemoryStatus) = AppUpdateCheckUseCase(
        releaseRepository = object : AppUpdateRepository {
            override fun fetchReleases(): List<AppRelease> = releases()
        },
        apkDownloadRepository = object : ApkDownloadRepository {
            override fun download(url: String): File = File.createTempFile("update", ".apk")
        },
        notifier = NoopNotifier,
        statusRepository = store,
        installedVersion = "0.1.344",
        nowEpochMillis = { 1L },
    )

    private fun runWorker(useCase: AppUpdateCheckUseCase): ListenableWorker.Result {
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: android.content.Context,
                workerClassName: String,
                workerParameters: androidx.work.WorkerParameters,
            ): ListenableWorker? =
                if (workerClassName == AppUpdateCheckWorker::class.java.name) {
                    AppUpdateCheckWorker(appContext, workerParameters) { useCase }
                } else {
                    null
                }
        }
        val worker = TestListenableWorkerBuilder<AppUpdateCheckWorker>(app)
            .setWorkerFactory(factory)
            .build()
        return worker.startWork().get()
    }

    @Test
    fun `a successful check returns success and persists the resulting status`() {
        val store = MemoryStatus()
        val result = runWorker(useCase(releases = { emptyList() }, store = store))
        assertTrue(result is Result.Success)
        assertEquals(AppUpdateStatus.UpToDate("0.1.344", 1L), store.stored)
    }

    @Test
    fun `a failed check returns failure and persists the failure status with its message`() {
        val store = MemoryStatus()
        val result = runWorker(useCase(releases = { throw IOException("HTTP 503 while reading manifest") }, store = store))
        assertTrue(result is Result.Failure)
        assertEquals(
            AppUpdateStatus.CheckFailed("0.1.344", "HTTP 503 while reading manifest", 1L),
            store.stored,
        )
    }
}
