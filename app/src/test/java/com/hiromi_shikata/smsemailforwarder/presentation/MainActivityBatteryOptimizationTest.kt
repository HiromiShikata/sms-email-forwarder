package com.hiromi_shikata.smsemailforwarder.presentation

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityBatteryOptimizationTest {

    @Test
    fun `battery optimization button is visible when not ignoring battery optimizations`() {
        assertEquals(View.VISIBLE, resolveBatteryOptimizationButtonVisibility(false))
    }

    @Test
    fun `battery optimization button is gone when ignoring battery optimizations`() {
        assertEquals(View.GONE, resolveBatteryOptimizationButtonVisibility(true))
    }
}
