package ru.n1ks.f1dashboard.capture

import android.os.SystemClock
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.zip.GZIPOutputStream

class LiveCaptureWorker(
    val file: File
) : AutoCloseable {

    companion object {
        private const val TAG = "LiveCaptureWorker"
    }

    private val outputStream: OutputStream
    private val startTimestamp = SystemClock.uptimeMillis()
    private lateinit var flowEmitter: Emitter<ByteArray>
    private val flow = Flowable.create<ByteArray>({
        flowEmitter = it
    }, BackpressureStrategy.BUFFER)
    private val flowDisposable: Disposable

    init {
        outputStream = GZIPOutputStream(FileOutputStream(file))
        Log.d(TAG, "open file " + file.path)
        flowDisposable = flow
            .map {LiveCaptureFrame(SystemClock.uptimeMillis() - startTimestamp, it) }
            .observeOn(Schedulers.newThread())
            .doOnTerminate {
                Log.d(TAG, "close file " + file.path)
                outputStream.close()
            }
            .subscribe { it.writeTo(outputStream); outputStream.flush() }
    }

    fun onPacket(packet: ByteArray) {
        flowEmitter.onNext(packet)
    }

    override fun close() {
        flowEmitter.onComplete()
        flowDisposable.dispose()
    }
}
