package com.hiromi_shikata.smsemailforwarder.domain.entity

data class CheckFailure(
    val message: String,
    val failedAtEpochMillis: Long,
)

sealed interface AppUpdateStatus {
    val installedVersion: String
    val checkedAtEpochMillis: Long

    data class UpToDate(
        override val installedVersion: String,
        override val checkedAtEpochMillis: Long,
    ) : AppUpdateStatus

    data class Available(
        override val installedVersion: String,
        val releases: List<AppRelease>,
        override val checkedAtEpochMillis: Long,
    ) : AppUpdateStatus

    data class Downloading(
        override val installedVersion: String,
        val releases: List<AppRelease>,
        override val checkedAtEpochMillis: Long,
    ) : AppUpdateStatus

    data class ReadyToInstall(
        override val installedVersion: String,
        val releases: List<AppRelease>,
        val apkFilePath: String,
        override val checkedAtEpochMillis: Long,
        val lastCheckFailure: CheckFailure? = null,
    ) : AppUpdateStatus

    data class CheckFailed(
        override val installedVersion: String,
        val message: String,
        override val checkedAtEpochMillis: Long,
    ) : AppUpdateStatus
}
