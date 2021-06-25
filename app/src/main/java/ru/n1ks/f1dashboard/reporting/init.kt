package ru.n1ks.f1dashboard.reporting

import android.content.Context
import android.util.Base64
import android.util.Log
import cat.ereza.customactivityoncrash.config.CaocConfig
import org.json.JSONArray

private const val TAG = "ErrorReporting"

fun initReporting(context: Context) {
    CaocConfig.Builder.create()
        .errorActivity(CrashReportActivity::class.java)
        .apply()

    val currentDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { t,e ->
        try {
            val lastPackets = PacketTail.tail()
            val jsonArray = JSONArray()
            lastPackets.forEach { jsonArray.put(Base64.encode(it, Base64.NO_WRAP or Base64.DEFAULT).decodeToString()) }
            CrashDataStoreHelper(context).store(mapOf(ReportingKeys.LastPacketsData to jsonArray.toString()))
        } catch (e: Throwable) {
            Log.e(TAG, "error on collecting last packets", e)
        }
        currentDefaultUncaughtExceptionHandler?.uncaughtException(t, e)
    }
}