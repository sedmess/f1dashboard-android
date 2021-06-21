package ru.n1ks.f1dashboard

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import io.reactivex.Flowable
import kotlin.reflect.KClass

abstract class TelemetryProviderService : Service() {

    companion object {

        fun bindService(context: Context, serviceClass: KClass<out TelemetryProviderService>, connection: Connection, vararg args: Pair<String, String> = emptyArray()) {
            val intent = Intent(context, serviceClass.java)
            if (!args.isNullOrEmpty()) {
                args.forEach { intent.putExtra(it.first, it.second) }
            }
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun unbindService(context: Context, connection: Connection) {
            context.unbindService(connection)
            connection.onUnbind()
        }
    }

    interface Connection : ServiceConnection {

        fun isConnected(): Boolean

        fun onUnbind()

        fun service(): TelemetryProviderService?
    }

    inner class Binder : android.os.Binder() {

        fun service(): TelemetryProviderService = this@TelemetryProviderService
    }

    @Suppress("PropertyName")
    protected val TAG: String = this.javaClass.simpleName

    private val binder = Binder()

    final override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "start")
        start(intent)
        return binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "stopping")
        stop()
        return false
    }

    final override fun onDestroy() {
        Log.d(TAG, "destroy")
        stop()
        super.onDestroy()
    }

    abstract fun flow(): Flowable<ByteArray>

    protected abstract fun start(intent: Intent)

    protected abstract fun stop()
}