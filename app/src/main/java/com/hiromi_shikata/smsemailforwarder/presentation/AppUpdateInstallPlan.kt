package com.hiromi_shikata.smsemailforwarder.presentation

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus

sealed interface AppUpdateInstallPlan {
    data object NothingToInstall : AppUpdateInstallPlan
    data class ApkMissing(val apkFilePath: String) : AppUpdateInstallPlan
    data object GrantUnknownSourcesPermission : AppUpdateInstallPlan
    data class InstallApk(val apkFilePath: String) : AppUpdateInstallPlan
}

fun planAppUpdateInstall(
    status: AppUpdateStatus?,
    canRequestPackageInstalls: Boolean,
    apkExists: (String) -> Boolean,
): AppUpdateInstallPlan {
    if (status !is AppUpdateStatus.ReadyToInstall) return AppUpdateInstallPlan.NothingToInstall
    if (!apkExists(status.apkFilePath)) return AppUpdateInstallPlan.ApkMissing(status.apkFilePath)
    if (!canRequestPackageInstalls) return AppUpdateInstallPlan.GrantUnknownSourcesPermission
    return AppUpdateInstallPlan.InstallApk(status.apkFilePath)
}
