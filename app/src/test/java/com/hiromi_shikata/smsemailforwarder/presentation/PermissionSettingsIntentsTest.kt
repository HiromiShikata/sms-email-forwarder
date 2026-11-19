package com.hiromi_shikata.smsemailforwarder.presentation

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PermissionSettingsIntentsTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `notifications open notification settings for this app and other permissions open app details`() {
        val notifications = permissionSettingsIntent(Manifest.permission.POST_NOTIFICATIONS, app.packageName)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, notifications.action)
        assertEquals(app.packageName, notifications.getStringExtra(Settings.EXTRA_APP_PACKAGE))

        val sms = permissionSettingsIntent(Manifest.permission.RECEIVE_SMS, app.packageName)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, sms.action)
        assertEquals("package:${app.packageName}", sms.data.toString())
    }
}
