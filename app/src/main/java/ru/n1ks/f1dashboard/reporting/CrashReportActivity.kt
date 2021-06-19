package ru.n1ks.f1dashboard.reporting

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import org.acra.ACRA
import org.acra.ReportField
import org.acra.dialog.CrashReportDialogHelper
import org.json.JSONArray
import ru.n1ks.f1dashboard.R

class CrashReportActivity : AppCompatActivity() {

    private lateinit var helper: CrashReportDialogHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ACRA.errorReporter.putCustomData("test1", "test1")
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
                val reportData = helper.reportData
                UDPPacketTail.tail().apply {
                    if (isNotEmpty()) {
                        val tailArr = JSONArray()
                        forEach {
                            val encodedPacket = String(Base64.encode(it, Base64.DEFAULT))
                            tailArr.put(encodedPacket)
                        }
                        reportData.put(ReportField.CUSTOM_DATA, tailArr)
                        reportData.put("udp_tail", tailArr)
                    } else {
                        reportData.put(ReportField.CUSTOM_DATA, "no data")
                        reportData.put("udp_tail", "no_data")
                    }
                }
                reportData.put("test2", "test2")
                startActivity(
                    Intent.createChooser(
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, reportData.toJSON())
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