package com.hiromi_shikata.smsemailforwarder.data.local

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.presentation.AppUpdateInstallActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AndroidAppUpdateNotifierTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `the notification tap opens the install activity and lists every newer release`() {
        AndroidAppUpdateNotifier.createChannel(app)
        val tapIntent = Intent(app, AppUpdateInstallActivity::class.java)
        val releases = listOf(
            AppRelease("0.1.346", "- Newer change", "u2"),
            AppRelease("0.1.345", "- Older change", "u1"),
        )
        AndroidAppUpdateNotifier(app, tapIntent).notifyReadyToInstall(releases)

        val posted = shadowOf(app.getSystemService<NotificationManager>()).allNotifications.single()
        val tapTarget = shadowOf(posted.contentIntent).savedIntent
        assertEquals(AppUpdateInstallActivity::class.java.name, tapTarget.component?.className)
        val bigText = posted.extras.getCharSequence("android.bigText").toString()
        assertTrue(bigText.contains("v0.1.346\n- Newer change"))
        assertTrue(bigText.contains("v0.1.345\n- Older change"))
    }
}
