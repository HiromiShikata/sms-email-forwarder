package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.entity.CheckFailure
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.selectReleasesNewerThan
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AppUpdateCheckUseCase(
    private val releaseRepository: AppUpdateRepository,
    private val apkDownloadRepository: ApkDownloadRepository,
    private val notifier: AppUpdateNotifier,
    private val statusRepository: AppUpdateStatusRepository,
    private val installedVersion: String,
    private val nowEpochMillis: () -> Long,
    private val versionCompare: AppVersionCompare = AppVersionCompare(),
) {
    fun execute(): AppUpdateStatus = checkLock.withLock {
        val checkedAt = nowEpochMillis()
        val previous = statusRepository.load()
        try {
            runCheck(previous, checkedAt)
        } catch (e: Exception) {
            val failure = CheckFailure(e.message ?: e.javaClass.name, nowEpochMillis())
            persist(failureStatus(previous, failure))
        }
    }

    private fun failureStatus(previous: AppUpdateStatus?, failure: CheckFailure): AppUpdateStatus {
        if (previous is AppUpdateStatus.ReadyToInstall && File(previous.apkFilePath).exists()) {
            return previous.copy(lastCheckFailure = failure)
        }
        return AppUpdateStatus.CheckFailed(installedVersion, failure.message, failure.failedAtEpochMillis)
    }

    private fun runCheck(previous: AppUpdateStatus?, checkedAt: Long): AppUpdateStatus {
        val newer = selectReleasesNewerThan(
            releaseRepository.fetchReleases(),
            installedVersion,
            versionCompare,
        )
        if (newer.isEmpty()) {
            notifier.cancel()
            return persist(AppUpdateStatus.UpToDate(installedVersion, checkedAt))
        }
        val latest = newer.first()
        if (previous is AppUpdateStatus.ReadyToInstall &&
            previous.releases.first().version == latest.version &&
            File(previous.apkFilePath).exists()
        ) {
            val ready = persist(previous.copy(releases = newer, checkedAtEpochMillis = checkedAt, lastCheckFailure = null))
            notifier.notifyReadyToInstall(newer)
            return ready
        }
        persist(AppUpdateStatus.Available(installedVersion, newer, checkedAt))
        persist(AppUpdateStatus.Downloading(installedVersion, newer, checkedAt))
        val apk = apkDownloadRepository.download(latest.apkDownloadUrl)
        val ready = persist(AppUpdateStatus.ReadyToInstall(installedVersion, newer, apk.absolutePath, checkedAt))
        notifier.notifyReadyToInstall(newer)
        return ready
    }

    private fun persist(status: AppUpdateStatus): AppUpdateStatus {
        statusRepository.save(status)
        return status
    }

    companion object {
        private val checkLock = ReentrantLock()
    }
}
