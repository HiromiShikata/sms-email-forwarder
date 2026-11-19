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
import java.util.concurrent.atomic.AtomicInteger

class AppUpdateCheckExclusionTest {
    private val release = AppRelease("0.1.346", "- B", "https://example.test/346.apk")

    private class SlowDownloads(
        private val inFlight: AtomicInteger,
        private val maxInFlight: AtomicInteger,
        private val file: File,
    ) : ApkDownloadRepository {
        override fun download(url: String): File {
            val now = inFlight.incrementAndGet()
            maxInFlight.accumulateAndGet(now) { a, b -> maxOf(a, b) }
            Thread.sleep(20)
            inFlight.decrementAndGet()
            return file
        }
    }

    private class Store : AppUpdateStatusRepository {
        @Volatile
        var stored: AppUpdateStatus? = null
        override fun load(): AppUpdateStatus? = stored
        override fun save(status: AppUpdateStatus) {
            stored = status
        }
    }

    private object NoopNotifier : AppUpdateNotifier {
        override fun notifyReadyToInstall(releases: List<AppRelease>) = Unit
        override fun cancel() = Unit
    }

    @Test
    fun `concurrent checks never download at the same time`() {
        val inFlight = AtomicInteger(0)
        val maxInFlight = AtomicInteger(0)
        val apk = File.createTempFile("update", ".apk")
        val threads = (1..6).map {
            Thread {
                AppUpdateCheckUseCase(
                    releaseRepository = object : AppUpdateRepository {
                        override fun fetchReleases(): List<AppRelease> = listOf(release)
                    },
                    apkDownloadRepository = SlowDownloads(inFlight, maxInFlight, apk),
                    notifier = NoopNotifier,
                    statusRepository = Store(),
                    installedVersion = "0.1.344",
                    nowEpochMillis = { 1L },
                ).execute()
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(1, maxInFlight.get())
    }

    @Test
    fun `each concurrent check still completes with its own result`() {
        val results = java.util.concurrent.ConcurrentLinkedQueue<AppUpdateStatus>()
        val apk = File.createTempFile("update", ".apk")
        val threads = (1..3).map {
            Thread {
                results += AppUpdateCheckUseCase(
                    releaseRepository = object : AppUpdateRepository {
                        override fun fetchReleases(): List<AppRelease> = emptyList()
                    },
                    apkDownloadRepository = SlowDownloads(AtomicInteger(), AtomicInteger(), apk),
                    notifier = NoopNotifier,
                    statusRepository = Store(),
                    installedVersion = "0.1.346",
                    nowEpochMillis = { 2L },
                ).execute()
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(3, results.size)
        assertTrue(results.all { it is AppUpdateStatus.UpToDate })
    }
}
