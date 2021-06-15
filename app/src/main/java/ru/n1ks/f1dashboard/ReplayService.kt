package ru.n1ks.f1dashboard

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.capture.LiveCaptureFrame
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException

class ReplayService : TelemetryProviderService() {

    companion object {
        const val SourcePath = "source_path"
    }

    private var messageFlow: Flowable<ByteArray>? = null

    override fun start(intent: Intent) {
        val sourcePath = intent.getStringExtra(SourcePath)
            ?: throw IllegalArgumentException("$SourcePath extra not found")
        val inputStream = BufferedInputStream(FileInputStream(sourcePath)) //todo use gzip
        Log.d(TAG, "open capture file $sourcePath")
        Log.d(TAG, "start replaying")
        var prevTS = 0L
        messageFlow = Flowable.create<LiveCaptureFrame>(
            {
                try {
                    var frame = LiveCaptureFrame.readFrom(inputStream)
                    while (frame != null) {
                        it.onNext(frame)
                        frame = LiveCaptureFrame.readFrom(inputStream)
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "can't read from stream: ${e.message}")
                }
                it.onComplete()
            },
            BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.newThread())
            .doFinally {
                Log.d(TAG, "close capture file $sourcePath")
                inputStream.close()
            }
            .doOnNext {
                val targetTS = it.timestamp - prevTS + SystemClock.uptimeMillis()
                while (SystemClock.uptimeMillis() < targetTS) {
                    Thread.yield()
                }
                prevTS = it.timestamp
            }
            .map { it.data }
    }

    override fun stop() {}

    override fun flow(): Flowable<ByteArray> =
        messageFlow ?: throw IllegalStateException("service not initialized")
}
