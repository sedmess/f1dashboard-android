package ru.n1ks.f1dashboard

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.toSingle
import ru.n1ks.f1dashboard.Properties.Companion.loadProperties
import ru.n1ks.f1dashboard.livedata.LiveData
import ru.n1ks.f1dashboard.livedata.LiveDataFields
import java.util.concurrent.TimeUnit

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    companion object {

        const val TAG = "MainActivity"
    }

    private lateinit var serviceConnection: ServiceConnection
    private val properties: Lazy<Properties> = lazy { getSharedPreferences(Properties.Name, Context.MODE_PRIVATE).loadProperties() }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        findViewById<View>(R.id.debugFrameCount).setOnClickListener { showEndpoint() }

        val liveData = LiveData(this, LiveDataFields)

        serviceConnection = object : ServiceConnection {

            @SuppressLint("CheckResult")
            override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                Log.d(TAG, "service $componentName connected")
                (binder as ListenerService.Binder).flow()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { packet -> liveData.onUpdate(packet) }
            }

            override fun onServiceDisconnected(componentName: ComponentName?) {
                Log.d(TAG, "service $componentName disconnected")
            }
        }

        showEndpoint()
    }

    override fun onStart() {
        super.onStart()

        ListenerService.bindService(this, properties.value, serviceConnection)
    }

    override fun onStop() {
        ListenerService.unbindService(this, serviceConnection)

        super.onStop()
    }

    @SuppressLint("CheckResult")
    private fun showEndpoint() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        val dialog = AlertDialog.Builder(this)
            .setMessage("Endpoint: $ipAddress:${properties.value.port}")
            .create()
        dialog.toSingle()
            .delay(5, TimeUnit.SECONDS)
            .subscribe { it -> if (it.isShowing) it.dismiss() }
        dialog.show()
    }

}