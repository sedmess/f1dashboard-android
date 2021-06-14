package ru.n1ks.f1dashboard

import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import ru.n1ks.f1dashboard.Properties.Companion.loadProperties
import java.net.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class ListenerService : TelemetryProviderService() {

    companion object {
        private const val UDPBufferLength = 2048
        private const val DroppedReportInterval = 10000
    }

    private var socket: DatagramSocket? = null
    private var messageFlow: Flowable<ByteArray>? = null

    override fun start(intent: Intent) {
        val port = getSharedPreferences(Properties.Name, Context.MODE_PRIVATE).loadProperties().port
        initServer(port)
    }

    override fun stop() {
        closeSocket()

        socket = null
        messageFlow = null
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
            .doOnTerminate { closeSocket() }
            .map { it.data }
    }

    private fun closeSocket() {
        if (socket?.isClosed == false) socket!!.close()
    }
}