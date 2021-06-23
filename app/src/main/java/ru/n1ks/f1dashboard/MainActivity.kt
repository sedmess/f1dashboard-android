package ru.n1ks.f1dashboard

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.toSingle
import ru.n1ks.f1dashboard.Properties.Companion.loadProperties
import ru.n1ks.f1dashboard.capture.Recorder
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

    private val openReportFile = registerForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) replayFromFile(it) }

    //todo use current mode
    private var isRecording = false
    private var isReplaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        findViewById<View>(R.id.drsCaption).apply {
            setOnClickListener {
//                val intent = Intent().apply {
//                    type = "application/json"
//                    action = Intent.ACTION_GET_CONTENT
//                }
                openReportFile.launch("application/json")
            }
            setOnLongClickListener {
                throw RuntimeException("ops!")
                true
            }
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
            private var service: TelemetryProviderService? = null
            private var flowDisposable: Disposable? = null

            override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
                Log.d(TAG, "service $componentName connected")
                service = (binder as TelemetryProviderService.Binder).service()
                flowDisposable = service!!.flow()
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
                service = null
                connected = false
            }

            override fun isConnected() = connected

            override fun service(): TelemetryProviderService? = service
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
            .setMessage(
                "Endpoint: $ipAddress:${
                    getSharedPreferences(
                        Properties.Name,
                        Context.MODE_PRIVATE
                    ).loadProperties().port
                }"
            )
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
        if (isRecording) {
            stopCapture()
        }
        fileList().find { it == "latest.cap" }.also { fileName ->
            if (fileName == null) {
                Toast.makeText(this, "No captures found", Toast.LENGTH_SHORT).show()
                return@also
            }

            TelemetryProviderService.unbindService(this, serviceConnection)

            sessionTimeTextView.background = ContextCompat.getDrawable(this, R.color.warn)

            TelemetryProviderService.bindService(
                this,
                ReplayService::class,
                serviceConnection,
                ReplayService.SourceType to ReplayService.SourceTypeCapture,
                ReplayService.SourcePath to File(this.filesDir, fileName).absolutePath
            )
            isReplaying = true
        }
    }

    private fun replayFromFile(uri: Uri) {
        //todo deduplicate code
        if (isRecording) {
            stopCapture()
        }
        if (isReplaying) {
            startReplay()
        }

        TelemetryProviderService.unbindService(this, serviceConnection)

        sessionTimeTextView.background = ContextCompat.getDrawable(this, R.color.warn)

        TelemetryProviderService.bindService(
            this,
            ReplayService::class,
            serviceConnection,
            ReplayService.SourceType to ReplayService.SourceTypeReport,
            ReplayService.SourcePath to uri.toString()
        )
        isReplaying = true
    }

    private fun stopReplay() {
        sessionTimeTextView.background = null
        TelemetryProviderService.unbindService(this, serviceConnection)
        TelemetryProviderService.bindService(this, ListenerService::class, serviceConnection)
        isReplaying = false
    }

    private fun toggleCapture() {
        if (isRecording) {
            stopCapture()
        } else {
            startCapture()
        }
    }

    private fun startCapture() {
        val service = serviceConnection.service()
        if (service is Recorder) {
            service.startRecording()
            debugFrameCountTextView.background = ContextCompat.getDrawable(this, R.color.warn)
            isRecording = true
        } else {
            Toast.makeText(this, "Can't start recording", Toast.LENGTH_SHORT).show()
        }

    }

    private fun stopCapture() {
        val service = serviceConnection.service()
        if (service is Recorder) {
            val frameCount = service.stopRecording()
            Toast.makeText(this@MainActivity, "Captured $frameCount frames", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(this, "Capture wasn't enabled", Toast.LENGTH_SHORT).show()
        }
        debugFrameCountTextView.background = null
        isRecording = false
    }
}