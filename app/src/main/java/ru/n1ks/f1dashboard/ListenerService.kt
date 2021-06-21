package ru.n1ks.f1dashboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.Properties.Companion.loadProperties
import ru.n1ks.f1dashboard.capture.LiveCaptureWorker
import ru.n1ks.f1dashboard.capture.Recorder
import ru.n1ks.f1dashboard.reporting.UDPPacketTail
import java.io.File
import java.net.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class ListenerService : TelemetryProviderService(), Recorder {

    companion object {
        private const val UDPBufferLength = 2048
        private const val DroppedReportInterval = 10000
    }

    private var socket: DatagramSocket? = null
    private var messageFlow: Flowable<ByteArray>? = null

    @Volatile
    private var liveCaptureWorker: LiveCaptureWorker? = null

    override fun start(intent: Intent) {
        val port = getSharedPreferences(Properties.Name, Context.MODE_PRIVATE).loadProperties().port
        initServer(port)
    }

    override fun stop() {
        closeSocket()

        synchronized(this) {
            if (liveCaptureWorker != null) {
                stopRecording()
            }
        }

        socket = null
        messageFlow = null
    }

    override fun startRecording() {
        synchronized(this) {
            if (liveCaptureWorker != null) {
                startRecording()
            }
            val captureFile = File(this.filesDir, "latest.cap") //todo filename
            liveCaptureWorker = LiveCaptureWorker(captureFile)
            Log.d(TAG, "start capturing to file: " + captureFile.absolutePath)
        }
    }

    override fun stopRecording(): Long {
        synchronized(this) {
            val frameCount = liveCaptureWorker?.let {
                Log.d(TAG, "stop capturing")
                val frameCount = it.frameCount
                it.close()
                frameCount
            } ?: 0
            liveCaptureWorker = null
            return frameCount
        }
    }

    override fun flow(): Flowable<ByteArray> = messageFlow ?: throw IllegalStateException("service not initialized")

    private fun initServer(port: Int) {
        socket = DatagramSocket(port)
        val droppedLastTimestamp = AtomicLong()
        val droppedCounter = AtomicLong()
        messageFlow = Flowable.create<DatagramPacket>(
            {
                droppedLastTimestamp.set(System.currentTimeMillis())
                while (!it.isCancelled) {
                    val packet = DatagramPacket(ByteArray(UDPBufferLength), UDPBufferLength)
                    try {
                        socket!!.receive(packet)
                    } catch (e: SocketException) {
                        if (e.message == "Socket closed")
                            break
                        it.onError(e)
                    } catch (e: Exception) {
                        it.onError(e)
                        break
                    }
                    it.onNext(packet)
                }
                it.onComplete()
            },
            BackpressureStrategy.MISSING
        )
            .subscribeOn(Schedulers.newThread())
            .onBackpressureDrop {
                if (droppedLastTimestamp.get() < System.currentTimeMillis() - DroppedReportInterval) {
                    synchronized(droppedLastTimestamp) {
                        val period = System.currentTimeMillis() - droppedLastTimestamp.get()
                        if (period > DroppedReportInterval) {
                            Log.i(
                                TAG,
                                "dropped ${droppedCounter.incrementAndGet()} in last $period ms"
                            )
                            droppedLastTimestamp.set(System.currentTimeMillis())
                            droppedCounter.set(0)
                            return@onBackpressureDrop
                        }
                    }
                }
                droppedCounter.incrementAndGet()
            }
            .doFinally { closeSocket() }
            .map { it.data }
            .doOnNext {
                UDPPacketTail.onPacket(it)
                if (liveCaptureWorker != null) {
                    synchronized(this) {
                        if (liveCaptureWorker != null) {
                            liveCaptureWorker!!.onPacket(it)
                        }
                    }
                }
            }
    }

    private fun closeSocket() {
        if (socket?.isClosed == false) socket!!.close()
    }
}