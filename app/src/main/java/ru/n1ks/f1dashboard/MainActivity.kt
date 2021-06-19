package ru.n1ks.f1dashboard

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.toSingle
import ru.n1ks.f1dashboard.Properties.Companion.loadProperties
import ru.n1ks.f1dashboard.capture.LiveCaptureWorker
import ru.n1ks.f1dashboard.livedata.LiveData
import ru.n1ks.f1dashboard.livedata.LiveDataFields
import ru.n1ks.f1dashboard.model.TelemetryPacketDeserializer
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var debugFrameCountTextView: TextView
    private lateinit var sessionTimeTextView: TextView

    private lateinit var serviceConnection: TelemetryProviderService.Connection

    private lateinit var liveData: LiveData

    @Volatile
    private var liveCaptureWorker: LiveCaptureWorker? = null

    private var isReplaying = false

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        findViewById<View>(R.id.drsCaption).setOnClickListener {
            throw RuntimeException("ops!")
        }

        debugFrameCountTextView = findViewById(R.id.debugFrameCount)
        debugFrameCountTextView.apply {
            setOnClickListener { showEndpoint() }
            setOnLongClickListener { toggleCapture(); true }
        }

        sessionTimeTextView = findViewById(R.id.sessionTimeValue)
        sessionTimeTextView.setOnLongClickListener { toggleReplay(); true }

        liveData = LiveData(this, LiveDataFields)

        serviceConnection = object : TelemetryProviderService.Connection {

            private var connected: Boolean = false

            private var flowDisposable: Disposable? = null

            override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                Log.d(TAG, "service $componentName connected")
                flowDisposable = (binder as TelemetryProviderService.Binder).flow()
                    .doOnNext { onPacket(it) }
                    .map { TelemetryPacketDeserializer.map(it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { packet -> liveData.onUpdate(packet) }
                connected = true
            }

            override fun onServiceDisconnected(componentName: ComponentName?) {
                onUnbind()
            }

            override fun onUnbind() {
                flowDisposable?.dispose()
                Log.d(TAG, "service $componentName disconnected")
                connected = false
            }

            override fun isConnected() = connected
        }

        showEndpoint()
    }

    override fun onStart() {
        super.onStart()

        TelemetryProviderService.bindService(this, ListenerService::class, serviceConnection)
    }

    override fun onStop() {
        if (serviceConnection.isConnected()) {
            TelemetryProviderService.unbindService(this, serviceConnection)
        }

        stopCapture()

        super.onStop()
    }

    @SuppressLint("CheckResult")
    private fun showEndpoint() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val ipAddress =
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        val dialog = AlertDialog.Builder(this)
            .setMessage("Endpoint: $ipAddress:${getSharedPreferences(Properties.Name, Context.MODE_PRIVATE).loadProperties().port}")
            .create()
        dialog.toSingle()
            .delay(5, TimeUnit.SECONDS)
            .subscribe { it -> if (it.isShowing) it.dismiss() }
        dialog.show()
    }

    private fun toggleReplay() {
        if (isReplaying) {
            stopReplay()
        } else {
            startReplay()
        }
    }

    private fun startReplay() {
        fileList().find { it == "latest.cap" }.also { fileName ->
            if (fileName == null) {
                Toast.makeText(this, "No captures found", Toast.LENGTH_SHORT).show()
                return@also
            }
            stopCapture()

            TelemetryProviderService.unbindService(this, serviceConnection)

            sessionTimeTextView.background = ContextCompat.getDrawable(this, R.color.warn)

            TelemetryProviderService.bindService(
                this,
                ReplayService::class,
                serviceConnection,
                ReplayService.SourcePath to File(this.filesDir, fileName).absolutePath
            )
            isReplaying = true
        }
    }

    private fun stopReplay() {
        sessionTimeTextView.background = null
        TelemetryProviderService.unbindService(this, serviceConnection)
        TelemetryProviderService.bindService(this, ListenerService::class, serviceConnection)
        isReplaying = false
    }

    private fun toggleCapture() {
        synchronized(this) {
            if (liveCaptureWorker != null) {
                stopCapture()
            } else {
                startCapture()
            }
        }
    }

    private fun startCapture() {
        synchronized(this) {
            if (liveCaptureWorker != null) {
                stopCapture()
            }
            val captureFile = File(this.filesDir, "latest.cap") //todo filename
            liveCaptureWorker = LiveCaptureWorker(captureFile)
            Log.d(TAG, "start capturing to file: " + captureFile.absolutePath)
            debugFrameCountTextView.background = ContextCompat.getDrawable(this, R.color.warn)
        }
    }

    private fun stopCapture() {
        synchronized(this) {
            liveCaptureWorker?.apply {
                Log.d(TAG, "stop capturing")
                close()
                debugFrameCountTextView.background = null
                Toast.makeText(this@MainActivity, "Captured ${this.frameCount} frames. Saved to: " + file.path, Toast.LENGTH_SHORT).show()
            }
            liveCaptureWorker = null
        }
    }

    private fun onPacket(packet: ByteArray) {
        if (liveCaptureWorker != null) {
            synchronized(this) {
                if (liveCaptureWorker != null) {
                    liveCaptureWorker!!.onPacket(packet)
                }
            }
        }
    }
}