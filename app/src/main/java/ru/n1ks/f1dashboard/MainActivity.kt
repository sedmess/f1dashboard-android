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
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private enum class State {
        None, ListenOnly, ListenRecording, ReplayCapture, ReplayCrashReport
    }

    private lateinit var debugFrameCountTextView: TextView

    private lateinit var serviceConnection: TelemetryProviderService.Connection

    private lateinit var liveData: LiveData

    private var state = State.None

    private val saveCaptureFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            if (it == null) {
                AlertDialog.Builder(this)
                    .setMessage("Do you really want not to save the last capture?") //todo resources
                    .setNegativeButton("Yes, delete it") { _, _ -> moveCaptureFile(null) }
                    .setPositiveButton("No, save it") { _, _ -> captureSaveDialog() }
                    .setCancelable(false)
                    .create().show()
            } else {
                moveCaptureFile(it)
            }
        }
    private val openCaptureFile = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) replayFromCapture(it)
    }
    private val openReportFile = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) replayFromCrashReport(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        findViewById<View>(R.id.drsCaption).apply {
            setOnClickListener {
                openReportFile.launch("application/json")
            }
            setOnLongClickListener {
                throw RuntimeException("ops!")
            }
        }

        findViewById<TextView>(R.id.sessionTimeValue).setOnClickListener { showEndpoint() }

        debugFrameCountTextView = findViewById<TextView>(R.id.debugFrameCount).apply {
            setOnClickListener { toggleReplay() }
            setOnLongClickListener { toggleCapture(); true }
        }

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
        state = State.ListenOnly
    }

    override fun onStop() {
        when (state) {
            State.ListenRecording -> stopCapture()
            State.ReplayCapture -> stopReplay()
            State.ReplayCrashReport -> stopReplay()
            else -> {
            }
        }

        if (serviceConnection.isConnected()) {
            TelemetryProviderService.unbindService(this, serviceConnection)
        }

        state = State.None

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
        when (state) {
            State.ListenOnly -> replaySelectDialog()
            State.ListenRecording -> {
                stopCapture()
                replaySelectDialog()
            }
            State.ReplayCapture -> stopReplay()
            State.ReplayCrashReport -> stopReplay()
            State.None -> {
            }
        }
    }

    private fun replaySelectDialog() {
        //todo captions
        AlertDialog.Builder(this)
            .setTitle("Replay")
            .setMessage("Source of replay")
            .setPositiveButton("Capture file") { _, _ -> openCaptureFile.launch("*/*") }
            .setNegativeButton("Crash report JSON file") { _, _ -> openReportFile.launch("application/json") }
            .create()
            .apply { setCanceledOnTouchOutside(false); show() }
    }

    private fun replayFromCapture(uri: Uri) {
        TelemetryProviderService.unbindService(this, serviceConnection)

        debugFrameCountTextView.background = ContextCompat.getDrawable(this, R.color.replaying)

        TelemetryProviderService.bindService(
            this,
            ReplayService::class,
            serviceConnection,
            ReplayService.SourceType to ReplayService.SourceTypeCapture,
            ReplayService.SourcePath to uri.toString()
        )
        state = State.ReplayCapture
    }

    private fun replayFromCrashReport(uri: Uri) {
        TelemetryProviderService.unbindService(this, serviceConnection)

        debugFrameCountTextView.background = ContextCompat.getDrawable(this, R.color.replaying)

        TelemetryProviderService.bindService(
            this,
            ReplayService::class,
            serviceConnection,
            ReplayService.SourceType to ReplayService.SourceTypeReport,
            ReplayService.SourcePath to uri.toString()
        )
        state = State.ReplayCrashReport
    }

    private fun stopReplay() {
        debugFrameCountTextView.background = null
        TelemetryProviderService.unbindService(this, serviceConnection)
        TelemetryProviderService.bindService(this, ListenerService::class, serviceConnection)
        state = State.ListenOnly
    }

    private fun toggleCapture() {
        when (state) {
            State.ListenOnly -> startCapture()
            State.ListenRecording -> stopCapture()
            State.ReplayCapture -> {
            }
            State.ReplayCrashReport -> {
            }
            State.None -> {
            }
        }
    }

    private fun startCapture() {
        val service = serviceConnection.service()
        if (service is Recorder) {
            service.startRecording()
            debugFrameCountTextView.background = ContextCompat.getDrawable(this, R.color.recoring)
            state = State.ListenRecording
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
            captureSaveDialog()
        } else {
            Toast.makeText(this, "Capture wasn't enabled", Toast.LENGTH_SHORT).show()
        }
        debugFrameCountTextView.background = null
        state = State.ListenOnly
    }

    private fun captureSaveDialog() {
        saveCaptureFile.launch("")
    }

    private fun moveCaptureFile(uri: Uri?) {
        fileList().find { it == Recorder.LastestCaptureFilename }.also { fileName ->
            if (fileName == null) {
                Toast.makeText(this, "No capture found", Toast.LENGTH_SHORT).show()
                return@also
            }

            val captureFile = File(this.filesDir, fileName)

            if (uri != null) {
                contentResolver.openOutputStream(uri).use { to ->
                    if (to == null) {
                        Toast.makeText(this, "Can't create target file", Toast.LENGTH_SHORT)
                            .show()
                        return@also
                    }
                    FileInputStream(captureFile).use { from ->
                        from.copyTo(to)
                    }
                }
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            captureFile.delete()
        }
    }
}