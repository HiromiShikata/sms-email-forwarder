package com.hiromi_shikata.smsemailforwarder.presentation

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityPermissionButtonTest {

    @Test
    fun `resolveGrantPermissionButtonVisibility returns GONE when permission is granted`() {
        assertEquals(View.GONE, resolveGrantPermissionButtonVisibility(true))
    }

    @Test
    fun `resolveGrantPermissionButtonVisibility returns VISIBLE when permission is not granted`() {
        assertEquals(View.VISIBLE, resolveGrantPermissionButtonVisibility(false))
    }

    @Test
    fun `resolvePermissionGrantStatus returns true when all permissions granted`() {
        assertEquals(
            true,
            resolvePermissionGrantStatus(
                mapOf(
                    "android.permission.RECEIVE_SMS" to true,
                    "android.permission.READ_SMS" to true,
                ),
            ),
        )
    }

    @Test
    fun `resolvePermissionGrantStatus returns false when any permission denied`() {
        assertEquals(
            false,
            resolvePermissionGrantStatus(
                mapOf(
                    "android.permission.RECEIVE_SMS" to false,
                    "android.permission.READ_SMS" to true,
                ),
            ),
        )
    }

    @Test
    fun `resolvePermissionGrantStatus returns false when all permissions denied`() {
        assertEquals(
            false,
            resolvePermissionGrantStatus(
                mapOf(
                    "android.permission.RECEIVE_SMS" to false,
                    "android.permission.READ_SMS" to false,
                ),
            ),
        )
    }
}
