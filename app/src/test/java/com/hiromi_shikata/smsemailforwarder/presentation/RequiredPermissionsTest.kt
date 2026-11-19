package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class RequiredPermissionsTest {
    private val sms = setOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

    @Test
    fun `missing permissions include notifications from android 13 and not before`() {
        val cases = listOf(
            "before android 13, sms granted" to Triple(32, sms, emptyList<String>()),
            "android 13, notifications denied" to Triple(33, sms, listOf(Manifest.permission.POST_NOTIFICATIONS)),
            "android 13, everything granted" to Triple(
                33,
                sms + Manifest.permission.POST_NOTIFICATIONS,
                emptyList<String>(),
            ),
            "android 13, sms missing" to Triple(33, emptySet(), listOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS,
            )),
            "before android 13, sms missing" to Triple(32, emptySet(), listOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
            )),
        )
        cases.forEach { (name, case) ->
            val (sdk, granted, expected) = case
            assertEquals(name, expected, missingRequiredPermissions(sdk) { it in granted })
        }
    }
}
