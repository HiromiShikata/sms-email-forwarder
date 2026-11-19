package com.hiromi_shikata.smsemailforwarder.presentation

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateInstallPlanTest {
    private val ready = AppUpdateStatus.ReadyToInstall(
        "0.1.344",
        listOf(AppRelease("0.1.345", "n", "u")),
        "/cache/update.apk",
        1L,
    )

    @Test
    fun `the plan follows the stored state, the permission and the apk file`() {
        val cases = listOf(
            "no status" to Triple(null, true, AppUpdateInstallPlan.NothingToInstall),
            "not ready" to Triple(
                AppUpdateStatus.UpToDate("0.1.344", 1L),
                true,
                AppUpdateInstallPlan.NothingToInstall,
            ),
            "ready but apk file gone" to Triple(ready, true, AppUpdateInstallPlan.ApkMissing("/cache/update.apk")),
            "ready, unknown sources not allowed" to Triple(
                ready,
                false,
                AppUpdateInstallPlan.GrantUnknownSourcesPermission,
            ),
            "ready, unknown sources allowed" to Triple(ready, true, AppUpdateInstallPlan.InstallApk("/cache/update.apk")),
        )
        cases.forEach { (name, case) ->
            val (status, allowed, expected) = case
            val apkExists = name != "ready but apk file gone"
            assertEquals(name, expected, planAppUpdateInstall(status, allowed) { apkExists })
        }
    }
}
