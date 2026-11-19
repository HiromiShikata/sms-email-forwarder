package com.hiromi_shikata.smsemailforwarder.presentation

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateStatusViewTest {
    private val releases = listOf(
        AppRelease("0.1.346", "", "u1"),
        AppRelease("0.1.345", "", "u2"),
    )

    @Test
    fun `every update state maps to the view the main screen shows`() {
        val cases = listOf(
            "no status yet" to Pair(null, AppUpdateStatusView.NotChecked("0.1.344")),
            "up to date" to Pair(
                AppUpdateStatus.UpToDate("0.1.344", 5L),
                AppUpdateStatusView.UpToDate("0.1.344", 5L),
            ),
            "available" to Pair(
                AppUpdateStatus.Available("0.1.344", releases, 5L),
                AppUpdateStatusView.Available("0.1.344", "0.1.346"),
            ),
            "downloading" to Pair(
                AppUpdateStatus.Downloading("0.1.344", releases, 5L),
                AppUpdateStatusView.Downloading("0.1.344", "0.1.346"),
            ),
            "ready to install" to Pair(
                AppUpdateStatus.ReadyToInstall("0.1.344", releases, "/p", 5L),
                AppUpdateStatusView.ReadyToInstall("0.1.344", "0.1.346"),
            ),
            "ready with a later failed check keeps the install target and the failure" to Pair(
                AppUpdateStatus.ReadyToInstall(
                    "0.1.344",
                    releases,
                    "/p",
                    5L,
                    com.hiromi_shikata.smsemailforwarder.domain.entity.CheckFailure("HTTP 503", 7L),
                ),
                AppUpdateStatusView.ReadyToInstall(
                    "0.1.344",
                    "0.1.346",
                    com.hiromi_shikata.smsemailforwarder.domain.entity.CheckFailure("HTTP 503", 7L),
                ),
            ),
            "failed check keeps message and time" to Pair(
                AppUpdateStatus.CheckFailed("0.1.344", "HTTP 503", 7L),
                AppUpdateStatusView.CheckFailed("0.1.344", "HTTP 503", 7L),
            ),
        )
        cases.forEach { (name, case) ->
            assertEquals(name, case.second, resolveAppUpdateStatusView("0.1.344", case.first))
        }
    }
}
