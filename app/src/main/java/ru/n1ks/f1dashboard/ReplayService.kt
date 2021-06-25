package ru.n1ks.f1dashboard

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.capture.LiveCaptureFrame
import ru.n1ks.f1dashboard.reporting.ByteArraysJSONUtils
import ru.n1ks.f1dashboard.reporting.PacketTail
import java.io.BufferedInputStream
import java.util.zip.GZIPInputStream

class ReplayService : TelemetryProviderService() {

    companion object {
        const val SourceType = "source_type"
        const val SourceTypeCapture = "capture"
        const val SourceTypeReport = "report"
        const val SourcePath = "source_path"
        const val NoDelays = "no_delays"
    }

    private var messageFlow: Flowable<ByteArray>? = null

    override fun start(intent: Intent) {
        val sourceType = intent.getStringExtra(SourceType) ?: throw IllegalArgumentException("$SourceType extra not found")
        when (sourceType) {
            SourceTypeCapture -> {
                val sourcePath = intent.getStringExtra(SourcePath)
                    ?: throw IllegalArgumentException("$SourcePath extra not found")
                val replayDelays = intent.getStringExtra(NoDelays).isNullOrEmpty()
                startFromCapture(sourcePath, replayDelays)
            }
            SourceTypeReport -> {
                val sourcePath = intent.getStringExtra(SourcePath)
                    ?: throw IllegalArgumentException("$SourcePath extra not found")
                val emulateDelays = intent.getStringExtra(NoDelays).isNullOrEmpty()
                startFromReport(sourcePath, emulateDelays)
            }
            else -> throw IllegalArgumentException("$SourceType = $sourceType is invalid")
        }
    }

    override fun stop() {}

    override fun flow(): Flowable<ByteArray> =
        messageFlow ?: throw IllegalStateException("service not initialized")

    private fun startFromCapture(sourcePath: String, replayDelays: Boolean) {
        val captureUri = Uri.parse(sourcePath)
        val inputStream = contentResolver.openInputStream(captureUri)?.let { GZIPInputStream(BufferedInputStream(it)) } ?: throw IllegalArgumentException("can't open uri $captureUri")
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
                } catch (e: Exception) {
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

    private fun startFromReport(reportPath: String, emulateDelays: Boolean) {
        val reportUri = Uri.parse(reportPath)
        val packets = contentResolver.openInputStream(reportUri).use {
            val inputStream = it ?: throw IllegalArgumentException("can't open uri $reportUri")
            ByteArraysJSONUtils.fromJSON(inputStream.readBytes().decodeToString())
        }
        var counter = 0L
        messageFlow = packets.toFlowable()
            .subscribeOn(Schedulers.newThread())
            .doFinally {
                Log.d(TAG, "read $counter frames")
            }
            .doOnNext {
                counter++
                if (emulateDelays) {
                    val targetTS = SystemClock.uptimeMillis() + 500
                    while (SystemClock.uptimeMillis() < targetTS) {
                        Thread.yield()
                    }
                }
            }
            .doOnNext { PacketTail.onPacket(it) }
    }
}
