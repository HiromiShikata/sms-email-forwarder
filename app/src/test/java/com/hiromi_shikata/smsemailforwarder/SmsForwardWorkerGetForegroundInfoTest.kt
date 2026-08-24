package com.hiromi_shikata.smsemailforwarder

import android.content.pm.ServiceInfo
import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingInProgressNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsForwardWorkerGetForegroundInfoTest {

    @Test
    fun `getForegroundInfo channel id is sms_forwarding`() {
        assertEquals("sms_forwarding", AndroidSmsForwardingInProgressNotifier.CHANNEL_ID)
    }

    @Test
    fun `getForegroundInfo notification id is 3000`() {
        assertEquals(3000, AndroidSmsForwardingInProgressNotifier.NOTIFICATION_ID)
    }

    @Test
    fun `getForegroundServiceTypeForSdk returns FOREGROUND_SERVICE_TYPE_DATA_SYNC on API 34`() {
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, getForegroundServiceTypeForSdk(34))
    }

    @Test
    fun `getForegroundServiceTypeForSdk returns FOREGROUND_SERVICE_TYPE_DATA_SYNC on API 36`() {
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, getForegroundServiceTypeForSdk(36))
    }

    @Test
    fun `getForegroundServiceTypeForSdk returns null on API 33`() {
        assertNull(getForegroundServiceTypeForSdk(33))
    }

    @Test
    fun `getForegroundServiceTypeForSdk returns null on minimum supported API 26`() {
        assertNull(getForegroundServiceTypeForSdk(26))
    }
}
