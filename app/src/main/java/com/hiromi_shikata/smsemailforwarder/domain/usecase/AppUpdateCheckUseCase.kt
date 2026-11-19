package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.AppVersionCompare
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.selectReleasesNewerThan
import java.io.File

class AppUpdateCheckUseCase(
    private val releaseRepository: AppUpdateRepository,
    private val apkDownloadRepository: ApkDownloadRepository,
    private val notifier: AppUpdateNotifier,
    private val statusRepository: AppUpdateStatusRepository,
    private val installedVersion: String,
    private val nowEpochMillis: () -> Long,
    private val versionCompare: AppVersionCompare = AppVersionCompare(),
) {
    fun execute(): AppUpdateStatus {
        val checkedAt = nowEpochMillis()
        return try {
            runCheck(checkedAt)
        } catch (e: Exception) {
            persist(
                AppUpdateStatus.CheckFailed(
                    installedVersion = installedVersion,
                    message = e.message ?: e.javaClass.name,
                    checkedAtEpochMillis = nowEpochMillis(),
                ),
            )
        }
    }

    private fun persist(status: AppUpdateStatus): AppUpdateStatus {
        statusRepository.save(status)
        return status
    }

    private fun runCheck(checkedAt: Long): AppUpdateStatus {
        val newer = selectReleasesNewerThan(
            releaseRepository.fetchReleasesNewerThan(installedVersion),
            installedVersion,
            versionCompare,
        )
        if (newer.isEmpty()) {
            notifier.cancel()
            return persist(AppUpdateStatus.UpToDate(installedVersion, checkedAt))
        }
        val latest = newer.first()
        val previous = statusRepository.load()
        if (previous is AppUpdateStatus.ReadyToInstall &&
            previous.releases.first().version == latest.version &&
            File(previous.apkFilePath).exists()
        ) {
            return persist(previous.copy(releases = newer, checkedAtEpochMillis = checkedAt))
        }
        statusRepository.save(AppUpdateStatus.Available(installedVersion, newer, checkedAt))
        statusRepository.save(AppUpdateStatus.Downloading(installedVersion, newer, checkedAt))
        val apk = apkDownloadRepository.download(latest.apkDownloadUrl)
        val ready = persist(AppUpdateStatus.ReadyToInstall(installedVersion, newer, apk.absolutePath, checkedAt))
        notifier.notifyReadyToInstall(newer)
        return ready
    }
}
