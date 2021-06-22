package ru.n1ks.f1dashboard

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.capture.LiveCaptureFrame
import ru.n1ks.f1dashboard.reporting.PacketTail
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

class ReplayService : TelemetryProviderService() {

    companion object {
        const val SourcePath = "source_path"
        const val NoDelays = "no_delays"
    }

    private var messageFlow: Flowable<ByteArray>? = null

    override fun start(intent: Intent) {
        val sourcePath = intent.getStringExtra(SourcePath)
            ?: throw IllegalArgumentException("$SourcePath extra not found")
        val replayDelays = intent.getStringExtra(NoDelays).isNullOrEmpty()
        val inputStream = BufferedInputStream(GZIPInputStream(FileInputStream(sourcePath)))
        Log.d(TAG, "open capture file $sourcePath")
        Log.d(TAG, "start replaying, replay delays = $replayDelays")
        var prevTS = 0L
        var counter = 0L
        messageFlow = Flowable.create<LiveCaptureFrame>(
            {
                try {
                    var frame = LiveCaptureFrame.readFrom(inputStream)
                    while (frame != null && !it.isCancelled) {
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
                Log.d(TAG, "close capture file $sourcePath, read $counter frames")
                inputStream.close()
            }
            .doOnNext {
                counter++
                if (replayDelays) {
                    val targetTS = it.timestamp - prevTS + SystemClock.uptimeMillis()
                    while (SystemClock.uptimeMillis() < targetTS) {
                        Thread.yield()
                    }
                    prevTS = it.timestamp
                }
            }
            .map { it.data }
            .doOnNext { PacketTail.onPacket(it) }
    }

    override fun stop() {}

    override fun flow(): Flowable<ByteArray> =
        messageFlow ?: throw IllegalStateException("service not initialized")
}
