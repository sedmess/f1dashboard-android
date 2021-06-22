package ru.n1ks.f1dashboard

import android.app.Application
import ru.n1ks.f1dashboard.reporting.initReporting

class DashboardApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initReporting(this)
    }
}