package com.hiromi_shikata.smsemailforwarder.presentation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hiromi_shikata.smsemailforwarder.BuildConfig
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsAppUpdateStatusRepository
import com.hiromi_shikata.smsemailforwarder.domain.entity.AppUpdateStatus
import java.io.File
class AppUpdateInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = SharedPrefsAppUpdateStatusRepository(this).load()
        val ready = status as? AppUpdateStatus.ReadyToInstall
        if (ready == null) {
            Toast.makeText(this, R.string.app_update_nothing_to_install, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val apk = File(ready.apkFilePath)
        if (!apk.exists()) {
            Toast.makeText(this, R.string.app_update_apk_missing, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (!packageManager.canRequestPackageInstalls()) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")),
            )
            Toast.makeText(this, R.string.app_update_allow_install_then_retry, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", apk)
        startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
        finish()
    }
}
