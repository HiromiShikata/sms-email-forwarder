package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateNotifier
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateRepository
import com.hiromi_shikata.smsemailforwarder.domain.repository.AppUpdateStatusRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class AppUpdateCheckUseCaseTest {
    private val events = mutableListOf<String>()

    private val newer = listOf(
        AppRelease("0.1.346", "- B", "https://example.test/346.apk"),
        AppRelease("0.1.345", "- A", "https://example.test/345.apk"),
    )

    private class FakeReleases(
        private val result: () -> List<AppRelease>,
    ) : AppUpdateRepository {
        override fun fetchReleases(): List<AppRelease> = result()
    }

    private class FakeDownloads(
        private val events: MutableList<String>,
        private val file: File,
        private val fail: Boolean,
    ) : ApkDownloadRepository {
        override fun download(url: String): File {
            events += "download:$url"
            if (fail) throw IOException("connection reset")
            return file
        }
    }

    private class FakeNotifier(private val events: MutableList<String>) : AppUpdateNotifier {
        val notified = mutableListOf<List<AppRelease>>()
        override fun notifyReadyToInstall(releases: List<AppRelease>) {
            events += "notify"
            notified += releases
        }

        override fun cancel() {
            events += "cancel"
        }
    }

    private class FakeStatusStore(private val events: MutableList<String>) : AppUpdateStatusRepository {
        var stored: AppUpdateStatus? = null
        override fun load(): AppUpdateStatus? = stored
        override fun save(status: AppUpdateStatus) {
            events += "save:${status::class.simpleName}"
            stored = status
        }
    }

    private fun useCase(
        releases: () -> List<AppRelease> = { newer },
        apk: File = File.createTempFile("update", ".apk"),
        failDownload: Boolean = false,
        store: FakeStatusStore = FakeStatusStore(events),
        notifier: FakeNotifier = FakeNotifier(events),
    ) = AppUpdateCheckUseCase(
        releaseRepository = FakeReleases(releases),
        apkDownloadRepository = FakeDownloads(events, apk, failDownload),
        notifier = notifier,
        statusRepository = store,
        installedVersion = "0.1.344",
        nowEpochMillis = { 9_000L },
    )

    @Test
    fun `newer release is downloaded first and the notification follows the download`() {
        val apk = File.createTempFile("update", ".apk")
        val notifier = FakeNotifier(events)
        val store = FakeStatusStore(events)
        val status = useCase(apk = apk, store = store, notifier = notifier).execute()

        assertEquals(
            listOf(
                "save:Available",
                "save:Downloading",
                "download:https://example.test/346.apk",
                "save:ReadyToInstall",
                "notify",
            ),
            events,
        )
        assertEquals(newer, notifier.notified.single())
        assertEquals(AppUpdateStatus.ReadyToInstall("0.1.344", newer, apk.absolutePath, 9_000L), status)
        assertEquals(status, store.stored)
    }

    @Test
    fun `no newer release marks up to date and cancels the notification without downloading`() {
        val status = useCase(releases = { emptyList() }).execute()
        assertEquals(AppUpdateStatus.UpToDate("0.1.344", 9_000L), status)
        assertEquals(listOf("cancel", "save:UpToDate"), events)
    }

    @Test
    fun `release fetch failure is recorded as a check failure with its message`() {
        val notifier = FakeNotifier(events)
        val status = useCase(releases = { throw IOException("HTTP 503 while reading feed") }, notifier = notifier)
            .execute()
        assertEquals(AppUpdateStatus.CheckFailed("0.1.344", "HTTP 503 while reading feed", 9_000L), status)
        assertTrue(events.none { it.startsWith("download") })
        assertTrue(notifier.notified.isEmpty())
    }

    @Test
    fun `download failure is recorded as a check failure and never notifies`() {
        val notifier = FakeNotifier(events)
        val status = useCase(failDownload = true, notifier = notifier).execute()
        assertEquals(AppUpdateStatus.CheckFailed("0.1.344", "connection reset", 9_000L), status)
        assertTrue(notifier.notified.isEmpty())
        assertTrue(!events.contains("notify"))
    }

    @Test
    fun `an already downloaded update for the same version is kept without downloading again`() {
        val apk = File.createTempFile("update", ".apk")
        val store = FakeStatusStore(events)
        store.stored = AppUpdateStatus.ReadyToInstall("0.1.344", newer, apk.absolutePath, 1_000L)
        val notifier = FakeNotifier(events)
        val status = useCase(store = store, notifier = notifier).execute()
        assertEquals(AppUpdateStatus.ReadyToInstall("0.1.344", newer, apk.absolutePath, 9_000L), status)
        assertTrue(events.none { it.startsWith("download") })
        assertTrue(notifier.notified.isEmpty())
    }
}
