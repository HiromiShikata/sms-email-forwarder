package com.hiromi_shikata.smsemailforwarder.domain.usecase

import com.hiromi_shikata.smsemailforwarder.domain.repository.ApkDownloadRepository
import java.io.File

class ApkDownloadUseCase(
    private val repository: ApkDownloadRepository,
) {
    fun execute(url: String): File = repository.download(url)
}
