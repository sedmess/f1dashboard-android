package ru.n1ks.f1dashboard

import android.app.Application
import android.content.Context
import org.acra.config.dialog
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import ru.n1ks.f1dashboard.reporting.CrashReportActivity

class DashboardApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            this.
            reportFormat = StringFormat.JSON

            dialog {
                //allows other customization
                reportDialogClass = CrashReportActivity::class.java
            }
        }
    }
}