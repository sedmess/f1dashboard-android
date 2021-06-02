package ru.n1ks.f1dashboard.reporting

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.acra.dialog.CrashReportDialogHelper
import ru.n1ks.f1dashboard.R

class CrashReportActivity : AppCompatActivity() {

    private lateinit var helper: CrashReportDialogHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            helper = CrashReportDialogHelper(this, intent)
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
                startActivity(
                    Intent.createChooser(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, helper.reportData.toJSON())
                            type = "text/plain"
                        },
                        getString(R.string.crashReportShareCaption)
                    )
                )
                helper.cancelReports()
                finish()
            }
            .setNegativeButton(R.string.crashReportCancelCaption) { _, _ ->
                helper.cancelReports()
                finish()
            }
            .create().apply {
                setCanceledOnTouchOutside(false)
                show()
            }
    }
}