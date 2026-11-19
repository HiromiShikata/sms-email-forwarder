package com.hiromi_shikata.smsemailforwarder.presentation

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsAppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppRelease
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppUpdateInstallActivityTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun storeReady(apk: File) {
        SharedPrefsAppUpdateStatusRepository(app).save(
            AppUpdateStatus.ReadyToInstall(
                "0.1.344",
                listOf(AppRelease("0.1.345", "n", "u")),
                apk.absolutePath,
                1L,
            ),
        )
    }

    @Test
    fun `installing the downloaded apk opens the package install view with a content uri and read grant`() {
        val apk = File(app.cacheDir, "update.apk").apply { writeText("apk") }
        storeReady(apk)
        shadowOf(app.packageManager).setCanRequestPackageInstalls(true)
        Robolectric.buildActivity(AppUpdateInstallActivity::class.java).create()
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("application/vnd.android.package-archive", started.type)
        assertEquals("content", started.data!!.scheme)
        assertEquals("${app.packageName}.provider", started.data!!.authority)
        assertTrue(started.data!!.lastPathSegment == "update.apk")
        assertTrue(started.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun `without the unknown sources permission the activity opens the settings screen for this package`() {
        val apk = File(app.cacheDir, "update.apk").apply { writeText("apk") }
        storeReady(apk)
        shadowOf(app.packageManager).setCanRequestPackageInstalls(false)
        Robolectric.buildActivity(AppUpdateInstallActivity::class.java).create()
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, started.action)
        assertEquals("package:${app.packageName}", started.data.toString())
    }

    @Test
    fun `with nothing ready the activity starts no install and finishes`() {
        val activity = Robolectric.buildActivity(AppUpdateInstallActivity::class.java).create().get()
        assertEquals(null, shadowOf(app).nextStartedActivity)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun `unknown sources settings intent targets this package`() {
        val intent = unknownSourcesSettingsIntent(app.packageName)
        assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, intent.action)
        assertEquals("package:${app.packageName}", intent.data.toString())
    }
}
