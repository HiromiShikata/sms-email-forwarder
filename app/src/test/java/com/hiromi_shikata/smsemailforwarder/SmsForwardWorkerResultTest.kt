package com.hiromi_shikata.smsemailforwarder

import androidx.work.ListenableWorker
import com.hiromi_shikata.smsemailforwarder.domain.entity.SmsForwardAttemptOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

class SmsForwardWorkerResultTest {

    @Test
    fun `toWorkerResult maps each attempt outcome to its worker result`() {
        val cases = listOf(
            SmsForwardAttemptOutcome.RETRY_SCHEDULED to ListenableWorker.Result.Retry::class.java,
            SmsForwardAttemptOutcome.FORWARDED to ListenableWorker.Result.Success::class.java,
            SmsForwardAttemptOutcome.FAILED to ListenableWorker.Result.Success::class.java,
        )

        cases.forEach { (outcome, expectedResultClass) ->
            assertEquals("outcome $outcome", expectedResultClass, toWorkerResult(outcome).javaClass)
        }
    }
}
