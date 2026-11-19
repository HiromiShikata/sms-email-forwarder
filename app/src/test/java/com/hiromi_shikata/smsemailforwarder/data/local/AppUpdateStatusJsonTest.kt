package com.hiromi_shikata.smsemailforwarder.data.local

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateStatusJsonTest {
    private val releases = listOf(
        AppRelease("0.1.346", "- Add \"x\"\n- Fix", "https://example.test/346.apk"),
        AppRelease("0.1.345", "", "https://example.test/345.apk"),
    )

    @Test
    fun `every status variant survives an encode and decode round trip`() {
        val cases = listOf(
            AppUpdateStatus.UpToDate("0.1.346", 1_000L),
            AppUpdateStatus.Available("0.1.344", releases, 2_000L),
            AppUpdateStatus.Downloading("0.1.344", releases, 3_000L),
            AppUpdateStatus.ReadyToInstall("0.1.344", releases, "/cache/update.apk", 4_000L),
            AppUpdateStatus.CheckFailed("0.1.344", "HTTP 503 while reading https://example.test", 5_000L),
            AppUpdateStatus.ReadyToInstall(
                "0.1.344",
                releases,
                "/cache/update.apk",
                6_000L,
                com.hiromi_shikata.smsemailforwarder.domain.entity.CheckFailure("HTTP 503", 7_000L),
            ),
        )
        cases.forEach { status ->
            assertEquals(status, AppUpdateStatusJson.decode(AppUpdateStatusJson.encode(status)))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects an unknown status type`() {
        AppUpdateStatusJson.decode("""{"type":"BOGUS","installedVersion":"1","checkedAtEpochMillis":1}""")
    }
}
