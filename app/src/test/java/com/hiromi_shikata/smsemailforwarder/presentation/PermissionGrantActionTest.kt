package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionGrantActionTest {
    private val sms = Manifest.permission.RECEIVE_SMS
    private val notifications = Manifest.permission.POST_NOTIFICATIONS

    @Test
    fun `missing permissions are requested by dialog unless they were requested and can no longer be requested`() {
        val cases = listOf(
            "never requested, dialog allowed" to Triple(
                listOf(sms),
                emptySet<String>(),
                PermissionGrantAction.Request(listOf(sms)),
            ),
            "requested before and rationale gone, permanently denied" to Triple(
                listOf(notifications),
                setOf(notifications),
                PermissionGrantAction.OpenSettings(notifications),
            ),
            "requested before but rationale still shown, dialog allowed" to Triple(
                listOf(notifications),
                setOf(notifications),
                PermissionGrantAction.Request(listOf(notifications)),
            ),
            "one requestable and one permanently denied, requests the requestable" to Triple(
                listOf(sms, notifications),
                setOf(notifications),
                PermissionGrantAction.Request(listOf(sms)),
            ),
            "nothing missing" to Triple(emptyList<String>(), setOf(sms), PermissionGrantAction.None),
        )
        cases.forEach { (name, case) ->
            val (missing, requestedBefore, expected) = case
            val rationale = name.startsWith("requested before but rationale still shown")
            val actual = resolvePermissionGrantAction(missing, requestedBefore) { rationale }
            assertEquals(name, expected, actual)
        }
    }
}
