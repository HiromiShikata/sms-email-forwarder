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
}
