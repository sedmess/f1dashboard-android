package ru.n1ks.f1dashboard

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.net.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong


class ListenerService : TelemetryProviderService() {

    companion object {

        private const val TAG = "ListenerService"

        private const val UDPBufferLength = 2048
        private const val DroppedReportInterval = 10000
    }

    inner class Binder : android.os.Binder() {

        fun flow() = this@ListenerService.messageFlow!!
    }

    private val binder = Binder()
    private var socket: DatagramSocket? = null
    private var messageFlow: Flowable<DatagramPacket>? = null

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "start")
        val port = intent.getIntExtra(Properties.Port, -1)

        initServer(port)

        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "stopping")

        closeSocket()

        socket = null
        messageFlow = null

        return false
    }

    override fun onDestroy() {
        Log.d(TAG, "destroy")
        super.onDestroy()

        closeSocket()
        socket = null
        messageFlow = null
    }

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
            .doOnTerminate { closeSocket() }
    }

    private fun closeSocket() {
        if (socket?.isClosed == false) socket!!.close()
    }
}