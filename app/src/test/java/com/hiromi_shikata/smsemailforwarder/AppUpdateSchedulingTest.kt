package com.hiromi_shikata.smsemailforwarder

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.model.WorkSpec
import androidx.work.testing.WorkManagerTestInitHelper
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppUpdateSchedulingTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private class MemoryStatus : AppUpdateStatusRepository {
        var stored: AppUpdateStatus? = null
        override fun load(): AppUpdateStatus? = stored
        override fun save(status: AppUpdateStatus) {
            stored = status
        }
    }

    private fun offlineUseCase() = AppUpdateCheckUseCase(
        releaseRepository = object : AppUpdateRepository {
            override fun fetchReleases(): List<AppRelease> = emptyList()
        },
        apkDownloadRepository = object : ApkDownloadRepository {
            override fun download(url: String): File = error("no download in scheduling tests")
        },
        notifier = object : AppUpdateNotifier {
            override fun notifyReadyToInstall(releases: List<AppRelease>) = Unit
            override fun cancel() = Unit
        },
        statusRepository = MemoryStatus(),
        installedVersion = "0.1.344",
        nowEpochMillis = { 1L },
    )

    @Before
    fun initWorkManager() {
        val useCase = offlineUseCase()
        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? =
                if (workerClassName == AppUpdateCheckWorker::class.java.name) {
                    AppUpdateCheckWorker(appContext, workerParameters) { useCase }
                } else {
                    null
                }
        }
        WorkManagerTestInitHelper.initializeTestWorkManager(
            app,
            Configuration.Builder().setWorkerFactory(factory).build(),
        )
    }

    private fun workInfos(name: String): List<WorkInfo> =
        WorkManager.getInstance(app).getWorkInfosForUniqueWork(name).get()

    private fun specOf(info: WorkInfo): WorkSpec =
        (WorkManager.getInstance(app) as WorkManagerImpl).workDatabase.workSpecDao().getWorkSpec(info.id.toString())!!

    @Test
    fun `the periodic check uses the legacy name and replaces an older keep-policy work with network-constrained hourly work`() {
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            APP_UPDATE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(1, TimeUnit.HOURS).build(),
        ).result.get()

        schedulePeriodicAppUpdateCheck(app)

        val infos = workInfos(APP_UPDATE_PERIODIC_WORK_NAME)
        assertEquals(1, infos.size)
        val spec = specOf(infos.single())
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(TimeUnit.HOURS.toMillis(1), spec.intervalDuration)
        assertTrue(infos.single().state == WorkInfo.State.ENQUEUED || infos.single().state == WorkInfo.State.RUNNING)
    }

    @Test
    fun `scheduling the periodic check twice keeps exactly one work`() {
        schedulePeriodicAppUpdateCheck(app)
        schedulePeriodicAppUpdateCheck(app)
        assertEquals(1, workInfos(APP_UPDATE_PERIODIC_WORK_NAME).size)
    }

    @Test
    fun `the resume check is a single network-constrained one-time work that does not duplicate`() {
        enqueueImmediateAppUpdateCheck(app)
        enqueueImmediateAppUpdateCheck(app)
        val infos = workInfos(APP_UPDATE_IMMEDIATE_WORK_NAME)
        assertEquals(1, infos.size)
        assertEquals(NetworkType.CONNECTED, specOf(infos.single()).constraints.requiredNetworkType)
        assertEquals(0L, specOf(infos.single()).intervalDuration)
    }
}
