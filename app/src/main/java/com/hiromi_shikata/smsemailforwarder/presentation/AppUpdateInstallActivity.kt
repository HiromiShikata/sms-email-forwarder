package com.hiromi_shikata.smsemailforwarder.presentation

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import com.hiromi_shikata.smsemailforwarder.BuildConfig
import com.hiromi_shikata.smsemailforwarder.R
import com.hiromi_shikata.smsemailforwarder.data.local.SharedPrefsAppUpdateStatusRepository
import java.io.File

class AppUpdateInstallActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val plan = planAppUpdateInstall(
            status = SharedPrefsAppUpdateStatusRepository(this).load(),
            canRequestPackageInstalls = packageManager.canRequestPackageInstalls(),
            apkExists = { File(it).exists() },
        )
        when (plan) {
            AppUpdateInstallPlan.NothingToInstall -> toast(R.string.app_update_nothing_to_install)
            is AppUpdateInstallPlan.ApkMissing -> toast(R.string.app_update_apk_missing)
            AppUpdateInstallPlan.GrantUnknownSourcesPermission -> {
                startActivity(unknownSourcesSettingsIntent(packageName))
                toast(R.string.app_update_allow_install_then_retry)
            }
            is AppUpdateInstallPlan.InstallApk -> {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${BuildConfig.APPLICATION_ID}.provider",
                    File(plan.apkFilePath),
                )
                startActivity(installApkIntent(uri))
            }
        }
        finish()
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
    }
}
