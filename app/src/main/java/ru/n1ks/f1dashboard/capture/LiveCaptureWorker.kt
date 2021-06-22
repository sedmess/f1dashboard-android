package ru.n1ks.f1dashboard.capture

import android.os.SystemClock
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPOutputStream

class LiveCaptureWorker(
    private val file: File
) : AutoCloseable {

    companion object {
        private const val TAG = "LiveCaptureWorker"
    }

    var frameCount: Long = 0
        private set
    private val outputStream = GZIPOutputStream(FileOutputStream(file))
    private val startTimestamp = SystemClock.uptimeMillis()
    private lateinit var flowEmitter: Emitter<ByteArray>
    private val flow = Flowable.create<ByteArray>({
        flowEmitter = it
    }, BackpressureStrategy.BUFFER)
    private val flowDisposable: Disposable

    init {
        Log.d(TAG, "open file " + file.path)
        flowDisposable = flow
            .map {LiveCaptureFrame(SystemClock.uptimeMillis() - startTimestamp, it) }
            .observeOn(Schedulers.newThread())
            .doFinally {
                Log.d(TAG, "close file " + file.path)
                outputStream.apply { finish(); flush(); close() }
            }
            .subscribe { it.writeTo(outputStream); outputStream.flush() }
    }

    fun onPacket(packet: ByteArray) {
        frameCount++
        flowEmitter.onNext(packet)
    }

    override fun close() {
        flowDisposable.dispose()
    }
}
