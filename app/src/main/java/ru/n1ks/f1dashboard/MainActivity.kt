package ru.n1ks.f1dashboard

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.text.format.Formatter
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.Properties.Companion.loadProperties
import ru.n1ks.f1dashboard.capture.LiveCaptureFrame
import ru.n1ks.f1dashboard.capture.LiveCaptureWorker
import ru.n1ks.f1dashboard.livedata.LiveData
import ru.n1ks.f1dashboard.livedata.LiveDataFields
import ru.n1ks.f1dashboard.model.TelemetryPacketDeserializer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.DatagramPacket
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var debugFrameCountTextView: TextView
    private lateinit var sessionTimeTextView: TextView

    private lateinit var serviceConnection: ServiceConnection
    private val properties: Lazy<Properties> =
        lazy { getSharedPreferences(Properties.Name, Context.MODE_PRIVATE).loadProperties() }

    private lateinit var liveData: LiveData

    @Volatile
    private var liveCaptureWorker: LiveCaptureWorker? = null

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        debugFrameCountTextView = findViewById(R.id.debugFrameCount)
        debugFrameCountTextView.apply {
            setOnClickListener { showEndpoint() }
            setOnLongClickListener { toggleCapture(); true }
        }

        sessionTimeTextView = findViewById(R.id.sessionTimeValue)
        sessionTimeTextView.setOnLongClickListener { toggleReplay(); true }

        liveData = LiveData(this, LiveDataFields)

        serviceConnection = object : ServiceConnection {

            private var flowDisposable: Disposable? = null

            override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                Log.d(TAG, "service $componentName connected")
                flowDisposable = (binder as ListenerService.Binder).flow()
                    .doOnNext { onPacket(it) }
                    .map { TelemetryPacketDeserializer.map(it.data) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { packet -> liveData.onUpdate(packet) }
            }

            override fun onServiceDisconnected(componentName: ComponentName?) {
                flowDisposable?.dispose()
                Log.d(TAG, "service $componentName disconnected")
            }
        }

        showEndpoint()
    }

    override fun onStart() {
        super.onStart()

        TelemetryProviderService.bindService(this, properties.value, ListenerService::class, serviceConnection)
    }

    override fun onStop() {
        TelemetryProviderService.unbindService(this, serviceConnection) //todo check bound

        stopCapture()

        super.onStop()
    }

    @SuppressLint("CheckResult")
    private fun showEndpoint() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION") val ipAddress =
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        val dialog = AlertDialog.Builder(this)
            .setMessage("Endpoint: $ipAddress:${properties.value.port}")
            .create()
        dialog.toSingle()
            .delay(5, TimeUnit.SECONDS)
            .subscribe { it -> if (it.isShowing) it.dismiss() }
        dialog.show()
    }

    private fun toggleReplay() { //todo
        startReplay()
    }

    private fun startReplay() {
        fileList().find { it == "latest.cap" }.also { fileName ->
            if (fileName == null) {
                Toast.makeText(this, "No captures found", Toast.LENGTH_SHORT).show()
                return@also
            }
            stopCapture()
            ListenerService.unbindService(this, serviceConnection)
            sessionTimeTextView.background = ContextCompat.getDrawable(this, R.color.warn)

            val inputStream = BufferedInputStream(FileInputStream(File(this.filesDir, fileName)))
            Log.d(TAG, "open capture file")
            Toast.makeText(this, "Replay...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "start replaying")
            var prevTS = 0L
            LiveCaptureFrame.flow(inputStream)
                .subscribeOn(Schedulers.newThread())
                .doOnTerminate {
                    Log.d(TAG, "close capture file")
                    inputStream.close()
                }
                .doOnNext { SystemClock.sleep(it.timestamp - prevTS); prevTS = it.timestamp }
                .map { TelemetryPacketDeserializer.map(it.data) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { packet -> liveData.onUpdate(packet) }
        }
    }

    private fun stopReplay() {
        //todo
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
                Toast.makeText(this@MainActivity, "Capture saved to: " + file.path, Toast.LENGTH_SHORT).show()
            }
            liveCaptureWorker = null
        }
    }

    private fun onPacket(packet: DatagramPacket) {
        if (liveCaptureWorker != null) {
            synchronized(this) {
                if (liveCaptureWorker != null) {
                    liveCaptureWorker!!.onPacket(packet)
                }
            }
        }
    }
}