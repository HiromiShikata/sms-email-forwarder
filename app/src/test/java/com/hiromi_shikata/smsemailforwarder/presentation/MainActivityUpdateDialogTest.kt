package com.hiromi_shikata.smsemailforwarder.presentation

import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityUpdateDialogTest {

    private val update = AppUpdate(
        latestVersion = "1.1.0",
        downloadUrl = "https://example.com/app.apk",
        isUpdateAvailable = true,
    )

    @Test
    fun `shouldShowUpdateDialog returns true when update is available and dialog not yet shown`() {
        assertTrue(shouldShowUpdateDialog(update, hasShownUpdateDialog = false))
    }

    @Test
    fun `shouldShowUpdateDialog returns false when update is null`() {
        assertFalse(shouldShowUpdateDialog(null, hasShownUpdateDialog = false))
    }

    @Test
    fun `shouldShowUpdateDialog returns false when dialog already shown in same session`() {
        assertFalse(shouldShowUpdateDialog(update, hasShownUpdateDialog = true))
    }
}
