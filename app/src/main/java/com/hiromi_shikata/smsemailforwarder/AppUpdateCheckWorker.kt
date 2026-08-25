package com.hiromi_shikata.smsemailforwarder

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidAppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.data.remote.GithubAppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.usecase.AppUpdateCheckUseCase

internal fun checkUpdateAndNotify(
    useCase: AppUpdateCheckUseCase,
    notifier: AppUpdateNotifier,
    currentVersion: String,
): Result {
    return try {
        val update = useCase.execute(currentVersion)
        if (update.isUpdateAvailable) {
            notifier.notify(update)
        } else {
            notifier.cancel()
        }
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }
}

class AppUpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val useCase = AppUpdateCheckUseCase(GithubAppUpdateRepository(BuildConfig.GITHUB_REPO))
        val notifier = AndroidAppUpdateNotifier(applicationContext)
        return checkUpdateAndNotify(useCase, notifier, BuildConfig.VERSION_NAME)
    }
}
