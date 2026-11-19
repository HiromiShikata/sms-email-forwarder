package com.hiromi_shikata.smsemailforwarder.presentation

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.entity.CheckFailure

sealed interface AppUpdateStatusView {
    val installedVersion: String

    data class NotChecked(override val installedVersion: String) : AppUpdateStatusView

    data class UpToDate(
        override val installedVersion: String,
        val checkedAtEpochMillis: Long,
    ) : AppUpdateStatusView

    data class Available(
        override val installedVersion: String,
        val latestVersion: String,
    ) : AppUpdateStatusView

    data class Downloading(
        override val installedVersion: String,
        val latestVersion: String,
    ) : AppUpdateStatusView

    data class ReadyToInstall(
        override val installedVersion: String,
        val latestVersion: String,
        val lastCheckFailure: CheckFailure? = null,
    ) : AppUpdateStatusView

    data class CheckFailed(
        override val installedVersion: String,
        val message: String,
        val failedAtEpochMillis: Long,
    ) : AppUpdateStatusView

    data class ReadFailed(
        override val installedVersion: String,
        val message: String,
    ) : AppUpdateStatusView
}

fun resolveAppUpdateStatusView(installedVersion: String, status: AppUpdateStatus?): AppUpdateStatusView = when (status) {
    null -> AppUpdateStatusView.NotChecked(installedVersion)
    is AppUpdateStatus.UpToDate -> AppUpdateStatusView.UpToDate(installedVersion, status.checkedAtEpochMillis)
    is AppUpdateStatus.Available -> AppUpdateStatusView.Available(installedVersion, status.releases.first().version)
    is AppUpdateStatus.Downloading -> AppUpdateStatusView.Downloading(installedVersion, status.releases.first().version)
    is AppUpdateStatus.ReadyToInstall -> AppUpdateStatusView.ReadyToInstall(installedVersion, status.releases.first().version, status.lastCheckFailure)
    is AppUpdateStatus.CheckFailed -> AppUpdateStatusView.CheckFailed(installedVersion, status.message, status.checkedAtEpochMillis)
}
