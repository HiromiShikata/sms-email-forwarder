package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ApkDownloadUseCaseTest {
    private val repository: ApkDownloadRepository = mock()
    private val useCase = ApkDownloadUseCase(repository)

    @Test
    fun `execute enqueues download and returns download id`() {
        val url = "https://github.com/HiromiShikata/sms-email-forwarder/releases/download/v1.0.0/sms-email-forwarder_universal.apk"
        val fileName = "sms-email-forwarder_universal.apk"
        whenever(repository.enqueueDownload(url, fileName)).thenReturn(42L)

        val downloadId = useCase.execute(url, fileName)

        assertEquals(42L, downloadId)
    }

    @Test
    fun `execute passes url and filename to repository`() {
        val url = "https://example.com/app.apk"
        val fileName = "app.apk"
        whenever(repository.enqueueDownload(url, fileName)).thenReturn(1L)

        useCase.execute(url, fileName)

        org.mockito.kotlin.verify(repository).enqueueDownload(url, fileName)
    }
}
