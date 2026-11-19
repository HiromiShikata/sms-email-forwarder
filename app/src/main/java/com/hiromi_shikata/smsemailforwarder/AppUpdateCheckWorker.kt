package com.hiromi_shikata.smsemailforwarder

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidAppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsAppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.CacheApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.ReleaseManifestAppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase
import com.hiromi_shikata.smsemailforwarder.presentation.AppUpdateInstallActivity
import android.content.Intent
import java.util.concurrent.TimeUnit

const val APP_UPDATE_PERIODIC_WORK_NAME = "app_update_check"
const val APP_UPDATE_IMMEDIATE_WORK_NAME = "app_update_check_now"

class AppUpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
    private val useCaseFactory: (Context) -> AppUpdateCheckUseCase,
) : Worker(context, params) {
    constructor(context: Context, params: WorkerParameters) : this(context, params, ::createAppUpdateCheckUseCase)

    override fun doWork(): Result {
        val status = useCaseFactory(applicationContext).execute()
        return if (status is AppUpdateStatus.CheckFailed) Result.failure() else Result.success()
    }
}

fun createAppUpdateCheckUseCase(context: Context): AppUpdateCheckUseCase = AppUpdateCheckUseCase(
    releaseRepository = ReleaseManifestAppUpdateRepository(BuildConfig.UPDATE_RELEASES_BASE_URL),
    apkDownloadRepository = CacheApkDownloadRepository(context),
    notifier = AndroidAppUpdateNotifier(
        context,
        Intent(context, AppUpdateInstallActivity::class.java),
    ),
    statusRepository = SharedPrefsAppUpdateStatusRepository(context),
    installedVersion = BuildConfig.VERSION_NAME,
    nowEpochMillis = { System.currentTimeMillis() },
)

private fun networkConnectedConstraints(): Constraints =
    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

fun schedulePeriodicAppUpdateCheck(context: Context) {
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        APP_UPDATE_PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkConnectedConstraints())
            .build(),
    )
}

fun enqueueImmediateAppUpdateCheck(context: Context) {
    WorkManager.getInstance(context).enqueueUniqueWork(
        APP_UPDATE_IMMEDIATE_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        OneTimeWorkRequestBuilder<AppUpdateCheckWorker>()
            .setConstraints(networkConnectedConstraints())
            .build(),
    )
}
