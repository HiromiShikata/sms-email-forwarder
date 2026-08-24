package com.hiromi_shikata.smsemailforwarder

import com.hiromi_shikata.smsemailforwarder.data.local.AndroidSmsForwardingInProgressNotifier
import org.junit.Assert.assertEquals
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
}
