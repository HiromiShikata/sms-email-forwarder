package com.hiromi_shikata.smsemailforwarder

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidAppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.data.remote.CacheApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.data.remote.GithubAppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.ApkDownloadUseCase
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase

internal fun checkUpdateAndNotify(
    useCase: AppUpdateCheckUseCase,
    downloadUseCase: ApkDownloadUseCase,
    notifier: AppUpdateNotifier,
    currentVersion: String,
): Result {
    return try {
        val update = useCase.execute(currentVersion)
        if (!update.isUpdateAvailable) {
            notifier.cancel()
            Result.success()
        } else {
            val apkFile = downloadUseCase.execute(update.downloadUrl)
            if (apkFile.exists()) {
                notifier.notifyDownloaded(update.latestVersion, update.releases, apkFile)
                Result.success()
            } else {
                Result.retry()
            }
        }
    } catch (e: Exception) {
        Result.retry()
    }
}

class AppUpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val useCase = AppUpdateCheckUseCase(GithubAppUpdateRepository(BuildConfig.GITHUB_REPO))
        val downloadUseCase = ApkDownloadUseCase(CacheApkDownloadRepository(applicationContext))
        val notifier = AndroidAppUpdateNotifier(applicationContext)
        return checkUpdateAndNotify(useCase, downloadUseCase, notifier, BuildConfig.VERSION_NAME)
    }
}
