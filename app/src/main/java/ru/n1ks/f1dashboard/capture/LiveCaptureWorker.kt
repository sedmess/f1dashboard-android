package ru.n1ks.f1dashboard.capture

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.DatagramPacket

@SuppressLint("CheckResult")
class LiveCaptureWorker(
    val file: File
) : AutoCloseable {

    companion object {
        private const val TAG = "LiveCaptureWorker"
    }

    private val outputStream: OutputStream
    private val startTimestamp = SystemClock.uptimeMillis()
    private lateinit var flowEmitter: Emitter<DatagramPacket>
    private val flow = Flowable.create<DatagramPacket>({
        flowEmitter = it
    }, BackpressureStrategy.BUFFER)

    init {
        outputStream = BufferedOutputStream(FileOutputStream(file))
        Log.d(TAG, "open file " + file.path)
        flow
            .map {LiveCaptureFrame(System.uptimeMillis() - startTimestamp, it.data) }
            .observeOn(Schedulers.newThread())
            .doOnTerminate {
                Log.d(TAG, "close file " + file.path)
                outputStream.close()
            }
            .subscribe { it.writeTo(outputStream) }
    }

    fun onPacket(packet: DatagramPacket) {
        flowEmitter.onNext(packet)
    }

    override fun close() {
        flowEmitter.onComplete()
    }
}
