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

    @Test
    fun `status text names exactly the missing permissions and shows granted only when none are missing`() {
        val cases = listOf(
            "all granted" to Pair(emptyList<String>(), PermissionStatusText.Granted),
            "notifications only missing" to Pair(
                listOf(Manifest.permission.POST_NOTIFICATIONS),
                PermissionStatusText.Missing(sms = false, notifications = true),
            ),
            "sms only missing" to Pair(
                listOf(Manifest.permission.READ_SMS),
                PermissionStatusText.Missing(sms = true, notifications = false),
            ),
            "both missing" to Pair(
                listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS),
                PermissionStatusText.Missing(sms = true, notifications = true),
            ),
        )
        cases.forEach { (name, case) ->
            val (missing, expected) = case
            assertEquals(name, expected, resolvePermissionStatusText(missing))
        }
    }
}
