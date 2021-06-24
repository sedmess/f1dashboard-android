package ru.n1ks.f1dashboard.reporting

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import ru.n1ks.f1dashboard.R

class CrashReportActivity : AppCompatActivity() {

    private lateinit var crashDataStoreHelper: CrashDataStoreHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            crashDataStoreHelper = CrashDataStoreHelper(this)
            buildAndShowDialog()
        } catch (e: IllegalArgumentException) {
            finish()
        }
    }

    private fun buildAndShowDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.crashReportTitle)
            .setMessage(R.string.crashReportText)
            .setPositiveButton(R.string.crashReportSendCaption) { _, _ ->
                val errorDetails =
                    CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent) +
                            (crashDataStoreHelper.load()[ReportingKeys.LastPacketsData]?.let { "\nLast packets:\n$it" } ?: "")
                startActivity(
                    Intent.createChooser(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, errorDetails)
                            type = "text/plain"
                        },
                        getString(R.string.crashReportShareCaption)
                    )
                )
                crashDataStoreHelper.delete()
                finish()
            }
            .setNegativeButton(R.string.crashReportCancelCaption) { _, _ ->
                crashDataStoreHelper.delete()
                finish()
            }
            .create().apply { setCanceledOnTouchOutside(false); show() }
    }
}