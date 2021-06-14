package ru.n1ks.f1dashboard

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.capture.LiveCaptureFrame
import java.io.FileInputStream
import java.util.zip.GZIPInputStream

class ReplayService : TelemetryProviderService() {

    companion object {
        const val SourcePath = "source_path"
    }

    private var messageFlow: Flowable<ByteArray>? = null

    override fun start(intent: Intent) {
        val sourcePath = intent.getStringExtra(SourcePath)
            ?: throw IllegalArgumentException("$SourcePath extra not found")
        val inputStream = GZIPInputStream(FileInputStream(sourcePath))
        Log.d(TAG, "open capture file")
        Log.d(TAG, "start replaying")
        var prevTS = 0L
        messageFlow = LiveCaptureFrame.flow(inputStream) //todo move flow creation to this class
            .subscribeOn(Schedulers.newThread())
            .doOnTerminate {
                Log.d(TAG, "close capture file")
                inputStream.close()
            }
            .doOnNext { SystemClock.sleep(it.timestamp - prevTS); prevTS = it.timestamp }
            .map { it.data }
    }

    override fun stop() {}

    override fun flow(): Flowable<ByteArray> = messageFlow ?: throw IllegalStateException("service not initialized")
}
