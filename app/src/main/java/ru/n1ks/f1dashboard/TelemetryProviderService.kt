package ru.n1ks.f1dashboard

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import io.reactivex.Flowable
import kotlin.reflect.KClass

abstract class TelemetryProviderService : Service() {

    companion object {

        fun bindService(context: Context, properties: Properties, serviceClass: KClass<out TelemetryProviderService>, connection: ServiceConnection) {
            val intent = Intent(context, serviceClass.java)
            intent.apply {
                putExtra(Properties.Port, properties.port)
            }
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun unbindService(context: Context, connection: ServiceConnection) =
            context.unbindService(connection)
    }

    abstract fun flow(): Flowable<ByteArray>
}