package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class ApkDownloadUseCaseTest {
    private val repository: ApkDownloadRepository = mock()
    private val useCase = ApkDownloadUseCase(repository)

    @Test
    fun `execute returns file from repository`() {
        val url = "https://github.com/HiromiShikata/sms-email-forwarder/releases/download/v1.0.0/sms-email-forwarder_universal.apk"
        val expectedFile = File("/cache/update.apk")
        whenever(repository.download(url)).thenReturn(expectedFile)

        val result = useCase.execute(url)

        assertEquals(expectedFile, result)
    }

    @Test
    fun `execute passes url to repository`() {
        val url = "https://example.com/app.apk"
        whenever(repository.download(url)).thenReturn(File("/cache/update.apk"))

        useCase.execute(url)

        verify(repository).download(url)
    }
}
