package com.hiromi_shikata.smsemailforwarder

import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsReceiverBuildWorkRequestTest {

    @Test
    fun `buildSmsForwardWorkRequest creates expedited work request`() {
        val request = buildSmsForwardWorkRequest("sender", "body", 1000L)
        assertTrue(request.workSpec.expedited)
    }

    @Test
    fun `buildSmsForwardWorkRequest sets out of quota policy to run as non expedited`() {
        val request = buildSmsForwardWorkRequest("sender", "body", 1000L)
        assertEquals(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST, request.workSpec.outOfQuotaPolicy)
    }

    @Test
    fun `buildSmsForwardWorkRequest sets sender in input data`() {
        val request = buildSmsForwardWorkRequest("+1234567890", "body", 1000L)
        assertEquals("+1234567890", request.workSpec.input.getString(SmsForwardWorker.KEY_SENDER))
    }

    @Test
    fun `buildSmsForwardWorkRequest sets body in input data`() {
        val request = buildSmsForwardWorkRequest("sender", "Hello SMS", 1000L)
        assertEquals("Hello SMS", request.workSpec.input.getString(SmsForwardWorker.KEY_BODY))
    }

    @Test
    fun `buildSmsForwardWorkRequest sets timestamp in input data`() {
        val request = buildSmsForwardWorkRequest("sender", "body", 5000L)
        assertEquals(5000L, request.workSpec.input.getLong(SmsForwardWorker.KEY_TIMESTAMP, -1L))
    }

    @Test
    fun `buildSmsForwardWorkRequest requires a connected network`() {
        val request = buildSmsForwardWorkRequest("sender", "body", 1000L)
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `buildSmsForwardWorkRequest uses exponential backoff`() {
        val request = buildSmsForwardWorkRequest("sender", "body", 1000L)
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
    }

    @Test
    fun `buildSmsForwardWorkRequest sets 30 second initial backoff delay`() {
        val request = buildSmsForwardWorkRequest("sender", "body", 1000L)
        assertEquals(30_000L, request.workSpec.backoffDelayDuration)
    }
}
